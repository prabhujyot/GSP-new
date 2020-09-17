package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import `in`.allen.gsp.databinding.ActivityProfileEditBinding
import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.tag
import `in`.allen.gsp.helpers.toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class ProfileEditActivity : AppCompatActivity(), ProfileListener {

    private val TAG = ProfileEditActivity::class.java.name

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityProfileEditBinding>(this, R.layout.activity_profile_edit)
        val viewModel = ProfileViewModel()
        binding.viewModel = viewModel
        viewModel.profileListener = this

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App
    }

    override fun onStarted() {
        toast("onStarted")
        tag(TAG,"onStarted")
    }

    override fun onSuccess(response: LiveData<String>) {
        response.observe(this, Observer {
            tag(TAG,"onSuccess $it")
        })
        toast("onSuccess")
    }

    override fun onFailed(message: String) {
        toast(message)
        tag(TAG,message)
    }

}