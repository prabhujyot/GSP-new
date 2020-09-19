package `in`.allen.gsp.utils.services

import android.util.Log
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.common.Priority
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import org.json.JSONObject
import java.io.File

class WebServices {

    private val TAG = WebServices::class.java.name

    // define interface
    interface WebServicesResponse {
        fun onSuccess(response: JSONObject)
        fun onFailure(response: String)
    }

    fun task(
        REQUEST_TAG: String,
        url: String,
        postObj: JSONObject?,
        webServicesResponse: WebServicesResponse
    ) {
        Log.d(REQUEST_TAG, "$url $postObj")
        if (postObj != null) {
            AndroidNetworking.post(url)
                .addJSONObjectBody(postObj) // posting json
                .setTag(REQUEST_TAG)
                .setPriority(Priority.MEDIUM)
                .doNotCacheResponse()
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        Log.d(REQUEST_TAG, response.toString())
                        webServicesResponse.onSuccess(response)
                    }

                    override fun onError(anError: ANError) {
                        webServicesResponse.onFailure(anError.message!!)
                    }
                })
        } else {
            AndroidNetworking.get(url)
                //                    .addPathParameter("pageNumber", "0")
                //                    .addQueryParameter("limit", "3")
                //                    .addHeaders("token", "1234")
                .setTag(REQUEST_TAG)
                .setPriority(Priority.MEDIUM)
                .doNotCacheResponse()
                .build()
                .getAsJSONObject(object : JSONObjectRequestListener {
                    override fun onResponse(response: JSONObject) {
                        Log.d(REQUEST_TAG, response.toString())
                        webServicesResponse.onSuccess(response)
                    }

                    override fun onError(anError: ANError) {
                        webServicesResponse.onFailure(anError.message!!)
                    }
                })
        }
    }

    fun cancel(REQUEST_TAG: String, forceful: Boolean) {
        if (forceful) {
            AndroidNetworking.forceCancel(REQUEST_TAG)
        } else {
            AndroidNetworking.cancel(REQUEST_TAG)
        }
    }

    fun upload(url: String?, file: File?) {
        AndroidNetworking.upload(url)
            .addMultipartFile("file", file)
            //                .addMultipartParameter("key","value")
            .setPriority(Priority.HIGH)
            .build()
            .setUploadProgressListener { bytesUploaded, totalBytes ->
                Log.d(TAG, "bytesUploaded: $bytesUploaded, totalBytes: $totalBytes")
            }
            .getAsJSONObject(object : JSONObjectRequestListener {
                override fun onResponse(response: JSONObject) {}

                override fun onError(error: ANError) {}
            })
    }

}