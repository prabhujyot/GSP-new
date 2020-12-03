package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject


class UserRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    val userLife = MutableLiveData<HashMap<String,Long>>()

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

    suspend fun updateProfile(params: HashMap<String, String>): String? {
        return apiRequest {
            api.updateProfile(params)
        }
    }

    suspend fun otp(user_id: Int, mobile: String): String? {
        return apiRequest {
            api.otp(user_id,mobile)
        }
    }

    suspend fun verifyMobile(user_id: Int, mobile: String, otp: String): String? {
        return apiRequest {
            api.verifyMobile(user_id,mobile,otp)
        }
    }

    suspend fun config(key: String): String {
        var value = ""
        val dbUser = getDBUser()
        if (dbUser != null) {
            try {
                val arr = JSONArray(dbUser.config)
                for(i in 0 until arr.length()) {
                    val item = arr.get(i) as JSONObject
                    if(item.getString("key").equals(key,true)) {
                        value = item.getString("value")
                        break
                    }
                }
            } catch (e: Exception) {}
        }
        return value
    }
}