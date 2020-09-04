package `in`.allen.gsp

import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import kotlinx.android.synthetic.main.activity_profile_edit.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class ProfileEditActivity : AppCompatActivity() {

    private val TAG = ProfileEditActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        toolbar.background = ResourcesCompat.getDrawable(resources, R.drawable.gradiant_blue, null)
        toolbar.background.alpha = 0
        toolbar.setContentInsetsAbsolute(0, 0)
        setSupportActionBar(toolbar)

        toolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App

        scrollView.setOnScrollChangeListener { _: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
            if(scrollY > 40) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                toolbar.background.alpha = 240
            } else {
                window.setFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                )
                toolbar.background.alpha = 0
            }
        }
    }

    fun btnActionProfileEdit(view: View) {}
}