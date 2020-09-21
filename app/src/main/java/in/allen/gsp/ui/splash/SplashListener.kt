package `in`.allen.gsp.ui.splash

import android.content.Intent

interface SplashListener {
    fun startActivityForResult(intent: Intent?, requestCode: Int)
}