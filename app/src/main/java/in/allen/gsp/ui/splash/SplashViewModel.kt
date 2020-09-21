package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.R
import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.Encryption
import `in`.allen.gsp.utils.NoInternetExeption
import `in`.allen.gsp.utils.tag
import android.content.Intent
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.AccessToken
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import org.json.JSONObject

const val GOOGLE_SIGN_IN : Int = 9001

class SplashViewModel(
    private val repository: UserRepository,
    private val googleSignInClient: GoogleSignInClient
): ViewModel() {

    // interface to activity
    var splashListener: SplashListener ?= null
    val startActivityForResultEvent = LiveMessageEvent<SplashListener>()

    // check firebase uid
    private val auth = FirebaseAuth.getInstance()

    fun btnActionSplash(view: View) {
        when (view.id) {
            R.id.btnFB -> {
            }
            R.id.btnGG -> {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResultEvent.sendEvent {
                    startActivityForResult(
                        signInIntent,
                        GOOGLE_SIGN_IN
                    )
                }
            }
        }
    }

    //Called from Activity receving result
    fun onResultFromActivity(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            GOOGLE_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                googleSignInComplete(task)
            }
        }
    }

    private fun googleSignInComplete(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.apply {
                tag("firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            }
        } catch (e: ApiException) {
            tag("Google sign in failed $e")
            hashMap["status"] = "fail"
            hashMap["message"] = "Google sign in failed $e"
            viewModelResponse.value = hashMap
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        hashMap["status"] = "progress"
        hashMap["message"] = ""
        viewModelResponse.value = hashMap

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    tag("signInWithCredential:success")
                    val firebaseUser = auth.currentUser
                    authToServer(firebaseUser)
                } else {
                    // If sign in fails, display a message to the user.
                    tag("signInWithCredential:failure ${task.exception}")
                    hashMap["status"] = "fail"
                    hashMap["message"] = "${task.exception}"
                    viewModelResponse.value = hashMap
                }
            }
    }

    private fun firebaseAuthWithFB(token: AccessToken) {
        tag("handleFacebookAccessToken:$token")
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    tag("signInWithCredential:success")
                    val user = auth.currentUser
                    authToServer(user)
                } else {
                    // If sign in fails, display a message to the user.
                    tag("signInWithCredential:failure ${task.exception}")
                    hashMap["status"] = "fail"
                    hashMap["message"] = "${task.exception}"
                    viewModelResponse.value = hashMap
                }
            }
    }

    private fun authToServer(firebaseUser: FirebaseUser?) {
        val isSignedIn = firebaseUser != null
        // Status text
        if (isSignedIn) {
            val displayName = firebaseUser?.displayName
            val email = firebaseUser?.email
            val emailVerified = firebaseUser?.isEmailVerified
            val photoURL = firebaseUser?.photoUrl
            val isAnonymous = firebaseUser?.isAnonymous
            val uid = firebaseUser?.uid
            val providerData = firebaseUser?.providerId

            tag("user: $displayName : $email : $emailVerified : $photoURL : $isAnonymous : $uid : $providerData")

            Coroutines.main {
                try {
                    val response = repository.login(
                        displayName!!,
                        email!!,
                        photoURL.toString(),
                        uid!!
                    )
                    tag("response: $response")
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        tag("responseObj: $responseObj")
                        if(responseObj.getInt("status") == 1) {
                            val data = responseObj.getJSONObject("data")
                            val user = User(
                                0,
                                data.getInt("user_id"),
                                data.getString("name"),
                                data.getString("avatar"),
                                data.getString("email"),
                                data.getString("mobile"),
                                data.getString("referral_id"),
                                data.getString("firebase_token"),
                                data.getString("firebase_uid"),
                                data.getString("played_qid"),
                                data.getString("create_date"),
                                data.getString("session_token"),
                                data.getInt("coins"),
                                data.getBoolean("is_admin")
                            )
                            repository.saveUser(user)

                            hashMap["status"] = "success"
                            hashMap["message"] = ""
                            viewModelResponse.value = hashMap
                        } else {
                            hashMap["status"] = "fail"
                            hashMap["message"] = responseObj.getString("message")
                            viewModelResponse.value = hashMap
                        }
                    }
                } catch (e: ApiException) {
                    hashMap["status"] = "fail"
                    hashMap["message"] = "${e.message}"
                    viewModelResponse.value = hashMap
                } catch (e: NoInternetExeption) {
                    hashMap["status"] = "fail"
                    hashMap["message"] = "${e.message}"
                    viewModelResponse.value = hashMap
                }
            }
        }
    }

    fun logout() {
        auth.signOut()
    }


    private val viewModelResponse = MutableLiveData<HashMap<String,String>>()
    private val hashMap = HashMap<String,String>()

    fun updateUI(): LiveData<HashMap<String, String>> {
        // check if user is firebase user
        val firebaseUser = auth.currentUser
        val isSignedIn = firebaseUser != null
        // Status text
        if (isSignedIn) {
            // check if firebase uid match with db user
            Coroutines.main {
                val dbUser = repository.getUser()
                tag("dbUser: $dbUser")
                if (dbUser != null) {
                    if(dbUser.firebase_uid.equals(firebaseUser?.uid,true)) {
                        val encryption = Encryption()
                        try {
                            val sessionToken = encryption.decrypt(dbUser.session_token)
                            val obj = JSONObject(sessionToken.toString())
                            val sysTime = System.currentTimeMillis() / 1000L
                            val expireTime = obj.getLong("expire_on")
                            tag("dbUser: sysTime: $sysTime expireTime: $expireTime  -- ${(sysTime > expireTime)}")
                            if (sysTime > expireTime) {
                                authToServer(firebaseUser)
                            } else {
                                hashMap["status"] = "success"
                                hashMap["message"] = ""
                                viewModelResponse.value = hashMap
                            }
                        } catch (e: Exception) {
                            hashMap["status"] = "fail"
                            hashMap["message"] = "${e.message}"
                            viewModelResponse.value = hashMap
                        }
                    } else {
                        // logout old user and ask login
                        logout()

                        hashMap["status"] = "fail"
                        hashMap["message"] = ""
                        viewModelResponse.value = hashMap
                    }
                } else {
                    authToServer(firebaseUser)
                }
            }
        } else {
            hashMap["status"] = "fail"
            hashMap["message"] = ""
            viewModelResponse.value = hashMap
        }

        return viewModelResponse
    }

}