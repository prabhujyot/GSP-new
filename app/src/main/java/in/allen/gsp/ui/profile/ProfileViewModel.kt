package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class ProfileViewModel(
    private val repository: UserRepository
): ViewModel() {

    private val TAG = ProfileViewModel::class.java.name

    var username: String? = null
    var mobile: String? = null
    var location: String? = null
    var quote: String? = null


    // interaction with activity
    val _loading = MutableLiveData<Resource.Loading<String>>()
    val _stats = MutableLiveData<Resource.Success<JSONObject>>()
    val _user = MutableLiveData<Resource.Success<User>>()
    val _error = MutableLiveData<Resource.Error<String>>()

    fun statsData() {
        // get user from db
        viewModelScope.launch {
            val dbUser = repository.getDBUser()
            if (dbUser != null) {
                _user.value = Resource.Success(dbUser)
                _loading.value = Resource.Loading()
                try {
                    val response = repository.profile(dbUser.user_id)
                    tag("response: $response")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if(responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            _stats.value = Resource.Success(data)
                        } else {
                            _error.value = Resource.Error(responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = e.message?.let { Resource.Error(it) }
                }
            } else {
                _error.value = Resource.Error("Not found")
            }
        }
    }

}