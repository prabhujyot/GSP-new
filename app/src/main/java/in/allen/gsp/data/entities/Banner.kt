package `in`.allen.gsp.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "banner")
data class Banner(
    @PrimaryKey(autoGenerate = false) val id: Int,
    var title: String,
    var image: String,
    @ColumnInfo(name = "action") var action: String,
    @ColumnInfo(name = "start_time") var startTime: String,
    @ColumnInfo(name = "end_time") var endTime: String,
    var meta: String,
    var status: Int
)