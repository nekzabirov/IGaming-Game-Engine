package db

import org.jetbrains.exposed.sql.Expression
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.QueryBuilder
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.append
import org.jetbrains.exposed.sql.castTo
import org.jetbrains.exposed.sql.intParam
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.stringParam

/** Postgres helpers created by `V11__fuzzy_search.sql` and `V12__fuzzy_search_fallback.sql`. */
private const val NORMALIZE_FUNCTION = "casino_search_norm"

private const val PHONETIC_FUNCTION = "casino_search_phonetic"

private const val CLOSE_WORD_FUNCTION = "casino_search_close"

/** A longer query is a paste, not a search — the tail only slows the scan down. */
private const val MAX_QUERY_LENGTH = 96

private const val MAX_TOKENS = 6

/** Shorter words carry too few consonants for a metaphone code to mean anything. */
private const val MIN_PHONETIC_LENGTH = 4

private val SEPARATORS = Regex("[^\\p{L}\\p{N}]+")

/**
 * Mirror of the SQL `casino_search_norm`: lower case, punctuation collapsed to single spaces.
 * The two must agree, otherwise a query token would not be searched the way the column was indexed.
 */
fun normalizeSearchQuery(raw: String): String =
    SEPARATORS.replace(raw.lowercase(), " ").trim().take(MAX_QUERY_LENGTH).trim()

/**
 * How many letters of a word the player is allowed to get wrong before the wide net stops
 * reaching it. Grows with the word, because one wrong letter in "ox" is a different word while
 * three wrong letters in "blackjack" is still obviously blackjack. `null` = too short to guess at.
 */
private fun maxTypoDistance(token: String): Int? = when {
    token.length >= 7 -> 3
    token.length >= 5 -> 2
    token.length >= 3 -> 1
    else -> null
}

/** Whether a wider retry of [rawQuery] would search for anything the strict pass did not. */
fun searchCanRelax(rawQuery: String): Boolean =
    normalizeSearchQuery(rawQuery).split(' ').any { maxTypoDistance(it) != null }

/**
 * Fuzzy, order-free, typo-tolerant matching over a set of columns — the searchable "haystack" of
 * one aggregate (a game's name + identity, a provider's name + identity + aliases, …).
 *
 * The strict pass ORs three branches, every one of them served by the GIN indexes of
 * `V11__fuzzy_search.sql`:
 *
 *  1. **every token** of the query is either a substring of the haystack (a fragment from either
 *     side: "olymp", "one bl") or trigram-similar to some word of it (`<%`, so "bonanca" still
 *     finds *Sweet Bonanza*). Tokens are ANDed, so word order and missing words don't matter —
 *     "gates olimpus" finds *Gates of Olympus*;
 *  2. the **whole phrase** is trigram-similar to the haystack, which covers a query typed without
 *     spaces ("bookofra" → *Book of Ra Deluxe*);
 *  3. the **double-metaphone** codes of the query are all present among the haystack's codes. This
 *     is what survives the misspellings trigrams give up on — "rulet" → *Roulette*, "gaets" →
 *     *Gates*, "krown" → *Crown*.
 *
 * `relaxed = true` adds a fourth, deliberately generous branch per token: any word of the haystack
 * that *starts* within [maxTypoDistance] edits of the token ("startbust" → *Starburst*, "blakj" →
 * *Blackjack*). It is a sequential scan, so it is meant for [searchPass] to fall back to — only
 * when the strict pass found nothing at all, where the alternative is an empty screen.
 *
 * The trigram threshold for (1) and (2) is the session-wide `pg_trgm.word_similarity_threshold`
 * set on the database by `V13__search_threshold.sql`.
 *
 * The column list handed to the constructor MUST match the expression indexed in
 * `V11__fuzzy_search.sql` — Postgres matches an expression index structurally.
 */
class SearchIndex(vararg parts: Expression<*>) {

    private val raw: Expression<String> = ConcatenatedColumns(parts.toList())

    private val text: Expression<String> = SqlFunction(NORMALIZE_FUNCTION, raw)

    private val phonetic: Expression<String> = SqlFunction(PHONETIC_FUNCTION, raw)

    fun matches(rawQuery: String, relaxed: Boolean = false): Op<Boolean> {
        val needle = normalizeSearchQuery(rawQuery)
        if (needle.isEmpty()) return Op.TRUE

        val tokens = needle.split(' ').take(MAX_TOKENS)

        val branches = buildList<Op<Boolean>> {
            add(tokens.map { token -> tokenMatches(token, relaxed) }.reduce { acc, op -> acc and op })

            if (tokens.size > 1) {
                add(wordSimilar(needle))
            }

            if (tokens.any { it.length >= MIN_PHONETIC_LENGTH }) {
                add(soundsLike(needle))
            }
        }

        return branches.reduce { acc, op -> acc or op }
    }

    /**
     * Ordering keys for a searched listing, in two tiers around the caller's own catalog order:
     *
     *  - [SearchRelevance.band] goes FIRST: a row that contains the query verbatim (as a prefix or
     *    a fragment) beats one that merely *sounds* like it, which beats a loose trigram
     *    resemblance. The phonetic band matters — without it "gaets" ranked *Gaelic Warrior*
     *    (trigram noise) above *Gates of Olympus*, which is the very row the query is about.
     *  - [SearchRelevance.score] goes LAST, after the catalog order: inside a band the popular row
     *    wins, and the trigram score only separates rows the catalog does not rank. Prefix and
     *    fragment share a band on purpose — "olymp" used to put *Olympus of Luck* (prefix) above
     *    *Gates of Olympus* (fragment), the game every player typing it wants.
     *
     * `null` when nothing was typed, so the caller keeps its own catalog ordering untouched.
     */
    fun relevance(rawQuery: String): SearchRelevance? {
        val needle = normalizeSearchQuery(rawQuery)
        if (needle.isEmpty()) return null

        val hasPhoneticCodes = needle.split(' ').any { it.length >= MIN_PHONETIC_LENGTH }

        return SearchRelevance(
            band = RelevanceBand(text, phonetic.takeIf { hasPhoneticCodes }, needle),
            score = TrigramScore(text, needle),
        )
    }

    /** Both tiers back to back — for listings that have no catalog order of their own to wrap. */
    fun relevanceOrdering(rawQuery: String): Array<Pair<Expression<*>, SortOrder>> =
        relevance(rawQuery)?.let { it.leading + it.trailing } ?: emptyArray()

    private fun tokenMatches(token: String, relaxed: Boolean): Op<Boolean> {
        val strict = containsFragment(token) or wordSimilar(token)
        if (!relaxed) return strict

        val distance = maxTypoDistance(token) ?: return strict

        return strict or startsCloseTo(token, distance)
    }

    private fun containsFragment(token: String): Op<Boolean> = Op.build { text like "%$token%" }

    private fun wordSimilar(needle: String): Op<Boolean> = WordSimilarityOp(stringParam(needle), text)

    private fun soundsLike(needle: String): Op<Boolean> = PhoneticContainsOp(phonetic, stringParam(needle))

    private fun startsCloseTo(token: String, distance: Int): Op<Boolean> =
        CloseWordOp(raw, stringParam(token), intParam(distance))
}

/** `part1 || ' ' || part2 || …` — the exact shape the expression indexes are built on. */
private class ConcatenatedColumns(private val parts: List<Expression<*>>) : Expression<String>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        parts.forEachIndexed { index, part ->
            if (index > 0) +" || ' ' || "
            +part
        }
    }
}

private class SqlFunction(
    private val functionName: String,
    private val argument: Expression<*>,
) : Expression<String>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append(functionName, '(', argument, ')')
    }
}

/** pg_trgm word similarity: the needle resembles some word-extent of the haystack. */
private class WordSimilarityOp(
    private val needle: Expression<String>,
    private val haystack: Expression<String>,
) : Op<Boolean>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append('(', needle, " <% ", haystack, ')')
    }
}

/**
 * Every metaphone code of the needle is present in the haystack's codes. The needle's codes are
 * computed by a scalar sub-select so the planner evaluates them once per scan (as an InitPlan)
 * instead of once per row.
 */
private class PhoneticContainsOp(
    private val haystack: Expression<String>,
    private val needle: Expression<String>,
) : Op<Boolean>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append('(', haystack, " @> (SELECT ", PHONETIC_FUNCTION, "(", needle, ")))")
    }
}

/** Some word of the haystack starts within [distance] edits of the needle. */
private class CloseWordOp(
    private val haystack: Expression<String>,
    private val needle: Expression<String>,
    private val distance: Expression<Int>,
) : Op<Boolean>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append(CLOSE_WORD_FUNCTION, '(', haystack, ", ", needle, ", ", distance, ')')
    }
}

/** The two halves of a searched listing's order — see [SearchIndex.relevance]. */
class SearchRelevance(
    val band: Expression<Int>,
    val score: Expression<Double>,
) {
    /** Keys that go BEFORE the catalog order. */
    val leading: Array<Pair<Expression<*>, SortOrder>> = arrayOf(band to SortOrder.DESC)

    /** Keys that go AFTER the catalog order. */
    val trailing: Array<Pair<Expression<*>, SortOrder>> = arrayOf(score to SortOrder.DESC)
}

private class RelevanceBand(
    private val text: Expression<String>,
    private val phonetic: Expression<String>?,
    private val needle: String,
) : Expression<Int>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("(CASE WHEN ", text, " LIKE ", stringParam("%$needle%"), " THEN 2")

        if (phonetic != null) {
            append(" WHEN ", PhoneticContainsOp(phonetic, stringParam(needle)), " THEN 1")
        }

        append(" ELSE 0 END)")
    }
}

private class TrigramScore(
    private val text: Expression<String>,
    private val needle: String,
) : Expression<Double>() {

    override fun toQueryBuilder(queryBuilder: QueryBuilder): Unit = queryBuilder {
        append("word_similarity(", stringParam(needle), ", ", text, ")")
    }
}

/**
 * What each catalog listing searches over. Every entry has a matching pair of expression indexes
 * in `V11__fuzzy_search.sql`; adding a column on one side without the other silently turns that
 * listing's search into a sequential scan.
 */
object SearchIndexes {

    val games = SearchIndex(CasinoGames.name, CasinoGames.identity)

    val providers = SearchIndex(CasinoProviders.name, CasinoProviders.identity)

    val collections = SearchIndex(Collections.name.castTo<String>(TextColumnType()), Collections.identity)
}

/** The condition a listing ended up searching with, and how many rows it matches. */
data class SearchPass<C>(
    val condition: C,
    val totalItems: Long,
)

/**
 * Runs the strict, index-backed search first and falls back to the wide net only when the strict
 * pass matched nothing at all — a player who typed something the catalog recognises never pays for
 * the fallback and never sees its noise. [relaxable] is the caller's own gate (see [searchCanRelax]).
 */
fun <C> searchPass(
    relaxable: Boolean,
    condition: (relaxed: Boolean) -> C,
    count: (C) -> Long,
): SearchPass<C> {
    val strict = condition(false)
    val strictTotal = count(strict)

    if (!relaxable || strictTotal > 0) return SearchPass(strict, strictTotal)

    val relaxed = condition(true)

    return SearchPass(relaxed, count(relaxed))
}
