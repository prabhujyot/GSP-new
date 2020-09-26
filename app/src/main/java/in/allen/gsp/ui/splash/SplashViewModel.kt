package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Encryption
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.tag
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.facebook.AccessToken
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import org.json.JSONObject


class SplashViewModel(
    private val repository: UserRepository
): ViewModel() {

//    suspend fun getDBUser(): User? {
//        return repository.getDBUser()
//    }
//
//    suspend fun setDBUser(user: User): Long {
//        return repository.setDBUser(user)
//    }
//
//    suspend fun loginUser( params: HashMap<String,String>): String? {
//        return withContext(Dispatchers.IO) { repository.login(params) }
//    }



    // interaction with activity
    val _loading = MutableLiveData<Resource.Loading<String>>()
    val _success = MutableLiveData<Resource.Success<User>>()
    val _error = MutableLiveData<Resource.Error<String>>()

    // check firebase uid
    private val auth = FirebaseAuth.getInstance()

    init {
        val currentUser = auth.currentUser
        tag("init currentUser $currentUser")
        authUser(currentUser)
    }

    fun firebaseAuthWithGoogle(idToken: String) {
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
                    _error.value = Resource.Error("${task.exception}")
                }
            }
    }

    fun firebaseAuthWithFB(token: AccessToken) {
        tag("handleFacebookAccessToken:$token")
        val credential = FacebookAuthProvider.getCredential(token.token)
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
                    _error.value = Resource.Error("${task.exception}")
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

            tag("user: $displayName : $email : $emailVerified : $photoURL : $isAnonymous : $uid : $providerData")

            viewModelScope.launch {
                _loading.value = Resource.Loading()
                try {
                    val params = HashMap<String,String>()
                    params["name"] = firebaseUser?.displayName!!
                    params["email"] = firebaseUser.email!!
                    params["avatar"] = firebaseUser.photoUrl.toString()
                    params["firebase_uid"] = firebaseUser.uid
                    val response = repository.login(params)
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
                            repository.setDBUser(user)
                            _success.value = Resource.Success(user)
                        } else {
                            _error.value = Resource.Error(responseObj.getString("message"))
                        }
                    }
                } catch (e: Exception) {
                    _error.value = e.message?.let { Resource.Error(it) }
                }
            }
        }
    }

    private fun logout() {
        auth.signOut()
    }

    private fun authUser(firebaseUser: FirebaseUser?) {
        val isSignedIn = firebaseUser != null
        tag("authUser $isSignedIn")
        // Status text
        if (isSignedIn) {
            // check if firebase uid match with db user
            viewModelScope.launch {
                val dbUser = repository.getDBUser()
                tag("authUser: dbUser $isSignedIn")
                if (dbUser != null) {
                    tag("authUser: dbUser uid ${dbUser.firebase_uid.equals(firebaseUser?.uid,true)}")
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
                                _success.value = Resource.Success(dbUser)
                            }
                        } catch (e: Exception) {
                            tag("authUser: ${e.message}")
                            _error.value = Resource.Error("${e.message}")
                        }
                    } else {
                        // logout old user and ask login
                        logout()
                        _error.value = Resource.Error("")
                    }
                } else {
                    authToServer(firebaseUser)
                }
            }
        } else {
            _error.value = Resource.Error("")
        }
    }

}