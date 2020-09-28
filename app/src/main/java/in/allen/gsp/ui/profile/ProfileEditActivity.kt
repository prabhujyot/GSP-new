package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityProfileEditBinding
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ProfileEditActivity : AppCompatActivity(), KodeinAware {

    private val TAG = ProfileEditActivity::class.java.name
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var viewModel: ProfileViewModel

    override val kodein by kodein()
    private val instance: UserRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_edit)
        viewModel = ProfileViewModel(instance)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

    }

    fun btnActionProfileEdit(view: View) {}

}