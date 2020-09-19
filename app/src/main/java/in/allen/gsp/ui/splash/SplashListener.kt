package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.data.db.entities.User
import android.content.Intent

interface SplashListener {
    fun startActivityForResult(intent: Intent?, requestCode: Int)
    fun onStarted()
    fun onProgress()
    fun onSuccess(user: User)
    fun onFailure(message: String)
}