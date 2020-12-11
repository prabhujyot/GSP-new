package `in`.allen.gsp.data.entities

import android.os.Parcel
import android.os.Parcelable
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
    val high_score: Int,
    val xp: Int,
    val create_date: String,
    val session_token: String,
    var coins: Int,
    var is_verified: Int,
    val is_admin: Boolean,
    val config: String
) : Parcelable {
    var life = 0
    var update_at = 0L

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString().toString(),
        parcel.readString().toString(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte(),
        parcel.readString().toString()
    ) {
        life = parcel.readInt()
        update_at = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeInt(user_id)
        parcel.writeString(name)
        parcel.writeString(avatar)
        parcel.writeString(email)
        parcel.writeString(mobile)
        parcel.writeString(about)
        parcel.writeString(location)
        parcel.writeString(referral_id)
        parcel.writeString(firebase_token)
        parcel.writeString(firebase_uid)
        parcel.writeString(played_qid)
        parcel.writeInt(high_score)
        parcel.writeInt(xp)
        parcel.writeString(create_date)
        parcel.writeString(session_token)
        parcel.writeInt(coins)
        parcel.writeInt(is_verified)
        parcel.writeByte(if (is_admin) 1 else 0)
        parcel.writeString(config)
        parcel.writeInt(life)
        parcel.writeLong(update_at)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}