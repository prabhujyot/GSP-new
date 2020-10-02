package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.db.entities.Contest
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.utils.Coroutines
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class ContestRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    private val contest = MutableLiveData<List<Contest>>()

    init {
        contest.observeForever {
            setDBList(it)
        }
    }

    suspend fun getList(user_id: Int): LiveData<List<Contest>> {
        return withContext(Dispatchers.IO) {
            fetchList(user_id)
            db.getContestDao().getList()
        }
    }

    private suspend fun fetchList(user_id: Int) {
        if(isFetchNeeded()) {
            val response = apiRequest {
                api.contest(user_id)
            }
            contest.postValue(response?.let { createData(it) })
        }
    }

    private fun isFetchNeeded(): Boolean {
        return true
    }

    private fun setDBList(list: List<Contest>) {
        Coroutines.io {
            db.getContestDao().setList(list)
        }
    }

    private fun createData(data: String): List<Contest> {
        val list = mutableListOf<Contest>()
        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val arr = response.getJSONArray("data")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject
                    val contest = Contest(
                        item.getInt("id"),
                        item.getString("name"),
                        item.getString("desc"),
                        item.getString("start_date"),
                        item.getString("end_date"),
                        item.getString("logo"),
                        item.getInt("enrollment_requirement"),
                        item.getString("enrollment_start_time"),
                        item.getString("enrollment_end_time"),
                        item.getInt("enrollment_max_user"),
                        item.getString("attempt_type"),
                        item.getString("contest_msg"),
                        item.getInt("status")
                    )
                    list.add(contest)
                }
            }
        }
        return list
    }

}