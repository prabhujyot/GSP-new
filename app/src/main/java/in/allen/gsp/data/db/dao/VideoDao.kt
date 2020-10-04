package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Video
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setList(videos: List<Video>)

    @Query("Select * from video where playlistId = :playlistId")
    fun getList(playlistId: String): LiveData<List<Video>>

}