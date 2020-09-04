package `in`.allen.gsp.helpers.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

abstract class AppReceiver : BroadcastReceiver() {

    private val tag = AppReceiver::class.java.name

    abstract fun response(type: String, response: Any)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(tag, " ${intent.action}")
        if (intent.action == "download") {
            val type = "download"
            response(type, intent.getStringExtra("param")!!)
        }
    }
}
