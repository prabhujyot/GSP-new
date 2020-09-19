package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.databinding.ActivityProfileEditBinding
import `in`.allen.gsp.utils.App
import `in`.allen.gsp.utils.tag
import `in`.allen.gsp.utils.toast
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
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
        tag("onStarted")
    }

    override fun onSuccess(user: User) {
        toast("onSuccess ${user.name}")
    }

    override fun onFailed(message: String) {
        toast(message)
        tag(message)
    }

}