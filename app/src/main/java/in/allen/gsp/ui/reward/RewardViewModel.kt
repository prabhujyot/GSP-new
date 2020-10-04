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

    private val TAG = RewardViewModel::class.java.name
    private var user: User?= null

    // interaction with activity
    private val _loading = MutableLiveData<Resource.Loading<String>>()
    private val _success = MutableLiveData<Resource.Success<Any>>()
    private val _error = MutableLiveData<Resource.Error<String>>()

    fun stateLoading(): LiveData<Resource.Loading<String>> {
        return _loading
    }

    fun stateError(): LiveData<Resource.Error<String>> {
        return _error
    }

    fun stateSuccess(): LiveData<Resource.Success<Any>> {
        return _success
    }


    fun userData() {
        viewModelScope.launch {
            val dbUser = userRepository.getDBUser()
            if (dbUser != null) {
                user = dbUser
                _success.value = Resource.Success(dbUser, "user")
            } else {
                _error.value = Resource.Error("message","Not found")
            }
        }
    }

    fun getStatement(type: String, page: Int) {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                try {
                    val response = rewardRepository.getStatement(user?.user_id!!, type, page)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            _success.value = Resource.Success(data, "statement")
                        } else {
                            _error.value = Resource.Error("message", responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = Resource.Error("exception","$TAG ${e.message}")
                }
            }
        }
    }

    private fun getDailyReward() {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                try {
                    val response = rewardRepository.getDailyReward(user?.user_id!!,"earn")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            _success.value = Resource.Success(data, "getDailyReward")
                        } else {
                            _error.value = Resource.Error("message", responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = Resource.Error("exception","$TAG ${e.message}")
                }
            }
        }
    }

    private fun setDailyReward(value: Int) {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                try {
                    val response = rewardRepository.setDailyReward(user?.user_id!!, "earn", value)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if (responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            _success.value = Resource.Success(data, "setDailyReward")
                        } else {
                            _error.value = Resource.Error("message",responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = Resource.Error("exception","$TAG ${e.message}")
                }
            }
        }
    }


    fun redeem(coins: Int) {
        if(user != null && user?.user_id!! > 0) {
            if(coins <= user?.coins!!) {
                viewModelScope.launch {
                    _loading.value = Resource.Loading("")
                    try {
                        val response = rewardRepository.redeem(user?.user_id!!, coins)
                        if (response != null) {
                            val responseObj = JSONObject(response)
                            if (responseObj.getInt("status") == 1) {
                                user?.coins = responseObj.getInt("data")
                                userRepository.setDBUser(user!!)
                                _success.value = Resource.Success(responseObj.getString("message"),"redeem")
                            } else {
                                _error.value = Resource.Error("message",responseObj.getString("message"))
                            }
                        }
                    } catch (e: Exception) {
                        _error.value = Resource.Error("exception","$TAG ${e.message}")
                    }
                }
            } else {
                _error.value = Resource.Error("message","$coins GSP Coins required to get coupon")
            }
        }
    }



    fun getConfig(key: String): String {
        var value = ""
        if(user != null && user?.user_id!! > 0) {
            val configData = user?.config!!
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
                    _error.value = Resource.Error("exception","$TAG ${e.message}")
                }
            }
        }
        return value
    }

}