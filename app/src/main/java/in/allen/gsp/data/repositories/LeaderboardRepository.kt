package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.db.entities.Leaderboard
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class LeaderboardRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    private val leaderboard = MutableLiveData<List<Leaderboard>>()

    init {
        leaderboard.observeForever {
            setDBList(it)
        }
    }

    suspend fun getList(): LiveData<List<Leaderboard>> {
        return withContext(Dispatchers.IO) {
            fetchList()
            db.getLeaderboardDao().getList()
        }
    }

    private suspend fun fetchList() {
        if(isFetchNeeded()) {
            val response = apiRequest {
                api.leaderboard()
            }

            tag("response : $response")

            leaderboard.postValue(response?.let { createData(it) })
        }
    }

    private fun isFetchNeeded(): Boolean {
        return true
    }

    private fun setDBList(list: List<Leaderboard>) {
        Coroutines.io {
            db.getLeaderboardDao().setList(list)
        }
    }

    private fun createData(data: String): List<Leaderboard> {
        val list = mutableListOf<Leaderboard>()
        val response = JSONObject(data)

        tag("" + response.has("status") + " : $response")

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
        }
        return list
    }

}