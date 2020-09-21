package `in`.allen.gsp.ui.splash

import `in`.allen.gsp.HomeActivity
import `in`.allen.gsp.R
import `in`.allen.gsp.databinding.ActivitySplashBinding
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.facebook.CallbackManager
import kotlinx.android.synthetic.main.activity_splash.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class SplashActivity : AppCompatActivity(), SplashListener, KodeinAware {

    override val kodein by kodein()
    private val factory: SplashViewModelFactory by instance()

    private lateinit var viewModel: SplashViewModel

    private lateinit var app: App


    // facebook
    private lateinit var callbackManager: CallbackManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = DataBindingUtil.setContentView<ActivitySplashBinding>(this, R.layout.activity_splash)
        viewModel = ViewModelProvider(this, factory).get(SplashViewModel::class.java)
        binding.viewModel = viewModel
        viewModel.splashListener = this

        //this sets the LifeCycler owner and receiver
        viewModel.startActivityForResultEvent.setEventReceiver(this, this)

        app = application as App

        var colorList = IntArray(2)
        colorList[0] = Color.rgb(5, 137, 229)
        colorList[1] = Color.rgb(52, 48, 182)
        imgFB.background = app.drawaleGradiantColor(
            R.drawable.left_corner_radius,
            colorList
        )
        colorList = IntArray(2)
        colorList[0] = Color.rgb(232, 232, 232)
        colorList[1] = Color.rgb(236, 236, 236)
        textFB.background = app.drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
        textGG.background = app.drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
        colorList = IntArray(2)
        colorList[0] = Color.rgb(239, 158, 17)
        colorList[1] = Color.rgb(248, 50, 41)
        imgGG.background = app.drawaleGradiantColor(
            R.drawable.left_corner_radius,
            colorList
        )

        // check firebase uid
        viewModel.updateUI().observe(this, Observer { response ->
            tag(response)
            if(response["status"].equals("progress",true)) {
                root_layout.showProgress()
            } else if(response["status"].equals("fail",true)) {
                askToLogin(true)
                root_layout.hideProgress()
                response["message"]?.let { it1 ->
                    if(it1.isNotEmpty())
                        root_layout.snackbar(it1)
                }
            } else if(response["status"].equals("success",true)) {
                root_layout.hideProgress()
                Intent(this, HomeActivity::class.java)
                    .also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(it)
                    }
            }
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        viewModel.onResultFromActivity(requestCode,resultCode,data)
        super.onActivityResult(requestCode, resultCode, data)
    }


//    private fun initFB() {
//        callbackManager = CallbackManager.Factory.create()
//
//        btn_fb.setOnClickListener {
//            tag(TAG, "$callbackManager : ${LoginManager.getInstance()}")
//            LoginManager.getInstance().registerCallback(callbackManager,
//                object : FacebookCallback<LoginResult> {
//                    override fun onSuccess(loginResult: LoginResult) {
//                        tag(TAG, "FB Login Success")
//                        firebaseAuthWithFB(loginResult.accessToken)
//                    }
//
//                    override fun onCancel() {
//                        tag(TAG,"FB Login cancel")
//                    }
//
//                    override fun onError(exception: FacebookException) {
//                        tag(TAG, "FB Error ${exception.message}")
//                    }
//                })
//            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
//        }
//    }



    private fun askToLogin(visible: Boolean) {
        if(visible) {
            tinyProgressBar.visibility = View.GONE
            layoutAction.visibility = View.VISIBLE
        } else {
            tinyProgressBar.visibility = View.VISIBLE
            layoutAction.visibility = View.GONE
        }
    }

}