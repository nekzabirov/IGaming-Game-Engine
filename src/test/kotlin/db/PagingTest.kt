package db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PagingTest : FunSpec({

    test("page 0 and page 1 are the same first page, size 0 reads one row") {
        Pageable(0, 0).let { it.pageReal shouldBe 1; it.sizeReal shouldBe 1; it.offset shouldBe 0L }
        Pageable(3, 20).offset shouldBe 40L
    }

    test("total pages rounds up and is never 0") {
        Pageable(1, 20).totalPages(0) shouldBe 1L
        Pageable(1, 20).totalPages(41) shouldBe 3L
    }
})
