package `in`.allen.gsp

import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class RewardActivity : AppCompatActivity() {

    private val TAG = RewardActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward)

        toolbar.background = ResourcesCompat.getDrawable(resources, R.drawable.gradiant_orange, null)
        toolbar.background.alpha = 0
        toolbar.setContentInsetsAbsolute(0, 0)
        setSupportActionBar(toolbar)

        toolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App

//        scrollView.setOnScrollChangeListener { _: NestedScrollView?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int ->
//            if(scrollY > 40) {
//                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
//                toolbar.background.alpha = 240
//            } else {
//                window.setFlags(
//                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
//                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
//                )
//                toolbar.background.alpha = 0
//            }
//        }
    }

    fun btnActionReward(view: View) {}

}