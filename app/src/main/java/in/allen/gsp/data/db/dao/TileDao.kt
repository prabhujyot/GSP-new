package `in`.allen.gsp.data.db.dao

import `in`.allen.gsp.data.entities.Tile
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TileDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun setList(tiles: List<Tile>)

    @Query("select * from tile order by id desc")
    fun getList(): List<Tile>

    @Query("select * from tile where id = :id")
    fun getItem(id:Int): Tile

    @Query("delete from tile")
    fun clearList()

}