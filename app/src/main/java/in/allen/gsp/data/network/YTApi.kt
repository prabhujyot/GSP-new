package `in`.allen.gsp.data.network

import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.QueryMap

interface YTApi {

    companion object {
        operator fun invoke(networkConnectionInterceptor: NetworkConnectionInterceptor): YTApi {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkConnectionInterceptor)
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

}