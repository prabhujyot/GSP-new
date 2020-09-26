package `in`.allen.gsp

import `in`.allen.gsp.ui.leaderboard.LeaderboardActivity
import `in`.allen.gsp.utils.App
import `in`.allen.gsp.utils.drawaleGradiantColor
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_play.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.bottomsheet_attachment.*
import kotlinx.android.synthetic.main.bottomsheet_category.*
import kotlinx.android.synthetic.main.bottomsheet_finish.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class PlayActivity : AppCompatActivity() {

    private val TAG = LeaderboardActivity::class.java.name

    private lateinit var app: App

    private lateinit var categorySheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var attachmentSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var finishSheetBehavior: BottomSheetBehavior<FrameLayout>

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

        categorySheetBehavior = BottomSheetBehavior.from(bottomSheetCategory)
        attachmentSheetBehavior = BottomSheetBehavior.from(bottomSheetAttachment)
        finishSheetBehavior = BottomSheetBehavior.from(bottomSheetFinish)
        categorySheetBehavior.isDraggable = false
        attachmentSheetBehavior.isDraggable = false
        finishSheetBehavior.isDraggable = false
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.play, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.menu_close) {
            finish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun initPointsTable() {
        var colorList = IntArray(2)
        colorList[0] = Color.rgb(112, 101, 193)
        colorList[1] = Color.rgb(0, 147, 234)
        progressLose.background = drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )
    }

    fun btnActionPlay(view: View) {
        when (view.id) {
            R.id.btnDoubleDip -> {
                if(categorySheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    categorySheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    categorySheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            R.id.btnFiftyFifty -> {
                if(attachmentSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    attachmentSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    attachmentSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            R.id.btnFlip -> {
                if(finishSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    finishSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                } else {
                    finishSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            R.id.btnQuit -> {

            }
        }
    }
}