package `in`.allen.gsp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class Leaderboard (
    @PrimaryKey(autoGenerate = false) val rank: Int,
    val user_id: String,
    val name: String,
    val avatar: String,
    val score: Int
)