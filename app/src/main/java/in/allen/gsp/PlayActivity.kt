package `in`.allen.gsp

import `in`.allen.gsp.helpers.App
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.res.ResourcesCompat
import kotlinx.android.synthetic.main.activity_play.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class PlayActivity : AppCompatActivity() {

    private val TAG = LeaderboardActivity::class.java.name

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        val bg = ResourcesCompat.getDrawable(resources,R.drawable.bg_play_o,null)
        if (bg != null) {
            bg.alpha = 20
            bg_layout.setImageDrawable(bg)
        }

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }
    }
}