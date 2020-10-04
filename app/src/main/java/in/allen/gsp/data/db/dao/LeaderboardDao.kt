package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Leaderboard
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LeaderboardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setList(leaderbords: List<Leaderboard>)

    @Query("Select * from leaderboard order by rank ASC")
    fun getList(): LiveData<List<Leaderboard>>

}