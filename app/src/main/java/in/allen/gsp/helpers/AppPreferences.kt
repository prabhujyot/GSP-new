package `in`.allen.gsp.helpers

import `in`.allen.gsp.HomeActivity
import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val tag = AppPreferences::class.java.name

    private val sharedpreferences: SharedPreferences = context.getSharedPreferences("App_" + context.packageName, Context.MODE_PRIVATE)
    private val editor: SharedPreferences.Editor = sharedpreferences.edit()

    fun clear() {
        editor.clear().commit()
    }

    var firebaseToken: String
        get() {
            return sharedpreferences.getString("firebaseToken","")!!
        }
        set(value) {
            editor.putString("firebaseToken", value).commit()
        }

    var appIntro: Boolean
        get() {
            return  sharedpreferences.getBoolean("appIntro", false)
        }
        set(value) {
            editor.putBoolean("appIntro", value).commit()
        }

    var appMute: Boolean
        get() {
            return  sharedpreferences.getBoolean("appMute", false)
        }
        set(value) {
            editor.putBoolean("appMute", value).commit()
        }

}