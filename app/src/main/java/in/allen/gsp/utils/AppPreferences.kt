package `in`.allen.gsp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager

class AppPreferences(context: Context) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val editor = sharedPreferences.edit()

    fun clear() {
        editor.clear().commit()
    }

    var firebaseToken: String
        get() {
            return sharedPreferences.getString("firebaseToken","")!!
        }
        set(value) {
            editor.putString("firebaseToken", value).commit()
        }

    var appIntro: Boolean
        get() {
            return  sharedPreferences.getBoolean("appIntro", false)
        }
        set(value) {
            editor.putBoolean("appIntro", value).commit()
        }

    var appMute: Boolean
        get() {
            return  sharedPreferences.getBoolean("appMute", false)
        }
        set(value) {
            editor.putBoolean("appMute", value).commit()
        }

    var timestampVideos: Long
        get() {
            return  sharedPreferences.getLong("timestampVideos", 0)
        }
        set(value) {
            editor.putLong("timestampVideos", value).commit()
        }

    var timestampLeaderboard: Long
        get() {
            return  sharedPreferences.getLong("timestampLeaderboard", 0)
        }
        set(value) {
            editor.putLong("timestampLeaderboard", value).commit()
        }
}