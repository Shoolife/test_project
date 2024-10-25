package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable

@OptIn(ExperimentalStdlibApi::class)
object BudgetService {

    suspend fun addRecord(body: BudgetRecord, authorId: Int?): BudgetRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.author = authorId?.let { AuthorEntity.findById(it) }
            }
            return@transaction entity.toResponse()
        }
    }

    suspend fun getYearStats(param: BudgetYearParam, authorNameFilter: String?): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            var query = BudgetTable
                .leftJoin(AuthorTable, { BudgetTable.author }, { AuthorTable.id })
                .select { BudgetTable.year eq param.year }
                .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                .limit(param.limit, param.offset)

            if (!authorNameFilter.isNullOrEmpty()) {
                println("Применение фильтрации по имени автора: $authorNameFilter")
                query = query.andWhere { AuthorTable.fullName.lowerCase() like "%${authorNameFilter.lowercase()}%" }
            }

            val data = BudgetEntity.wrapRows(query).map { it.toResponseWithAuthor() }
            val sumByType = data.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            return@transaction BudgetYearStatsResponse(
                total = data.size,
                totalByType = sumByType,
                items = data
            )
        }
    }




}