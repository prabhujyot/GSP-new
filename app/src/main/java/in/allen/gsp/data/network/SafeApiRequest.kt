package `in`.allen.gsp.data.network

import `in`.allen.gsp.utils.ApiException
import `in`.allen.gsp.utils.tag
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response

abstract class SafeApiRequest {
    private val TAG = SafeApiRequest::class.java.name

    suspend fun<T:Any> apiRequest(call: suspend () -> Response<T>) : T? {
        val response = call.invoke()
        tag("$TAG response $response")

        if(response.isSuccessful) {
            return response.body()
        } else {
            val error = response.errorBody()?.string()
            val message = StringBuilder()
            error.let {
                try {
                    message.append(it?.let { it1 -> JSONObject(it1).getString("message") })
                } catch (e: JSONException) {}
                message.append("\n")
            }
            message.append("Error Code: ${response.code()}")
            throw ApiException(message.toString())
        }
    }

}