package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Banner
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BannerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setList(banner: List<Banner>)

    @Query("select * from banner order by id desc")
    fun getList(): LiveData<List<Banner>>

    @Query("select * from banner where id = :id")
    fun getItem(id:Int): Banner

    @Query("delete from banner")
    fun clearList()

}