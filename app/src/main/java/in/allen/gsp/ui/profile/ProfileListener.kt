package `in`.allen.gsp.ui.profile

import androidx.lifecycle.LiveData

interface ProfileListener {
    fun onStarted()
    fun onSuccess(response: LiveData<String>)
    fun onFailed(message: String)
}