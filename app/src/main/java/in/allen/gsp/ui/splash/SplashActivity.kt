package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.IntroActivity
import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.services.LifeService
import `in`.allen.gsp.databinding.ActivitySplashBinding
import `in`.allen.gsp.ui.home.HomeActivity
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.dynamiclinks.PendingDynamicLinkData
import com.google.firebase.messaging.FirebaseMessaging
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance


private const val GOOGLE_SIGN_IN : Int = 9001


class SplashActivity : AppCompatActivity(), DIAware {

    private lateinit var googleSignInClient: GoogleSignInClient
    private val TAG = SplashActivity::class.java.name
    private lateinit var binding: ActivitySplashBinding
    private lateinit var viewModel: SplashViewModel

//    override val di: DI by subDI(di) {}
    override val di: DI by lazy { (applicationContext as DIAware).di }

    private lateinit var app: App
    private val repository: UserRepository by instance()
    private val preferences: AppPreferences by instance()

    private lateinit var callbackManager: CallbackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_splash)
        viewModel = SplashViewModel(repository)

        var colorList = IntArray(2)
        colorList[0] = Color.rgb(5, 137, 229)
        colorList[1] = Color.rgb(52, 48, 182)
        binding.imgFB.background = drawaleGradiantColor(
            R.drawable.left_corner_radius,
            colorList
        )
        colorList = IntArray(2)
        colorList[0] = Color.rgb(232, 232, 232)
        colorList[1] = Color.rgb(236, 236, 236)
        binding.textFB.background = drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
        binding.textGG.background = drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
        colorList = IntArray(2)
        colorList[0] = Color.rgb(239, 158, 17)
        colorList[1] = Color.rgb(248, 50, 41)
        binding.imgGG.background = drawaleGradiantColor(
            R.drawable.left_corner_radius,
            colorList
        )

        printKeyHash()
        isReferred()

        app = application as App
        app.bindMusicService()

        // subscribe notification
        if(preferences.getPref("Notification")?.isEmpty() == true) {
            FirebaseMessaging.getInstance().subscribeToTopic("Notification").addOnCompleteListener {
                preferences.setPref("Notification","true")
                tag("Subscription: ${preferences.getPref("Notification")}")
            }
        }

        googleSignInClient = initGoogle()
        initFB()

        viewModel.authUser()

        observeSuccess()
        observeLoading()
        observeError()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            GOOGLE_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account?.apply {
                        viewModel.firebaseAuthWithGoogle(account.idToken!!)
                    }
                } catch (e: ApiException) {
                    viewModel.setError("Google sign in failed $e", viewModel.TAG)
                }
            }
        }

        // Pass the activity result back to the Facebook SDK
        if(::callbackManager.isInitialized) {
            callbackManager.onActivityResult(requestCode, resultCode, data)
        }
    }

    /* google login */
    private fun initGoogle(): GoogleSignInClient {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private fun actionGoogle() {
        if(::googleSignInClient.isInitialized) {
            startActivityForResult(
                googleSignInClient.signInIntent,
                GOOGLE_SIGN_IN
            )
        }
    }

    /* fb login */
    private fun initFB() {
        callbackManager = CallbackManager.Factory.create()
    }

    private fun actionFB() {
        if(::callbackManager.isInitialized) {
            LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        tag("initFB onSuccess loginResult.accessToken ${loginResult.accessToken}")
                        viewModel.fbAccessToken = loginResult.accessToken
                        tag("initFB onSuccess loginResult.accessToken ${viewModel.fbAccessToken}")
                        viewModel.fbGraphRequest(loginResult.accessToken)
                    }

                    override fun onCancel() {
                        tag("initFB onCancel")
                        viewModel.setError("FB Login cancel", viewModel.TAG)
                    }

                    override fun onError(exception: FacebookException) {
                        tag("initFB onError ${exception.message}")
                        viewModel.setError("FB Error ${exception.message}", viewModel.TAG)
                    }
                })
            LoginManager.getInstance()
                .logInWithReadPermissions(this, listOf("email", "public_profile"))
        }
    }

    private fun isReferred() {
        viewModel.firebaseToken = preferences.firebaseToken
        tag("isReferred: " + viewModel.firebaseToken)

        FirebaseDynamicLinks
            .getInstance()
            .getDynamicLink(intent)
            .addOnSuccessListener {
                OnSuccessListener<PendingDynamicLinkData> {
                    if(it != null) {
                        val deepLink = it.link
                        if(deepLink != null && deepLink.getBooleanQueryParameter("referral_id",false)) {
                            viewModel.referredById = deepLink.getQueryParameter("referral_id").toString()
                        }
                    }
                }
            }
    }


    private fun observeLoading() {
        viewModel.getLoading().observe(this) {
            tag("$TAG _loading: ${it.message}")
            binding.rootLayout.hideProgress()
            if (it.data is Boolean && it.data) {
                binding.rootLayout.showProgress()
            }
        }
    }

    private fun observeError() {
        viewModel.getError().observe(this) {
            tag("$TAG _error: ${it.message}")
            if (it != null) {
                binding.rootLayout.hideProgress()
                askToLogin()
                when (it.message) {
                    "alert" -> {
                        it.data?.let { it1 -> alertDialog("Error", it1) {} }
                    }
                    "tag" -> {
                        it.data?.let { it1 -> tag("$TAG $it1") }
                    }
                    "toast" -> {
                        it.data?.let { it1 -> toast(it1) }
                    }
                    "snackbar" -> {
                        it.data?.let { it1 -> binding.rootLayout.snackbar(it1) }
                    }
                }
            }
        }
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(this) {
            tag("$TAG _success: ${it.data}")
            if (it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        val user = it.data as User

                        Intent(applicationContext, LifeService::class.java).apply {
                            val bundle = Bundle()
                            bundle.putParcelable("user", user)
                            bundle.putLong("timestampLife", preferences.timestampLife)
                            putExtra("bundle", bundle)
                            startService(this)
                        }

                        if (!preferences.appIntro) {
                            Intent(this, IntroActivity::class.java)
                                .also { it1 ->
                                    it1.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(it1)
                                }
                            preferences.appIntro = true
                        } else {
                            Intent(this, HomeActivity::class.java)
                                .also { it1 ->
                                    it1.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(it1)
                                }
                        }
                    }
                    "checkFB" -> {
                        val accessToken = AccessToken.getCurrentAccessToken()
                        val isLoggedIn = accessToken != null && !accessToken.isExpired
                        if(isLoggedIn) {
                            viewModel.fbGraphRequest(accessToken)
                        } else {
                            viewModel.setError("", "")
                        }
                    }
                }
            }
        }
    }

    private fun askToLogin() {
        binding.tinyProgressBar.show(false)
        binding.layoutAction.show()
    }

    fun btnActionSplash(view: View) {
        when (view.id) {
            R.id.btnFB -> {
                actionFB()
            }
            R.id.btnGG -> {
                actionGoogle()
            }
        }
    }

}