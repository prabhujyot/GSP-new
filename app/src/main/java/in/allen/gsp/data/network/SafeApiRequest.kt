package `in`.allen.gsp.data.network

import retrofit2.Response

abstract class SafeApiRequest {

    suspend fun<T:Any> apiRequest(call: suspend () -> Response<T>) : T {
        val response = call.invoke()

        if(response.isSuccessful) {
            return response.body()
        } else {
            val error = response.errorBody().toString()
            val message = StringBuilder()
            error.let {
                message.append(error)
            }
            message.append("Error Code: ${response.code()}")
            throw Exception(message.toString())
        }
    }

}