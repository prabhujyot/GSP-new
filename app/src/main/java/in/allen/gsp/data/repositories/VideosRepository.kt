package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.Video
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.data.network.YTApi
import `in`.allen.gsp.utils.AppPreferences
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class VideosRepository(
    private val api: YTApi,
    private val db: AppDatabase,
    private val preferences: AppPreferences
): SafeApiRequest() {

    private val videos = MutableLiveData<List<Video>>()

    init {
        videos.observeForever {
            saveDBVideos(it)
        }
    }

    suspend fun getVideos(
        params: Map<String, String>,
        fetchInterval: Int
    ): LiveData<List<Video>> {
        return withContext(Dispatchers.IO) {
            fetchVideos(params,fetchInterval)
            db.getVideoDao().getList(params["playlistId"] ?: error(""))
        }
    }

    private suspend fun fetchVideos(
        params:Map<String, String>,
        fetchInterval: Int
    ) {
        if(isFetchNeeded(fetchInterval)) {
            try {
                val response = apiRequest {
                    api.playlist(params)
                }
                videos.postValue(response?.let { createData(it,fetchInterval) })
            } catch (e: Exception) {}
        }
    }

    private fun isFetchNeeded(fetchInterval: Int): Boolean {
        val savedAt = preferences.timestampVideos
        val diff: Long = System.currentTimeMillis() - savedAt
        return diff > fetchInterval
    }

    private fun saveDBVideos(video: List<Video>) {
        Coroutines.io {
            db.getVideoDao().setList(video)
        }
    }

    private fun createData(data: String, fetchInterval: Int): List<Video> {
        val list = mutableListOf<Video>()
        val response = JSONObject(data)
        if(!response.has("error")) {
            val arr = response.getJSONArray("items")
            tag(""+ arr + " : " + arr.length())
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject

                    var thumbnails = ""
                    if(item.getJSONObject("snippet").getJSONObject("thumbnails").has("medium")) {
                        thumbnails = item.getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("medium").getString("url")
                    }
                    val video = Video(
                        item.getJSONObject("snippet").getJSONObject("resourceId").getString("videoId"),
                        item.getJSONObject("snippet").getString("title"),
                        item.getJSONObject("snippet").getString("channelTitle"),
                        item.getJSONObject("snippet").getString("playlistId"),
                        item.getJSONObject("snippet").getString("publishedAt"),
                        item.getJSONObject("snippet").getString("description"),
                        thumbnails
                    )
                    list.add(video)
                }
            }
            val timestamp = System.currentTimeMillis().plus(fetchInterval)
            preferences.timestampVideos = timestamp
        }
        return list
    }

    suspend fun getVideoDetails(params: Map<String, String>): String? {
        return apiRequest {
            api.video(params)
        }
    }

    suspend fun getComments(params: Map<String, String>): String? {
        return apiRequest {
            api.comments(params)
        }
    }
}