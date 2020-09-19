package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.HomeActivity
import `in`.allen.gsp.R
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.hideProgress
import `in`.allen.gsp.utils.showProgress
import `in`.allen.gsp.utils.tag
import android.content.Intent
import android.view.View
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
import kotlinx.android.synthetic.main.activity_splash.*
import org.json.JSONObject

const val GOOGLE_SIGN_IN : Int = 9001

class SplashViewModel(
    private val repository: UserRepository,
    private val googleSignInClient: GoogleSignInClient
): ViewModel() {


    val startActivityForResultEvent = LiveMessageEvent<SplashListener>()


    private val TAG = SplashViewModel::class.java.name

    var splashListener: SplashListener ?= null


    // check firebase uid
    private val auth = FirebaseAuth.getInstance()
    fun getFirebaseUID() = auth.currentUser



    fun getLoggedInUser() = repository.getUser()

    fun btnActionSplash(view: View) {
        when (view.id) {
            R.id.btnFB -> {
                splashListener?.onStarted()
                Coroutines.main {
                    try {
                        val postObj = JSONObject()
                        val response = repository.login(postObj)
                        if(response.status == 1) {
                            splashListener?.onSuccess(response.data)
                            repository.saveUser(response.data)
                        } else {
                            splashListener?.onFailure(response.message)
                        }
                    } catch (e: Exception) {
                        splashListener?.onFailure("${e.message}")
                    }
                }
            }
            R.id.btnGG -> {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResultEvent.sendEvent { startActivityForResult(signInIntent, GOOGLE_SIGN_IN) }
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
            splashListener?.onFailure("$e")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        splashListener?.onProgress()
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    tag("signInWithCredential:success")
                    val user = auth.currentUser
                    auth(user)
                } else {
                    // If sign in fails, display a message to the user.
                    tag("signInWithCredential:failure ${task.exception}")
                    splashListener?.onFailure("${task.exception}")
                }
            }
    }

    private fun firebaseAuthWithFB(token: AccessToken) {
        splashListener?.onProgress()
        tag("handleFacebookAccessToken:$token")
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    tag("signInWithCredential:success")
                    val user = auth.currentUser
                    auth(user)
                } else {
                    // If sign in fails, display a message to the user.
                    tag("signInWithCredential:failure ${task.exception}")
                    splashListener?.onFailure("${task.exception}")
                }
            }
    }

    private fun auth(user: FirebaseUser?) {
        val isSignedIn = user != null
        // Status text
        if (isSignedIn) {
            val displayName = user?.displayName
            val email = user?.email;
            val emailVerified = user?.isEmailVerified;
            val photoURL = user?.photoUrl;
            val isAnonymous = user?.isAnonymous;
            val uid = user?.uid;
            val providerData = user?.providerId;

            tag("user: $displayName : $email : $emailVerified : $photoURL : $isAnonymous : $uid : $providerData"
            )

            splashListener?.onStarted()
            Coroutines.main {
                try {
                    val postObj = JSONObject()
                    postObj.put("name",displayName)
                    postObj.put("email",displayName)
                    postObj.put("emailVerified",emailVerified)
                    postObj.put("avatar",photoURL)
                    postObj.put("uid",uid)
                    postObj.put("provider",providerData)


                    val response = repository.login(postObj)
                    if(response.status == 1) {
                        splashListener?.onSuccess(response.data)
                        repository.saveUser(response.data)
                    } else {
                        splashListener?.onFailure(response.message)
                    }
                } catch (e: Exception) {
                    splashListener?.onFailure("${e.message}")
                }
            }
        }
    }
}