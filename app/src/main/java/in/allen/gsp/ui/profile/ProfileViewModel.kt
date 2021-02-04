package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class ProfileViewModel(
    private val repository: UserRepository,
    private val rewardRepository: RewardRepository
): ViewModel() {

    val ALERT = "alert"
    val SNACKBAR = "snackbar"
    val TAG = "tag"
    val TOAST = "toast"

    var user: User?= null
    var flagScratchcard = false

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
        _success.value = Resource.Success(data, filter)
    }


    // user info
    fun userData() {
        viewModelScope.launch {
            val dbUser = repository.getDBUser()
            if (dbUser != null) {
                user = dbUser
                setSuccess(dbUser, "user")
            } else {
                setError("Not Found", TAG)
            }
        }
    }

    fun statsData(user_id: Int) {
        setLoading(true)
        viewModelScope.launch {
            try {
                val response = repository.profile(user_id)
                if (response != null) {
                    val responseObj = JSONObject(response)
                    if (responseObj.getInt("status") == 1) {
                        val data = responseObj.getJSONObject("data")
                        setSuccess(data,"stats")
                    } else {
                        setError(responseObj.getString("message"),TAG)
                    }
                }
            } catch (e: Exception) {
                e.message?.let { setError(it,TAG) }
            }
        }
    }

    fun updateProfile(params: HashMap<String, String>) {
        if(user != null && user?.user_id!! > 0) {
            setLoading(true)
            viewModelScope.launch {
                try {
                    params["user_id"] = user?.user_id!!.toString()
                    val response = repository.updateProfile(params)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            user?.location = params["location"].toString()
                            user?.about = params["about"].toString()
                            repository.setDBUser(user!!)

                            val data = responseObj.getString("data")
                            setSuccess(data,"updateProfile")
                        } else {
                            setError(responseObj.getString("message"),SNACKBAR)
                        }
                    }
                } catch (e: Exception) {
                    e.message?.let { setError(it,TAG) }
                }
            }
        }
    }

    fun verifyMobile(mobile: String, otp: String) {
        if(user != null && user?.user_id!! > 0) {
            setLoading(true)
            viewModelScope.launch {
                try {
                    val response = repository.verifyMobile(user?.user_id!!, mobile, otp)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            user?.mobile = mobile
                            user?.is_verified = 1
                            repository.setDBUser(user!!)

                            val data = responseObj.getString("message")
                            setSuccess(data,"verifyMobile")
                        } else {
                            setError(responseObj.getString("message"),SNACKBAR)
                        }
                    }
                } catch (e: Exception) {
                    e.message?.let { setError(it,TAG) }
                }
            }
        }
    }

    fun getOTP(mobile: String) {
        if(user != null && user?.user_id!! > 0) {
            setLoading(true)
            viewModelScope.launch {
                try {
                    val response = repository.otp(user?.user_id!!, mobile)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            user?.mobile = mobile
                            user?.is_verified = 0
                            repository.setDBUser(user!!)

                            val data = responseObj.getString("data")
                            setSuccess(data, "getOTP")
                        } else {
                            setError(responseObj.getString("message"),SNACKBAR)
                        }
                    }
                } catch (e: Exception) {
                    e.message?.let { setError(it,TAG) }
                }
            }
        }
    }

    fun scratchcards(page: Int) {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                try {
                    val response = rewardRepository.getScratchcards(user?.user_id!!, page)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            setSuccess(data,"scratchcards")
                        } else {
                            setError(responseObj.getString("message"),SNACKBAR)
                        }
                    }
                } catch (e: Exception) {
                    setError("${e.message}", ALERT)
                }
            }
        }
    }

    fun setScratchcard(id: Int, status: Int) {
        viewModelScope.launch {
            rewardRepository.updateTransactionStatus(id,status)
        }
    }


    private var countDownTimer: CountDownTimer ?= null
    fun countdownStart(i: Long) {
        countDownTimer = object : CountDownTimer(i, 1000) {
            override fun onTick(l: Long) {
                val hms = String.format(
                    "%02d:%02d",
                    TimeUnit.MILLISECONDS.toMinutes(l) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(
                            l
                        )
                    ),
                    TimeUnit.MILLISECONDS.toSeconds(l) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(
                            l
                        )
                    )
                )
                setSuccess(hms,"disableOTP")
            }

            override fun onFinish() {
                setSuccess("Resend OTP","enableOTP")
                countDownTimer?.cancel()
            }
        }
        (countDownTimer as CountDownTimer).start()
    }

    fun countdownCancel() {
        countDownTimer?.cancel()
    }

}