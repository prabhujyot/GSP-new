package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.Banner
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.utils.Coroutines
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class BannerRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    private val banner = MutableLiveData<List<Banner>>()

    init {
        banner.observeForever {
            setDBList(it)
        }
    }

    suspend fun getList(user_id: Int): LiveData<List<Banner>> {
        return withContext(Dispatchers.IO) {
            fetchList(user_id)
            getDBList()
        }
    }

    private suspend fun fetchList(user_id: Int) {
        if(isFetchNeeded()) {
            val response = apiRequest {
                api.banners(user_id)
            }
            banner.postValue(response?.let { createData(it) })
        }
    }

    private fun isFetchNeeded(): Boolean {
        return true
    }

    private fun getDBList() =db.getBannerDao().getList()

    private fun setDBList(list: List<Banner>) {
        Coroutines.io {
            db.getBannerDao().clearList()
            db.getBannerDao().setList(list)
        }
    }

    private fun createData(data: String): List<Banner> {
        val list = mutableListOf<Banner>()
        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val arr = response.getJSONArray("data")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject
                    val banner = Banner(
                        item.getInt("id"),
                        item.getString("title"),
                        item.getString("image"),
                        item.getString("banner_type"),
                        item.getString("banner_action"),
                        item.getString("start_time"),
                        item.getString("end_time"),
                        item.getString("meta"),
                        item.getInt("status")
                    )
                    list.add(banner)
                }
            }
        }
        return list
    }

}