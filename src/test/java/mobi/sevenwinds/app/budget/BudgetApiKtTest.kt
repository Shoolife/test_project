package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AuthorRecord
import mobi.sevenwinds.app.author.AuthorTable
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    fun setUp() {
        transaction {
            BudgetTable.deleteAll()
            AuthorTable.deleteAll()
        }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetRecord(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetRecord(2030, 1, 1, BudgetType.Расход))

        val count = transaction { BudgetTable.selectAll().count() }
        println("Количество записей в BudgetTable после добавления: $count")

        val response = RestAssured.given()
            .queryParam("limit", 5)
            .queryParam("offset", 0)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>()


        println("Всего записей: ${response.total} / ${response.items} / ${response.totalByType}")

        assertEquals(5, response.total)
        assertEquals(105, response.totalByType[BudgetType.Приход.name])
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetRecord(2020, 5, 400, BudgetType.Приход))

        val response = RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>()

        println(response.items)

        assertEquals(30, response.items[0].amount)
        assertEquals(5, response.items[1].amount)
        assertEquals(400, response.items[2].amount)
        assertEquals(100, response.items[3].amount)
        assertEquals(50, response.items[4].amount)
    }

    @Test
    fun testFilterByAuthorName() {
        val authorId = addAuthor("Иван Петров")
        addRecord(BudgetRecord(2020, 5, 100, BudgetType.Приход), authorId)
        addRecord(BudgetRecord(2020, 6, 200, BudgetType.Приход))

        val response = RestAssured.given()
            .queryParam("limit", 10)
            .queryParam("offset", 0)
            .queryParam("authorName", "Иван")
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>()

        assertEquals(1, response.items.size)
        assertEquals("Иван Петров", response.items[0].authorName)
    }

    private fun addAuthor(name: String): Int {
        val response = RestAssured.given()
            .jsonBody(AuthorRecord(fullName = name))
            .post("/author/add")
            .toResponse<AuthorRecord>()
        return response.id
    }

    private fun addRecord(record: BudgetRecord, authorId: Int? = null) {
        val response = RestAssured.given()
            .queryParam("authorId", authorId)
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetRecord>()
        assertEquals(record, response)
    }
}

