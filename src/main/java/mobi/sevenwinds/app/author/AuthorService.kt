package mobi.sevenwinds.app.author

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime

object AuthorService {

    suspend fun addAuthor(body: AuthorRecord): AuthorRecord = withContext(Dispatchers.IO) {
        transaction {
            val entity = AuthorEntity.new {
                this.fullName = body.fullName
                this.createdAt = DateTime.now()
            }
            return@transaction entity.toResponse()
        }
    }

    private fun AuthorEntity.toResponse(): AuthorRecord {
        return AuthorRecord(
            id = this.id.value,
            fullName = this.fullName,
            createdAt = this.createdAt.toString()
        )
    }
}