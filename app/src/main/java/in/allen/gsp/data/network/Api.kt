package `in`.allen.gsp.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*

interface Api {

    companion object {
        operator fun invoke(networkConnectionInterceptor: NetworkConnectionInterceptor): Api {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkConnectionInterceptor)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://www.klipinterest.com/gsp-admin/index.php/android_v2/")
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
        @FieldMap params: Map<String, String>
    ): Response<String>

    // user stats
    @FormUrlEncoded
    @POST("profile")
    suspend fun profile(
        @Field("user_id") user_id: Int
    ): Response<String>

    // update user
    @FormUrlEncoded
    @POST("update_user")
    suspend fun updateProfile(
        @FieldMap params: Map<String, String>
    ): Response<String>

    // otp
    @FormUrlEncoded
    @POST("otp")
    suspend fun otp(
        @Field("user_id") user_id: Int,
        @Field("mobile") mobile: String
    ): Response<String>

    // mobile verification
    @FormUrlEncoded
    @POST("mobile_verification")
    suspend fun verifyMobile(
        @Field("user_id") user_id: Int,
        @Field("mobile") mobile: String,
        @Field("otp") otp: String
    ): Response<String>


    // leaderboard
    @GET("leaderboard")
    suspend fun leaderboard(): Response<String>

}