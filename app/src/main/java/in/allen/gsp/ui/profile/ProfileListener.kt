package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.data.db.entities.User

interface ProfileListener {
    fun onStarted()
    fun onSuccess(user: User)
    fun onFailed(message: String)
}