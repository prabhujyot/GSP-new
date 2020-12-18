package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.Message
import `in`.allen.gsp.data.network.SafeApiRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class MessageRepository(
    private val db: AppDatabase
): SafeApiRequest() {

    suspend fun setItem(message: Message) = db.getMessageDao().insert(message)

    suspend fun getItem(id: Int): Message {
        return withContext(Dispatchers.IO) {
            db.getMessageDao().getItem(id)
        }
    }

    fun updateItem(id: Int,status: Int) = db.getMessageDao().update(id,status)

    fun getLastItem() = db.getMessageDao().getLastItem()

    suspend fun getList(userId: Int, page: Int): HashMap<String,Any> {
        val hashMap = HashMap<String,Any>()
        withContext(Dispatchers.IO) {
            val offset = page.minus(1).times(50)
            hashMap["list"] = db.getMessageDao().getList(userId,offset)
            hashMap["page"] = page.plus(1)
        }
        return hashMap
    }

}