package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.db.entities.Video
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.data.network.YTApi
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.paging.PagingSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject


class VideosRepository(
    private val api: YTApi,
    private val db: AppDatabase
): SafeApiRequest() {

    private val videos = MutableLiveData<List<Video>>()

    init {
        videos.observeForever {
            saveVideos(it)
        }
    }

    suspend fun getVideos(
        params:Map<String, String>
    ): LiveData<List<Video>> {
        return withContext(Dispatchers.IO) {
            fetchVideos(params)
            db.getVideoDao().getList(params["playlistId"] ?: error(""))
        }
    }

    private suspend fun fetchVideos(
        params:Map<String, String>
    ) {
        if(isFetchNeeded()) {
            val response = apiRequest {
                api.playlist(params)
            }
            videos.postValue(response?.let { createData(it) })
        }
    }

    private fun isFetchNeeded(): Boolean {
        return true
    }

    private fun saveVideos(video: List<Video>) {
        Coroutines.io {
            db.getVideoDao().setList(video)
        }
    }

    private fun createData(data: String): List<Video> {
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
        }
        return list
    }








    suspend fun getLiveVideos(
        params:Map<String, String>
    ): PagingSource.LoadResult<String, Video> {
        return try {
            val response = apiRequest {
                api.playlist(params)
            }

            val obj = JSONObject(response.toString())
            val prevKey = if (obj.has("prevPageToken")) obj.getString("prevPageToken") else null
            val nextKey = if (obj.has("nextPageToken")) obj.getString("nextPageToken") else null
            PagingSource.LoadResult.Page(
                createData(response!!),
                prevKey,
                nextKey)

//            LoadResult.Page(
//                data = response.data,
//                prevKey = if (nextPageNumber > 0) nextPageNumber - 1 else null,
//                nextKey = if (nextPageNumber < response.totalPages) nextPageNumber + 1 else null
//            )
        } catch (e: Exception) {
            PagingSource.LoadResult.Error(e)
        }
    }

}