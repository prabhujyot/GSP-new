package `in`.allen.gsp.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contest")
data class Contest(
    @PrimaryKey(autoGenerate = false) val id: Int,
    var name: String,
    var desc: String,
    var startDate: String,
    var endDate: String,
    var logo: String,
    @ColumnInfo(name = "enrollment_requirement") var enrollmentRequirement: Int,
    @ColumnInfo(name = "enrollment_start_time") var enrollmentStartTime: String,
    @ColumnInfo(name = "enrollment_end_time") var enrollmentEndTime: String,
    @ColumnInfo(name = "enrollment_max_user") var enrollmentMaxUser: Int,
    @ColumnInfo(name = "attempt_type") var attemptType: String,
    @ColumnInfo(name = "contest_msg") var contestMsg: String,
    var status: Int
)