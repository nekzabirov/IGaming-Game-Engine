-- casino-engine stops being a multi-aggregator engine. GameHub owns every vendor integration now
-- and casino-engine talks to it as a single upstream service, the same way it already talks to
-- pam-engine. One vendor means one row per game, so `casino_game_variants` collapses into
-- `casino_games`; sessions/rounds/wallet calls no longer carry a vendor-specific `aggregator` or a
-- casino-engine-minted session at all — the hub calls straight in with player+game+round.
--
-- The data being dropped here is catalogue metadata and a handful of test rounds/spins, not a
-- financial ledger — sync rebuilds the catalogue from GameHub on its first run after this lands.

-- Empty the tables whose shape is about to change, before touching their columns: a NOT NULL
-- column added to live rows needs a default or a backfill, and there is nothing correct to
-- backfill `casino_rounds.player_id` from once `casino_sessions` is gone.
TRUNCATE TABLE spins, casino_rounds, casino_games, casino_providers CASCADE;

-- ------------------------------------------------------------------------------------- games --
ALTER TABLE casino_games
    ADD COLUMN free_spin_enable  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN free_chip_enable  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN jackpot_enable    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN demo_enable       BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN bonus_buy_enable  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN locales           JSON    NOT NULL DEFAULT '[]',
    ADD COLUMN platforms         JSON    NOT NULL DEFAULT '[]',
    ADD COLUMN play_lines        INT     NOT NULL DEFAULT 0,
    ADD COLUMN custom_images     JSON    NOT NULL DEFAULT '{}';

-- Was `DOUBLE PRECISION NOT NULL DEFAULT 96` (recalculated locally, daily). Now synced from
-- GameHub, which reports it as `optional` — a game with no bets in its last window is honestly
-- unmeasured, not RTP 0, so the column has to be able to say so.
ALTER TABLE casino_games
    ALTER COLUMN rtp DROP DEFAULT,
    ALTER COLUMN rtp DROP NOT NULL;

-- --------------------------------------------------------------------------------- providers --
ALTER TABLE casino_providers DROP COLUMN aggregator_id;
ALTER TABLE casino_providers DROP COLUMN aliases;
ALTER TABLE casino_providers ADD COLUMN custom_images JSON NOT NULL DEFAULT '{}';

-- ------------------------------------------------------------------------------------ rounds --
-- No more session in between: the hub names a round by its own id, with the player and the game
-- (nullable — empty for a sportsbook leg) carried directly on every wallet call.
-- Table renames (V7/V8) explicitly kept the original constraint/index names — the FKs and the
-- unique index below are still named after `rounds`/`sessions`, not `casino_rounds`.
ALTER TABLE casino_rounds DROP CONSTRAINT rounds_session_id_fkey;
ALTER TABLE casino_rounds DROP CONSTRAINT rounds_game_variant_id_fkey;
DROP INDEX IF EXISTS rounds_external_id_session_id_unique;

ALTER TABLE casino_rounds DROP COLUMN session_id;
ALTER TABLE casino_rounds RENAME COLUMN game_variant_id TO game_id;
ALTER TABLE casino_rounds ALTER COLUMN game_id DROP NOT NULL;
ALTER TABLE casino_rounds ADD CONSTRAINT casino_rounds_game_id_fkey
    FOREIGN KEY (game_id) REFERENCES casino_games (id);

-- No DEFAULT needed for either: the table was just truncated, so there are no existing rows a
-- bare NOT NULL could conflict with.
ALTER TABLE casino_rounds ADD COLUMN player_id VARCHAR(255) NOT NULL;
ALTER TABLE casino_rounds ADD COLUMN currency VARCHAR(10) NOT NULL;

CREATE UNIQUE INDEX casino_rounds_external_id_unique ON casino_rounds (external_id);
CREATE INDEX casino_rounds_player_id ON casino_rounds (player_id);

-- ------------------------------------------------------------------------- everything gone --
-- `casino_sessions`: nothing looks a session up any more — the hub owns launch and never calls
-- back with a casino-engine token. What survives is `session.events`, published from an in-memory
-- value, never persisted.
DROP TABLE casino_sessions CASCADE;

-- Freespins: the hub is the only source of grant state now (CreateFreespin/CancelFreespin/
-- FreespinPresetsCasino); casino-engine tracks nothing locally any more.
DROP TABLE freespins;

-- Sportsbook: bets and their sessions move to the hub's OpenSportbook + the same wallet path as
-- casino. No local Bet aggregate any more — money is money.
DROP TABLE bets;
DROP TABLE sportbook_sessions;

-- The vendor abstraction itself.
DROP TABLE casino_game_variants;
DROP TABLE aggregators;

-- ------------------------------------------------------------------------------- spins bug --
-- `SpinTable.externalId` has carried `.uniqueIndex()` in the Exposed table definition since V1,
-- and `SpinRepositoryImpl.save()` has always caught a unique-violation and mapped it to
-- `SpinAlreadyExistsException` — but on at least one environment this schema (Flyway, not Exposed
-- auto-DDL) only ever created a PLAIN index (`spins_external_id_idx`); the idempotency guarantee
-- the code has always claimed relied on a constraint that was never actually there. Fixed here, in
-- the same migration that makes `WalletGrpcService` lean on it directly: two concurrent
-- redeliveries of the same leg now collide in Postgres, not in a race between two callers' SELECTs.
--
-- IF EXISTS / IF NOT EXISTS on both sides: prematch's own DB already carries the fix as
-- `spins_external_id_unique` (fixed out of band at some point, ahead of this migration ever
-- running there), so this has to be a no-op wherever it already holds, not just where it doesn't.
DROP INDEX IF EXISTS spins_external_id_idx;
CREATE UNIQUE INDEX IF NOT EXISTS spins_external_id_unique ON spins (external_id);
