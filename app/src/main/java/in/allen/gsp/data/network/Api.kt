package `in`.allen.gsp.data.network

import `in`.allen.gsp.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface Api {

    companion object {
        operator fun invoke(networkConnectionInterceptor: NetworkConnectionInterceptor): Api {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(networkConnectionInterceptor)
                .addInterceptor(logging)
                .connectTimeout(1, TimeUnit.MINUTES)
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
        @FieldMap params: Map<String, String>
    ): Response<String>

    // user stats
    @GET("profile")
    suspend fun profile(
        @Query("user_id") user_id: Int
    ): Response<String>

    // update user
    @FormUrlEncoded
    @POST("profile")
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

    // banners
    @GET("banners")
    suspend fun banners(
        @Query("user_id") user_id: Int
    ): Response<String>

    // leaderboard
    @GET("leaderboard")
    suspend fun leaderboard(): Response<String>

    // statements
    @GET("transactions")
    suspend fun getTransactions(
        @Query("user_id") user_id: Int,
        @Query("type") type: String,
        @Query("page") page: Int,
    ): Response<String>

    // transaction status update
    @FormUrlEncoded
    @POST("transactions")
    suspend fun updateTransactionStatus(
        @Field("id") id: Int,
        @Field("status") status: Int
    ): Response<String>
    
    // daily reward get status
    @GET("dailyreward")
    suspend fun getDailyReward(
        @Query("user_id") user_id: Int,
        @Query("type") type: String
    ): Response<String>

    // post daily reward value
    @FormUrlEncoded
    @POST("dailyreward")
    suspend fun setDailyReward(
        @Field("user_id") user_id: Int,
        @Field("type") type: String,
        @Field("value") value: Int
    ): Response<String>

    // redeem coins
    @FormUrlEncoded
    @POST("redeem")
    suspend fun redeem(
        @Field("user_id") user_id: Int,
        @Field("value") value: Int
    ): Response<String>

    // preview question set
    @GET("preview")
    suspend fun getPreview(
        @Query("user_id") user_id: Int
    ): Response<String>

    // quiz question set
    @GET("quiz")
    suspend fun getQuiz(
        @Query("user_id") user_id: Int
    ): Response<String>

    // wild question set
    @GET("wset")
    suspend fun getWQset(
        @Query("user_id") user_id: Int,
        @Query("value") value: Int
    ): Response<String>

    // offer purchase
    @GET("offer")
    suspend fun getOffer(
        @Query("user_id") user_id: Int,
        @Query("value") value: Int
    ): Response<String>

    // save quiz data
    @FormUrlEncoded
    @POST("quiz")
    suspend fun postQuiz(
        @FieldMap params: Map<String, String>
    ): Response<String>

    // scratchcards
    @GET("scratchcard")
    suspend fun getScratchcards(
        @Query("user_id") user_id: Int,
        @Query("page") page: Int,
    ): Response<String>

    // contest status
    @GET("contest_status")
    suspend fun contestStatus(
        @Query("user_id") user_id: Int,
        @Query("contest_id") contest_id: Int
    ): Response<String>

    // contest enroll
    @FormUrlEncoded
    @POST("contest_enrol")
    suspend fun contestEnrol(
        @Field("user_id") user_id: Int,
        @Field("contest_id") contest_id: Int
    ): Response<String>

    // get contest data
    @GET("contest")
    suspend fun getContest(
        @Query("user_id") user_id: Int,
        @Query("contest_id") contest_id: Int
    ): Response<String>

    // save contest data
    @FormUrlEncoded
    @POST("contest")
    suspend fun postContest(
        @FieldMap params: Map<String, String>
    ): Response<String>

}