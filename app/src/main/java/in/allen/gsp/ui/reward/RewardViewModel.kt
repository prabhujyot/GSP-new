package `in`.allen.gsp.ui.reward

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject


class RewardViewModel(
    private val userRepository: UserRepository,
    private val rewardRepository: RewardRepository
): ViewModel() {

    private val ALERT = "alert"
    private val SNACKBAR = "snackbar"
    private val TAG = "tag"
    private val TOAST = "toast"

    private var userData: User?= null

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

    fun setUserData(user: User) {
        userData = user
    }

    fun getUserData(): User? {
        return userData
    }


    fun userData() {
        viewModelScope.launch {
            val dbUser = userRepository.getDBUser()
            if (dbUser != null) {
                setUserData(dbUser)
                setSuccess(dbUser,"user")
            } else {
                setError("Not Found",TAG)
            }
        }
    }

    fun getStatement(type: String, page: Int) {
        val user = getUserData()
        if(user != null && user.user_id > 0) {
            viewModelScope.launch {
                try {
                    val response = rewardRepository.getStatement(user.user_id, type, page)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            setSuccess(data,"statement")
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

    fun getDailyReward() {
        val user = getUserData()
        if(user != null && user.user_id > 0) {
            viewModelScope.launch {
                try {
                    val response = rewardRepository.getDailyReward(user.user_id,"earn")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            user.coins = data.getInt("total_coins")
                            userRepository.setDBUser(user)
                            setSuccess(user,"user")
                            setSuccess(data,"getDailyReward")
                        } else {
                            setError(responseObj.getString("message"), SNACKBAR)
                        }
                    }
                } catch (e: Exception) {
                    setError("${e.message}", ALERT)
                }
            }
        }
    }

    fun setDailyReward(value: Int) {
        val user = getUserData()
        if(user != null && user.user_id > 0) {
            viewModelScope.launch {
                setLoading(true)
                try {
                    val response = rewardRepository.setDailyReward(user.user_id, "earn", value)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            user.coins = data.getInt("total_coins")
                            userRepository.setDBUser(user)
                            setSuccess(user,"user")
                            setSuccess(data, "setDailyReward")
                        } else {
                            setError(responseObj.getString("message"), SNACKBAR)
                        }
                    }
                } catch (e: Exception) {
                    setError("${e.message}", ALERT)
                }
            }
        }
    }


    fun redeem(coins: Int) {
        val user = getUserData()
        if(user != null && user.user_id > 0) {
            if(coins <= user.coins) {
                viewModelScope.launch {
                    setLoading(true)
                    try {
                        val response = rewardRepository.redeem(user.user_id, coins)
                        if (response != null) {
                            val responseObj = JSONObject(response)
                            if (responseObj.getInt("status") == 1) {
                                user.coins = responseObj.getInt("data")
                                userRepository.setDBUser(user)
                                setSuccess(user,"user")
                                setSuccess(responseObj.getString("message"),"redeem")
                            } else {
                                setError(responseObj.getString("message"), SNACKBAR)
                            }
                        }
                    } catch (e: Exception) {
                        setError("${e.message}", ALERT)
                    }
                }
            } else {
                setError("$coins GSP Coins required to get coupon", SNACKBAR)
            }
        }
    }



    fun getConfig(key: String): String {
        var value = ""
        val user = getUserData()
        if(user != null && user.user_id > 0) {
            val configData = user.config
            if(configData.trim().length > 5) {
                try {
                    val arr = JSONArray(configData)
                    if(arr.length() > 0) {
                        for (i in 0 until arr.length()) {
                            val obj = arr[i] as JSONObject
                            if(obj.getString("key").equals(key,true)) {
                                value = obj.getString("value")
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    setError("${e.message}", ALERT)
                }
            }
        }
        return value
    }

}