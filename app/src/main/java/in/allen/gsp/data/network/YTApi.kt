package `in`.allen.gsp.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface YTApi {

    companion object {
        operator fun invoke(networkConnectionInterceptor: NetworkConnectionInterceptor): YTApi {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkConnectionInterceptor)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://www.googleapis.com/youtube/v3/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YTApi::class.java)
        }
    }

    // playlist
    @GET("playlistItems")
    suspend fun playlist(
        @QueryMap params:Map<String, String>
    ): Response<String>

    // video data
    @GET("videos")
    suspend fun video(
        @QueryMap params:Map<String, String>
    ): Response<String>

    // video comments
    @GET("commentThreads")
    suspend fun comments(
        @QueryMap params:Map<String, String>
    ): Response<String>

}