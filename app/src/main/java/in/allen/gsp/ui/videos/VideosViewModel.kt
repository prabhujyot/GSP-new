package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.repositories.VideosRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class VideosViewModel(
    private val userRepository: UserRepository,
    private val repository: VideosRepository
) : ViewModel() {

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

    init {
        userData()
    }

    fun userData() {
        viewModelScope.launch {
            val dbUser = userRepository.getDBUser()
            if (dbUser != null) {
                setSuccess(dbUser,"user")
            }
        }
    }

    fun channelList(user_id: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getChannelList(user_id)
                if (response != null) {
                    val responseObj = JSONObject(response)
                    if(responseObj.getInt("status") == 1) {
                        val dataArr = responseObj.getJSONArray("data")
                        setSuccess(dataArr,"channelList")
                    } else {
                        setError("${responseObj.getString("message")}", ALERT)
                    }
                }
            } catch (e: Exception) {
                setError("${e.message}",ALERT)
            }
        }
    }


    fun videoList(channelId:String) {
        viewModelScope.launch {
            val minutes= userRepository.config("fetch-interval")
            val fetchInterval = minutes.toInt().times(60).times(1000)
            val response by lazyDeferred {
                repository.getVideos(channelId,fetchInterval)
            }
            setSuccess(response,"videoList")
        }
    }
}