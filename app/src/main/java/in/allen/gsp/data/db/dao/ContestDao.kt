package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Contest
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ContestDao {

    @Query("select * from contest order by id desc")
    fun getList(): LiveData<List<Contest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(contest: List<Contest>)

    @Query("select * from contest  where id = :id")
    fun getItem(id:Int): Contest

}