package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Message
import androidx.lifecycle.LiveData
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

    @Query("select * from message order by create_date desc")
    fun getList(): LiveData<List<Message>>

//    @Insert
//    suspend fun setList(messages: List<Message>)

    @Query("select * from message  where id = :id")
    fun getItem(id:Int): LiveData<Message>

}