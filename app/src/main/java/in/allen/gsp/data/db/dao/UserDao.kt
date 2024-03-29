package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.User
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(user: User): Long

    @Query("select * from user where id = 0")
    fun getUser(): User?

    @Query("delete from user")
    fun deleteUser()

}