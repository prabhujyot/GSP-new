package `in`.allen.gsp

import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class ProfileEditActivity : AppCompatActivity() {

    private val TAG = ProfileEditActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App

    }

    fun btnActionProfileEdit(view: View) {}
}