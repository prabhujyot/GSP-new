package `in`.allen.gsp.ui.home

import `in`.allen.gsp.data.repositories.BannerRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


class HomeViewModel(
    private val userRepository: UserRepository,
    private val bannerRepository: BannerRepository
): ViewModel() {

    private val TAG = HomeViewModel::class.java.name

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
                _success.value = Resource.Success(dbUser,"user")
            } else {
                _error.value = Resource.Error("Not found")
            }
        }
    }

    fun bannerData(user_id: Int) {
        val response by lazyDeferred {
            bannerRepository.getList(user_id)
        }
        _success.value = Resource.Success(response,"banner")
    }
}