package `in`.allen.gsp.data.repositories

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.helpers.services.WebServices
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.json.JSONObject

class ProfileRepository {

    private val TAG = ProfileRepository::class.java.name

    fun updateProfile(postObj: JSONObject?): LiveData<String> {
        val res = MutableLiveData<String>()
        WebServices().task(
            "updateProfile",
            BuildConfig.BASE_URL + "appdata",
            postObj,
            object: WebServices.WebServicesResponse{
                override fun onSuccess(response: JSONObject) {
                    res.value = response.toString()
                }

                override fun onFailure(response: String) {
                    res.value = response
                }
            }
        )
        return res
    }
}