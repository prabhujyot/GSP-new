package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.services.LifeService
import `in`.allen.gsp.utils.Encryption
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.tag
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit


class SplashViewModel(
    private val repository: UserRepository
): ViewModel() {

    val ALERT = "alert"
    val SNACKBAR = "snackbar"
    val TAG = "tag"
    val TOAST = "toast"

    // interaction with activity
    private val _loading = MutableLiveData<Resource.Loading<Any>>()
    private val _success = MutableLiveData<Resource.Success<Any>>()
    private val _error = MutableLiveData<Resource.Error<String>>()

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

    init {
        val currentUser = auth.currentUser
        authUser(currentUser)
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    authToServer(firebaseUser)
                } else {
                    setError("signInWithCredential:failure ${task.exception}", TAG)
                }
            }
    }

    fun firebaseAuthWithFB(token: AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    authToServer(firebaseUser)
                } else {
                    setError("signInWithCredential:failure ${task.exception}", TAG)
                }
            }
    }

    private fun authToServer(firebaseUser: FirebaseUser?) {
        val isSignedIn = firebaseUser != null
        tag("authToServer $isSignedIn")
        if (isSignedIn) {
            val displayName = firebaseUser?.displayName
            val email = firebaseUser?.email
            val emailVerified = firebaseUser?.isEmailVerified
            val photoURL = firebaseUser?.photoUrl
            val isAnonymous = firebaseUser?.isAnonymous
            val uid = firebaseUser?.uid
            val providerData = firebaseUser?.providerId

            viewModelScope.launch {
                setLoading(true)
                try {
                    val params = HashMap<String, String>()
                    params["name"] = firebaseUser?.displayName!!
                    params["email"] = firebaseUser.email!!
                    params["avatar"] = firebaseUser.photoUrl.toString()
                    params["firebase_uid"] = firebaseUser.uid
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
                            setError(responseObj.getString("message"), TAG)
                        }
                    }
                } catch (e: Exception) {
                    setError(e.message.toString(), TAG)
                }
            }
        }
    }

    private fun logout() {
        auth.signOut()
    }

    private fun authUser(firebaseUser: FirebaseUser?) {
        val isSignedIn = firebaseUser != null
        // Status text
        if (isSignedIn) {
            // check if firebase uid match with db user
            viewModelScope.launch {
                val dbUser = repository.getDBUser()
                if (dbUser != null) {
                    tag(
                        "authUser: dbUser uid ${
                            dbUser.firebase_uid.equals(
                                firebaseUser?.uid,
                                true
                            )
                        }"
                    )
                    if(dbUser.firebase_uid.equals(firebaseUser?.uid, true)) {
                        val encryption = Encryption()
                        try {
                            val sessionToken = encryption.decrypt(dbUser.session_token)
                            val obj = JSONObject(sessionToken.toString())
                            val sysTime = System.currentTimeMillis() / 1000L
                            val expireTime = obj.getLong("expire_on")
                            if (sysTime > expireTime) {
                                authToServer(firebaseUser)
                            } else {
                                viewModelScope.launch {
                                    delay(2 * 1000)
                                    setSuccess(dbUser, "user")
                                }
                            }
                        } catch (e: Exception) {
                            setError("authUser: ${e.message}", TAG)
                        }
                    } else {
                        // logout old user and ask login
                        logout()
                        setError("", "")
                    }
                } else {
                    authToServer(firebaseUser)
                }
            }
        } else {
            setError("", "")
        }
    }
}