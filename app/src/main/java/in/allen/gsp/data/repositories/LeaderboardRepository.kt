package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.Leaderboard
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.utils.AppPreferences
import `in`.allen.gsp.utils.Coroutines
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class LeaderboardRepository(
    private val api: Api,
    private val db: AppDatabase,
    private val preferences: AppPreferences
): SafeApiRequest() {

    private val leaderboard = MutableLiveData<List<Leaderboard>>()

    init {
        leaderboard.observeForever {
            setDBList(it)
        }
    }

    suspend fun getList(fetchInterval: Int): LiveData<List<Leaderboard>> {
        return withContext(Dispatchers.IO) {
            fetchList(fetchInterval)
            db.getLeaderboardDao().getList()
        }
    }

    private suspend fun fetchList(fetchInterval: Int) {
        if(isFetchNeeded(fetchInterval)) {
            try {
                val response = apiRequest {
                    api.leaderboard()
                }
                leaderboard.postValue(response?.let { createData(it, fetchInterval) })
            } catch (e: Exception) {}
        }
    }

    private fun isFetchNeeded(fetchInterval: Int): Boolean {
        val savedAt = preferences.timestampLeaderboard
        val diff: Long = System.currentTimeMillis() - savedAt
        return diff > fetchInterval
    }

    private fun setDBList(list: List<Leaderboard>) {
        Coroutines.io {
            db.getLeaderboardDao().clearList()
            db.getLeaderboardDao().setList(list)
        }
    }

    private fun createData(data: String, fetchInterval: Int): List<Leaderboard> {
        val list = mutableListOf<Leaderboard>()
        val response = JSONObject(data)
        if(response.getInt("status") == 1) {
            val arr = response.getJSONArray("data")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject
                    val leaderboard = Leaderboard(
                        item.getInt("rank"),
                        item.getString("user_id"),
                        item.getString("name"),
                        item.getString("avatar"),
                        item.getInt("score")
                    )
                    list.add(leaderboard)
                }
            }
            val timestamp = System.currentTimeMillis().plus(fetchInterval)
            preferences.timestampLeaderboard = timestamp
        }
        return list
    }
}