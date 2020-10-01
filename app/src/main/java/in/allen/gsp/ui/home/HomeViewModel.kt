package `in`.allen.gsp.ui.home

import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class HomeViewModel(
    private val repository: UserRepository
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

    fun setError(error: String) {
        _error.value = Resource.Error(error)
    }

}