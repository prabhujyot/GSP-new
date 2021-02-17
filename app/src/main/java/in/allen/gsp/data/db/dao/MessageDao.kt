package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Message
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(messages: Message): Long

    @Query("update message set status = :status where id = :id")
    fun update(id:Int,status: Int)

    @Query("delete from message where id = :id")
    fun delete(id:Int)

    @Query("select * from message where user_id = :user_id order by create_date desc limit :offset,50")
    fun getList(user_id: Int, offset: Int): List<Message>

//    @Insert
//    suspend fun setList(messages: List<Message>)

    @Query("select * from message where id = :id")
    fun getItem(id:Int): Message

    @Query("select * from message order by id desc limit 1")
    fun getLastItem(): Message

    @Query("select count(msg) from message where user_id = :user_id and status = 0")
    fun countUnread(user_id: Int): Int

}