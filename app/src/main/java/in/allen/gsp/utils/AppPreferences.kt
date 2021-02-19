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

    var appMusic: Boolean
        get() {
            return  sharedPreferences.getBoolean("appMusic", false)
        }
        set(value) {
            editor.putBoolean("appMusic", value).commit()
        }

    var timestampChannel1: Long
        get() {
            return  sharedPreferences.getLong("timestampChannel1", 0)
        }
        set(value) {
            editor.putLong("timestampChannel1", value).commit()
        }

    var timestampChannel2: Long
        get() {
            return  sharedPreferences.getLong("timestampChannel2", 0)
        }
        set(value) {
            editor.putLong("timestampChannel2", value).commit()
        }

    var timestampChannel3: Long
        get() {
            return  sharedPreferences.getLong("timestampChannel3", 0)
        }
        set(value) {
            editor.putLong("timestampChannel3", value).commit()
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

    var subscribeNotification: Boolean
        get() {
            return  sharedPreferences.getBoolean("subscribeNotification", false)
        }
        set(value) {
            editor.putBoolean("subscribeNotification", value).commit()
        }

    var appNotification: Boolean
        get() {
            return  sharedPreferences.getBoolean("appNotification", false)
        }
        set(value) {
            editor.putBoolean("appNotification", value).commit()
        }

}