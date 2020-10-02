package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.NotificationActivity
import `in`.allen.gsp.R
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivitySplashBinding
import `in`.allen.gsp.ui.home.HomeActivity
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


private const val GOOGLE_SIGN_IN : Int = 9001

class SplashActivity : AppCompatActivity(), KodeinAware {

    private val TAG = SplashActivity::class.java.name
    private lateinit var binding: ActivitySplashBinding
    private lateinit var viewModel: SplashViewModel

    override val kodein by kodein()
    private val repository: UserRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        observeSuccess()
        observeLoading()
        observeError()

        catchNotification()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            GOOGLE_SIGN_IN -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    account?.apply {
                        tag("$TAG firebaseAuthWithGoogle:" + account.id)
                        viewModel.firebaseAuthWithGoogle(account.idToken!!)
                    }
                } catch (e: ApiException) {
                    tag("$TAG Google sign in failed $e")
                    viewModel.setError("Google sign in failed $e")
                }
            }
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

    /* fb login */
    private fun initFB() {
        val callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(loginResult: LoginResult) {
                    tag("$TAG FB Login Success")
                    viewModel.firebaseAuthWithFB(loginResult.accessToken)
                }

                override fun onCancel() {
                    viewModel.setError("FB Login cancel")
                }

                override fun onError(exception: FacebookException) {
                    tag("$TAG FB Error ${exception.message}")
                    viewModel.setError("FB Error ${exception.message}")
                }
            })
        LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
    }


    private fun observeLoading() {
        viewModel.stateLoading().observe(this, {
            tag("$TAG viewModel._loading: ${it.message}")
            binding.rootLayout.showProgress()
        })
    }

    private fun observeError() {
        viewModel.stateError().observe(this, {
            tag("$TAG viewModel._error: ${it.message}")
            binding.rootLayout.hideProgress()
            askToLogin()

            it.message?.let { it1 ->
                if (it1.isNotBlank())
                    binding.rootLayout.snackbar(it1.trim())
            }
        })
    }

    private fun observeSuccess() {
        viewModel.stateSuccess().observe(this, {
            tag("$TAG viewModel._success: ${it.data}")
            binding.rootLayout.hideProgress()
            if (it.data != null) {
                Intent(this, HomeActivity::class.java)
                    .also { it1 ->
                        it1.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(it1)
                    }
            }
        })
    }

    private fun askToLogin() {
        binding.tinyProgressBar.show(false)
        binding.layoutAction.show()
    }

    fun btnActionSplash(view: View) {
        when (view.id) {
            R.id.btnFB -> {
                initFB()
            }
            R.id.btnGG -> {
                val googleSignInClient = initGoogle()
                startActivityForResult(
                    googleSignInClient.signInIntent,
                    GOOGLE_SIGN_IN
                )
            }
        }
    }

    private fun catchNotification() {
        if (intent.hasExtra("click_action")
            && (intent.getStringExtra("click_action")
                    == "ACTION_NOTIFICATION")) {
            Intent(this, NotificationActivity::class.java).also {
                it.putExtra("title", intent.getStringExtra("title"))
                it.putExtra("body", intent.getStringExtra("body"))
                it.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(it)
            }
        }
    }

}