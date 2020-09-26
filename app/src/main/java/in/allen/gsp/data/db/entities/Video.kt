package `in`.allen.gsp.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "video")
data class Video (
    @PrimaryKey(autoGenerate = false) val videoId: String,
    val title: String,
    val channelTitle: String,
    val playlistId: String,
    val publishedAt: String,
    val description: String,
    val thumb: String
)