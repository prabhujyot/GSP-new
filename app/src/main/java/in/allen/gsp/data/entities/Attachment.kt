package `in`.allen.gsp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attachment")
data class Attachment (
    @PrimaryKey(autoGenerate = false) val qid: Int,
    val type: String,
    val data: String,
    val filename: String
)