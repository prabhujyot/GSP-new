package `in`.allen.gsp.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User (
    @PrimaryKey(autoGenerate = false) val id: Int,
    val user_id: Int,
    val name: String,
    val avatar: String,
    val email: String,
    var mobile: String,
    var about: String,
    var location: String,
    val referral_id: String,
    val firebase_token: String,
    val firebase_uid: String,
    val played_qid: String,
    val create_date: String,
    val session_token: String,
    var coins: Int,
    var is_verified: Int,
    val is_admin: Boolean,
    val config: String
)