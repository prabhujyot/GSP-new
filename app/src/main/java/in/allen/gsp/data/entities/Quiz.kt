package `in`.allen.gsp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz")
data class Quiz(
    @PrimaryKey(autoGenerate = false) val id: Int,
    var quiz_no: Int,
    var qset: String,
    var wset: String,
    var fset: String,
    var attachment: String,
    var status: Int,
    var level: Int
) {
    var start_time = ""
    var endTime = ""
    var points = 0
    var score = 0
    var stats_data = ""
    var lifeline = ""
}