package `in`.allen.gsp.ui.leaderboard

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


class LeaderboardViewModel(
    private val userRepository: UserRepository,
    private val repository: LeaderboardRepository
): ViewModel() {

    val ALERT = "alert"
    val SNACKBAR = "snackbar"
    val TAG = "tag"
    val TOAST = "toast"

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
        _error.postValue(Resource.Error(filter, data))
    }

    fun setSuccess(data: Any, filter: String) {
        _success.value = Resource.Success(data, filter)
    }

    fun userData() {
        viewModelScope.launch {
            val dbUser = userRepository.getDBUser()
            if (dbUser != null) {
                setSuccess(dbUser,"user")
            } else {
                setError("Not found",SNACKBAR)
            }
        }
    }

    fun leaderboard(user: User) {
        try {
            val arr = JSONArray(user.config)
            var minutes = 1440

            for(i in 0 until arr.length()) {
                val item = arr.get(i) as JSONObject
                if(item.getString("key").equals("fetch-interval",true)) {
                    minutes = item.getInt("value")
                    break
                }
            }

            val fetchInterval = minutes.times(1000)
            tag("leaderboard fetchInterval: $fetchInterval")
            val response by lazyDeferred {
                repository.getList(fetchInterval)
            }
            setSuccess(response,"leaderboard")
        } catch (e: Exception) {
            setLoading(false)
            setError(e.message.toString(),SNACKBAR)
        }
    }
}