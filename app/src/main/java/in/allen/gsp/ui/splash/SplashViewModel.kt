package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.*
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.google.firebase.auth.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject


class SplashViewModel(
    private val repository: UserRepository
): ViewModel() {

    lateinit var fbAccessToken: AccessToken
    private var currentUser: FirebaseUser? = null
    val ALERT = "alert"
    val SNACKBAR = "snackbar"
    val TAG = "tag"
    val TOAST = "toast"

    // interaction with activity
    private val _loading = MutableLiveData<Resource.Loading<Any>>()
    private val _success = MutableLiveData<Resource.Success<Any>>()
    private val _error = MutableLiveData<Resource.Error<String>>()

    private var provider = ""

    fun getLoading(): LiveData<Resource.Loading<Any>> {
        return _loading
    }

    fun getError(): LiveData<Resource.Error<String>> {
        return _error
    }

    fun getSuccess(): LiveData<Resource.Success<Any>> {
        return _success
    }

    fun setLoading(loading: Boolean) {
        _loading.value = Resource.Loading(loading)
    }

    fun setError(data: String, filter: String) {
        _error.postValue(Resource.Error(filter, data))
    }

    fun setSuccess(data: Any, filter: String) {
        _success.value = Resource.Success(data, filter)
    }

    // check firebase uid
    private val auth = FirebaseAuth.getInstance()
    var referredById = ""
    var firebaseToken = ""


    fun firebaseAuthWithGoogle(idToken: String) {
        tag("firebaseAuthWithGoogle")
        provider = "google"
        setLoading(true)
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        signInFirebaseAccount(credential)
    }

//    fun firebaseAuthWithFB(token: AccessToken) {
//        provider = "facebook"
//        tag("firebaseAuthWithFB: $token authUser: $currentUser")
//        setLoading(true)
//        val credential = FacebookAuthProvider.getCredential(token.token)
//        signInFirebaseAccount(credential)
//    }

    fun fbGraphRequest(token: AccessToken) {
        val graphRequest = GraphRequest.newMeRequest(
            token
        ) { _, response ->
            tag("FB GraphResponse $response")
            if(response != null) {
                val obj = response.jsonObject
                val params = HashMap<String, String>()
                params["name"] = obj.getString("name")
                if(obj.has("email")) {
                    params["email"] = obj.getString("email")
                } else {
                    params["email"] = obj.getString("id")
                }
                params["avatar"] =
                    obj.getJSONObject("picture").getJSONObject("data").getString("url")
                params["firebase_token"] = firebaseToken
                params["referred_by_id"] = referredById
                setLoading(true)
                authToServer(params)
            } else {
                setError("", "")
            }
        }

        val parameters = Bundle()
        parameters.putString(
            "fields",
            "id, email, first_name, last_name, name, picture, gender, birthday, location"
        )
        graphRequest.parameters = parameters
        graphRequest.executeAsync()
    }

    private fun signInFirebaseAccount(credential: AuthCredential) {
        tag("signInWithCredential")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    tag("firebaseAuthWithFB success: ${auth.currentUser}")
                    val firebaseUser = auth.currentUser
                    val params = HashMap<String, String>()
                    params["name"] = firebaseUser?.displayName!!
                    params["email"] = firebaseUser.email!!
                    params["avatar"] = firebaseUser.photoUrl.toString()
                    params["firebase_uid"] = firebaseUser.uid
                    params["firebase_token"] = firebaseToken
                    params["referred_by_id"] = referredById
                    authToServer(params)
                } else {
                    tag("signInWithCredential Exception: " + task.exception)
                    if(task.exception is FirebaseAuthUserCollisionException
                        && provider.equals("facebook",true)) {
                        fbGraphRequest(fbAccessToken)
                    } else {
                        setError("signInWithCredential:failure ${task.exception}", SNACKBAR)
                    }
                }
            }
    }

    private fun authToServer(params: HashMap<String, String>) {
        tag("authToServer $params")
        viewModelScope.launch {
            try {
                val response = repository.login(params)
                if (response != null) {
                    val responseObj = JSONObject(response)
                    if(responseObj.getInt("status") == 1) {
                        val data = responseObj.getJSONObject("data")
                        val user = User(
                            0,
                            data.getInt("user_id"),
                            data.getString("name"),
                            data.getString("avatar"),
                            data.getString("email"),
                            data.getString("mobile"),
                            data.getString("about"),
                            data.getString("location"),
                            data.getString("referral_id"),
                            data.getString("firebase_token"),
                            data.getString("firebase_uid"),
                            data.getString("played_qid"),
                            data.getInt("high_score"),
                            data.getInt("xp"),
                            data.getString("create_date"),
                            data.getString("session_token"),
                            data.getInt("coins"),
                            data.getInt("redeemed_otp_status"),
                            data.getBoolean("is_admin"),
                            data.getString("config_data")
                        )
                        repository.setDBUser(user)
                        setSuccess(user, "user")
                    } else {
                        setError(responseObj.getString("message"), SNACKBAR)
                    }
                }
            } catch (e: ApiException) {
                setError(e.message.toString(), SNACKBAR)
            } catch (e: NoInternetExeption) {
                setError(e.message.toString(), SNACKBAR)
            }
        }
    }

    private fun logout() {
        auth.signOut()
    }

    fun authUser() {
        currentUser = auth.currentUser
        tag("authUser: $currentUser")


        val isSignedIn = currentUser != null
        // Status text
        if (isSignedIn) {
            val params = HashMap<String, String>()
            params["name"] = currentUser?.displayName!!
            params["email"] = currentUser!!.email!!
            params["avatar"] = currentUser!!.photoUrl.toString()
            params["firebase_uid"] = currentUser!!.uid
            params["firebase_token"] = firebaseToken
            params["referred_by_id"] = referredById

            // check if firebase uid match with db user
            viewModelScope.launch {
                val dbUser = repository.getDBUser()
                if (dbUser != null) {
                    tag(
                        "authUser: dbUser uid ${
                            dbUser.firebase_uid.equals(
                                currentUser!!.uid,
                                true
                            )
                        }"
                    )
                    if(dbUser.firebase_uid.equals(currentUser!!.uid, true)) {
                        val encryption = Encryption()
                        try {
                            val sessionToken = encryption.decrypt(dbUser.session_token)
                            val obj = JSONObject(sessionToken.toString())
                            val sysTime = System.currentTimeMillis() / 1000L
                            val expireTime = obj.getLong("expire_on")
                            if (sysTime > expireTime) {
                                authToServer(params)
                            } else {
                                viewModelScope.launch {
                                    delay(2 * 1000)
                                    setSuccess(dbUser, "user")
                                }
                            }
                        } catch (e: Exception) {
                            setError("authUser: ${e.message}", SNACKBAR)
                        }
                    } else {
                        // logout old user and ask login
                        logout()
                        setError("", "")
                    }
                } else {
                    authToServer(params)
                }
            }
        } else {
            setSuccess("", "checkFB")
        }
    }

}