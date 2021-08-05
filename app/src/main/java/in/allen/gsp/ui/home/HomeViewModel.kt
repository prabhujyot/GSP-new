package `in`.allen.gsp.ui.home

import `in`.allen.gsp.data.repositories.BannerRepository
import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.data.repositories.MessageRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HomeViewModel(
    private val userRepository: UserRepository,
    private val bannerRepository: BannerRepository,
    private val leaderboardRepository: LeaderboardRepository,
    private val messageRepository: MessageRepository
): ViewModel() {

    var admin: Boolean = false
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
                admin = dbUser.is_admin
                messageRepository.getUnreadCount(dbUser.user_id)
                setSuccess(dbUser,"user")
            } else {
                setError("Not found",SNACKBAR)
            }
        }
    }

    fun bannerData(user_id: Int) {
        val response by lazyDeferred {
            bannerRepository.getList(user_id)
        }
        setSuccess(response,"banner")
    }

    fun tileData() {
        viewModelScope.launch {
            delay(1000)
            val response = bannerRepository.getTileList()
            setSuccess(response,"tiles")
        }
    }

    fun leaderboard() {
        viewModelScope.launch {
            val minutes= userRepository.config("fetch-interval")
            val fetchInterval = minutes.toInt().times(60).times(1000)
            val response by lazyDeferred {
                leaderboardRepository.getList(fetchInterval)
            }
            setSuccess(response,"leaderboard")
        }
    }

}