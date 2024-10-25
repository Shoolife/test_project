package mobi.sevenwinds.app.budget

import com.papsign.ktor.openapigen.annotations.parameters.PathParam
import com.papsign.ktor.openapigen.annotations.parameters.QueryParam
import com.papsign.ktor.openapigen.route.info
import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.get
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respond
import com.papsign.ktor.openapigen.route.route
import mobi.sevenwinds.app.author.AuthorRecord
import mobi.sevenwinds.app.author.AuthorService


data class AddBudgetParams(
    @QueryParam("author_id") val authorId: Int?
)

data class BudgetYearParam(
    @PathParam("year") val year: Int,
    @QueryParam("limit") val limit: Int,
    @QueryParam("offset") val offset: Int,
    @QueryParam("authorName") val authorName: String? = null
)

fun NormalOpenAPIRoute.budget() {
    route("/budget") {
        route("/add").post<AddBudgetParams, BudgetRecord, BudgetRecord>(info("Добавить запись")) { params, body ->
            respond(BudgetService.addRecord(body, params.authorId))
        }

        route("/year/{year}/stats").get<BudgetYearParam, BudgetYearStatsResponse>(info("Получить статистику за год")) { param ->
            respond(BudgetService.getYearStats(param, param.authorName))
        }
    }

    route("/author/add").post<Unit, AuthorRecord, AuthorRecord>(info("Добавить автора")) { _, body ->
        val newAuthor = AuthorService.addAuthor(body)
        respond(newAuthor)
    }
}

fun BudgetEntity.toResponseWithAuthor(): BudgetRecordWithAuthor {
    return BudgetRecordWithAuthor(
        year = this.year,
        month = this.month,
        amount = this.amount,
        type = this.type,
        authorName = this.author?.fullName,
        authorCreatedAt = this.author?.createdAt?.toString()
    )
}

data class BudgetRecord(
    val year: Int,
    val month: Int,
    val amount: Int,
    val type: BudgetType
)

class BudgetYearStatsResponse(
    val total: Int,
    val totalByType: Map<String, Int>,
    val items: List<BudgetRecordWithAuthor>
)

data class BudgetRecordWithAuthor(
    val year: Int,
    val month: Int,
    val amount: Int,
    val type: BudgetType,
    val authorName: String?,
    val authorCreatedAt: String?
)

enum class BudgetType {
    Приход, Расход
}
