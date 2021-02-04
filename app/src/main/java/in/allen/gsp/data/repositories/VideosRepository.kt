package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.Video
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.data.network.YTApi
import `in`.allen.gsp.utils.AppPreferences
import `in`.allen.gsp.utils.Coroutines
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class VideosRepository(
    private val ytApi: YTApi,
    private val api: Api,
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
        channelId:String,
        fetchInterval: Int
    ): LiveData<List<Video>> {
        return withContext(Dispatchers.IO) {
            fetchVideos(channelId,fetchInterval)
            db.getVideoDao().getList(channelId)
        }
    }

    private suspend fun fetchVideos(
        channelId:String,
        fetchInterval: Int
    ) {
        if(isFetchNeeded(channelId,fetchInterval)) {
            try {
                val response = apiRequest {
                    api.getVideos(channelId)
                }
                videos.postValue(response?.let {
                    createData(it,fetchInterval,channelId)
                })
            } catch (e: Exception) {}
        }
    }

    private fun isFetchNeeded(channelId: String,fetchInterval: Int): Boolean {
        val savedAt: Long = when {
            channelId.equals("UCL3FND-1oDhcru5AMpLGDiQ",true) -> {
                preferences.timestampChannel2
            }
            channelId.equals("UC5kS6RDXzNdftVReekcRJCA",true) -> {
                preferences.timestampChannel3
            }
            else -> {
                preferences.timestampChannel1
            }
        }
        val diff: Long = System.currentTimeMillis() - savedAt
        return diff > fetchInterval
    }

    private fun saveDBVideos(video: List<Video>) {
        Coroutines.io {
            db.getVideoDao().setList(video)
        }
    }

    private fun createData(data: String, fetchInterval: Int,channelId: String): List<Video> {
        val list = mutableListOf<Video>()
        val response = JSONObject(data)

        if(response.getInt("status") == 1) {
            val arr = response.getJSONArray("data")
            if(arr.length() > 0) {
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject
                    val video = Video(
                        item.getString("videoId"),
                        item.getString("title"),
                        item.getString("channelTitle"),
                        item.getString("channelId"),
                        item.getString("publishedAt"),
                        item.getString("description"),
                        item.getString("thumb")
                    )
                    list.add(video)
                }

                val timestamp = System.currentTimeMillis().plus(fetchInterval)
                when {
                    channelId.equals("UCL3FND-1oDhcru5AMpLGDiQ",true) -> {
                        preferences.timestampChannel2 = timestamp
                    }
                    channelId.equals("UC5kS6RDXzNdftVReekcRJCA",true) -> {
                        preferences.timestampChannel3 = timestamp
                    }
                    else -> {
                        preferences.timestampChannel1 = timestamp
                    }
                }
            }
        }

        return list
    }

    suspend fun getVideoDetails(params: Map<String, String>): String? {
        return apiRequest {
            ytApi.video(params)
        }
    }

    suspend fun getComments(params: Map<String, String>): String? {
        return apiRequest {
            ytApi.comments(params)
        }
    }


    suspend fun getPlaylists(params: Map<String, String>): String? {
        return apiRequest {
            ytApi.playlists(params)
        }
    }

    suspend fun getChannelList(user_id: Int): String? {
        return apiRequest {
            api.getChannels(user_id)
        }
    }
}