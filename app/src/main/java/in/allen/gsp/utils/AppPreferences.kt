package `in`.allen.gsp.utils

import android.content.Context
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

    var timestampPlaylist1: Long
        get() {
            return  sharedPreferences.getLong("timestampPlaylist1", 0)
        }
        set(value) {
            editor.putLong("timestampPlaylist1", value).commit()
        }

    var timestampPlaylist2: Long
        get() {
            return  sharedPreferences.getLong("timestampPlaylist2", 0)
        }
        set(value) {
            editor.putLong("timestampPlaylist2", value).commit()
        }

    var timestampLeaderboard: Long
        get() {
            return  sharedPreferences.getLong("timestampLeaderboard", 0)
        }
        set(value) {
            editor.putLong("timestampLeaderboard", value).commit()
        }

    var timestampLife: Long
        get() {
            return  sharedPreferences.getLong("timestampLife", 0)
        }
        set(value) {
            editor.putLong("timestampLife", value).commit()
        }
}