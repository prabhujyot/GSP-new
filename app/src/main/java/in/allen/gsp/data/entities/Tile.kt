package `in`.allen.gsp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tile")
data class Tile(
    @PrimaryKey(autoGenerate = false) val id: Int,
    var text: String,
    var image: String,
    var status: Int
)