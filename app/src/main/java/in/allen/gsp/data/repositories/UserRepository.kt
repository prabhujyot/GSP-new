package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest


class UserRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    suspend fun login(params: Map<String, String>): String? {
        return apiRequest {
            api.authentication(params)
        }
    }

    suspend fun setDBUser(user: User) = db.getUserDao().upsert(user)

    suspend fun getDBUser() = db.getUserDao().getUser()

    suspend fun profile(user_id: Int): String? {
        return apiRequest {
            api.profile(user_id)
        }
    }


//    fun login(name: String, email: String, avatar: String, firebaseUid: String): LiveData<String> {
//        val loginResponse = MutableLiveData<String>()
//        api.authentication(name,email,avatar,firebaseUid)
//            .enqueue(object: Callback<ResponseBody> {
//                override fun onResponse(
//                    call: Call<ResponseBody>?,
//                    response: Response<ResponseBody>?
//                ) {
//                    if(response?.isSuccessful!!) {
//                        loginResponse.value = response.body().string()
//                    } else {
//                        loginResponse.value = response.errorBody().string()
//                    }
//                }
//
//                override fun onFailure(call: Call<ResponseBody>?, t: Throwable?) {
//                    loginResponse.value = t?.message
//                }
//            })
//        return loginResponse
//    }

}