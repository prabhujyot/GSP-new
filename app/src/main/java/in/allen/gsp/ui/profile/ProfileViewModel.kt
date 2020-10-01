package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.tag
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class ProfileViewModel(
    private val repository: UserRepository
): ViewModel() {

    private val TAG = ProfileViewModel::class.java.name

    private var user: User ?= null

    // interaction with activity
    private val _loading = MutableLiveData<Resource.Loading<String>>()
    private val _error = MutableLiveData<Resource.Error<String>>()
    private val _success = MutableLiveData<Resource.Success<Any>>()

    fun stateLoading(): LiveData<Resource.Loading<String>> {
        return _loading
    }

    fun stateError(): LiveData<Resource.Error<String>> {
        return _error
    }

    fun stateSuccess(): LiveData<Resource.Success<Any>> {
        return _success
    }


    // user info
    fun userData() {
        viewModelScope.launch {
            val dbUser = repository.getDBUser()
            if (dbUser != null) {
                user = dbUser
                _success.value = Resource.Success(dbUser,"user")
            } else {
                _error.value = Resource.Error("Not found")
            }
        }
    }

    fun statsData(user_id: Int) {
        _loading.value = Resource.Loading("")
        viewModelScope.launch {
            try {
                val response = repository.profile(user_id)
                tag("response: $response")
                if (response != null) {
                    val responseObj = JSONObject(response)
                    tag("responseObj: $responseObj")
                    if (responseObj.getInt("status") == 1) {
                        val data = responseObj.getJSONObject("data")
                        _success.value = Resource.Success(data,"stats")
                    } else {
                        _error.value = Resource.Error(responseObj.getString("message"))
                    }
                }
            } catch (e: Exception) {
                _error.value = e.message?.let { Resource.Error(it) }
            }
        }
    }

    fun updateProfile(params: HashMap<String, String>) {
        if(user != null && user?.user_id!! > 0) {
            _loading.value = Resource.Loading("")
            viewModelScope.launch {
                try {
                    params["user_id"] = user?.user_id!!.toString()
                    val response = repository.updateProfile(params)
                    tag("response: $response")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getString("data")
                            _success.value = Resource.Success(data,"updateProfile")

                            user?.location = params["location"].toString()
                            user?.about = params["about"].toString()
                            repository.setDBUser(user!!)
                        } else {
                            _error.value = Resource.Error(responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = e.message?.let { Resource.Error(it) }
                }
            }
        }
    }

    fun verifyMobile(mobile: String, otp: String) {
        if(user != null && user?.user_id!! > 0) {
            _loading.value = Resource.Loading("")
            viewModelScope.launch {
                try {
                    val response = repository.verifyMobile(user?.user_id!!, mobile, otp)
                    tag("response: $response")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getString("message")
                            _success.value = Resource.Success(data,"verifyMobile")

                            user?.mobile = mobile
                            user?.is_verified = 1
                            repository.setDBUser(user!!)
                        } else {
                            _error.value = Resource.Error(responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = e.message?.let { Resource.Error(it) }
                }
            }
        }
    }

    fun getOTP(mobile: String) {
        if(user != null && user?.user_id!! > 0) {
            _loading.value = Resource.Loading("")
            viewModelScope.launch {
                try {
                    val response = repository.otp(user?.user_id!!, mobile)
                    tag("response: $response")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getString("data")
                            _success.value = Resource.Success(data, "getOTP")

                            user?.mobile = mobile
                            user?.is_verified = 0
                            repository.setDBUser(user!!)
                        } else {
                            _error.value = Resource.Error(responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = e.message?.let { Resource.Error(it) }
                }
            }
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
                _success.value = Resource.Success(hms,"disableOTP")
            }

            override fun onFinish() {
                _success.value = Resource.Success("Resend OTP","enableOTP")
                countDownTimer?.cancel()
            }
        }
        (countDownTimer as CountDownTimer).start()
    }

    fun countdownCancel() {
        countDownTimer?.cancel()
    }

}