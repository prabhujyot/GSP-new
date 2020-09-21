package `in`.allen.gsp.data.network

import `in`.allen.gsp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST

interface Api {

    companion object {
        operator fun invoke(networkConnectionInterceptor: NetworkConnectionInterceptor): Api {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkConnectionInterceptor)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(BuildConfig.BASE_URL + "gsp-admin/index.php/android_v2/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(Api::class.java)
        }
    }

    // login
    @FormUrlEncoded
    @POST("authentication")
    suspend fun authentication(
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("avatar") avatar: String,
        @Field("firebase_uid") firebaseUid: String
    ): Response<String>

    // leaderboard
    @GET("leaderboard")
    fun leaderboard(): Call<ResponseBody>

}