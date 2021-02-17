package `in`.allen.gsp.ui.message

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.MessageRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class NotificationViewModel(
    private val userRepository: UserRepository,
    private val messageRepository: MessageRepository
): ViewModel() {

    private val ALERT = "alert"
    private val SNACKBAR = "snackbar"
    private val TAG = "tag"
    private val TOAST = "toast"

    // interaction with activity
    private val _loading = MutableLiveData<Resource.Loading<Any>>()
    private val _success = MutableLiveData<Resource.Success<Any>>()
    private val _error = MutableLiveData<Resource.Error<String>>()

    fun getLoading(): LiveData<Resource.Loading<Any>> {
        return _loading
    }

    fun getError(): LiveData<Resource.Error<String>> {
        return _error
    }

    fun getSuccess(): LiveData<Resource.Success<Any>> {
        return _success
    }

    fun setLoading(loading: Boolean) {
        _loading.value = Resource.Loading(loading)
    }

    fun setError(data: String, filter: String) {
        _error.value = Resource.Error(filter, data)
    }

    fun setSuccess(data: Any, filter: String) {
        _success.value = Resource.Success(data,filter)
    }

    var user: User?= null

    init {
        userData()
    }

    fun userData() {
        viewModelScope.launch {
            val dbUser = userRepository.getDBUser()
            if (dbUser != null) {
                user = dbUser
                getNotifications(1)
            } else {
                setError("Not Found",TAG)
            }
        }
    }

    fun getNotifications(page: Int) {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                val response = messageRepository.getList(user?.user_id!!, page)
                setSuccess(response,"notifications")
            }
        }
    }

    fun openMessage(notificationId: Int, position: Int) {
        tag("getUnreadCount 1")
        viewModelScope.launch {
            Coroutines.io {
                messageRepository.updateItem(notificationId,1)
                if(user != null && user?.user_id!! > 0) {
                    tag("getUnreadCount")
                    messageRepository.getUnreadCount(user?.user_id!!)
                }
            }

            val response = messageRepository.getItem(notificationId)
            val data = HashMap<String,Any>()
            data["item"] = response
            data["position"] = position
            setSuccess(data,"open")
        }
    }

}