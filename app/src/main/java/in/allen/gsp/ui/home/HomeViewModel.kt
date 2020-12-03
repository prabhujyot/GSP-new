package `in`.allen.gsp.ui.home

import `in`.allen.gsp.data.repositories.BannerRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class HomeViewModel(
    private val userRepository: UserRepository,
    private val bannerRepository: BannerRepository
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

    fun bannerData(user_id: Int) {
        val response by lazyDeferred {
            bannerRepository.getList(user_id)
        }
        setSuccess(response,"banner")
    }

    // attachment timer
    private var lifeTimer: CountDownTimer?= null
    fun lifeTimerStart(i: Long) {
        lifeTimer = object : CountDownTimer(i, 10) {
            override fun onTick(l: Long) {
                setSuccess(l,"lifeTimer")
            }

            override fun onFinish() {
                lifeTimerCancel()
            }
        }
        (lifeTimer as CountDownTimer).start()
    }

    fun lifeTimerCancel() {
        lifeTimer?.cancel()
    }
}