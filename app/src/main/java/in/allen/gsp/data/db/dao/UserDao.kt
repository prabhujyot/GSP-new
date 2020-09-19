package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.db.entities.User
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: User): Long

    @Query("select * from user where id = 0")
    fun getUser(): LiveData<User>

}