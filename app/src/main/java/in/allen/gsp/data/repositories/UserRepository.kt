package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.SafeApiRequest
import `in`.allen.gsp.data.network.responses.AuthenticationResponse
import org.json.JSONObject


class UserRepository(
    private val api: Api,
    private val db: AppDatabase
): SafeApiRequest() {

    suspend fun login(postObj: JSONObject): AuthenticationResponse {
        return apiRequest {
            api.authentication(postObj)
        }
    }

    suspend fun saveUser(user: User) = db.getUserDao().upsert(user)

    fun getUser() = db.getUserDao().getUser()

}