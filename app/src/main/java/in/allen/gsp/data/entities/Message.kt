package `in`.allen.gsp.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message")
data class Message (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val title: String,
    val msg: String,
    @ColumnInfo(name = "create_date") val date: String,
    val status: Int
)