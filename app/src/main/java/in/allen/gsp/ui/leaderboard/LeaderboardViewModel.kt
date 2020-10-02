package `in`.allen.gsp.ui.leaderboard

import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class LeaderboardViewModel(
    private val repository: LeaderboardRepository
): ViewModel() {

    private val TAG = LeaderboardViewModel::class.java.name

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

    fun leaderboard() {
        val response by lazyDeferred {
            repository.getList()
        }
        _success.value = Resource.Success(response)
    }

}