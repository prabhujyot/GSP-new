package `in`.allen.gsp

import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.item_topic_progress.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlin.random.Random


class ProfileActivity : AppCompatActivity() {

    private val TAG = ProfileActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        setSupportActionBar(toolbar)
        toolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App

        getStatistics()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.profile, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.menu_edit) {
            startActivity(Intent(this, ProfileEditActivity::class.java))
        }
        if(id == R.id.menu_share) {
            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = app.screenShot(statistics,"screenshot.jpg")
            share.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this,applicationContext.packageName+".provider",file))
            startActivity(Intent.createChooser(share, "Share Image"))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getStatistics() {
        var colorList = IntArray(2)
        colorList[0] = Color.rgb(112, 101, 193)
        colorList[1] = Color.rgb(0, 147, 234)
        progressLose.background = app.drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )

        val topics = ArrayList<HashMap<String, String>>()
        var hashMap = HashMap<String, String>()
        hashMap["topic"]="History"
        hashMap["percent"]="80"
        topics.add(hashMap)

        hashMap = HashMap()
        hashMap["topic"]="Political"
        hashMap["percent"]="70"
        topics.add(hashMap)

        hashMap = HashMap()
        hashMap["topic"]="Science"
        hashMap["percent"]="60"
        topics.add(hashMap)

        hashMap = HashMap()
        hashMap["topic"]="Current Affairs"
        hashMap["percent"]="55"
        topics.add(hashMap)

        hashMap = HashMap()
        hashMap["topic"]="Miscellaneous"
        hashMap["percent"]="65"
        topics.add(hashMap)

        hashMap = HashMap()
        hashMap["topic"]="Bollywood"
        hashMap["percent"]="50"
        topics.add(hashMap)

        for(el in topics) {
            val topicView: View = layoutInflater.inflate(
                R.layout.item_topic_progress,
                layoutTopics,
                false
            )
            colorList = IntArray(2)
            colorList[0] = Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
            colorList[1] = Color.rgb(Random.nextInt(255), Random.nextInt(255), Random.nextInt(255))
            topicView.progressTopic.background = app.drawaleGradiantColor(
                R.drawable.right_corner_radius,
                colorList
            )
            topicView.progressTopic.text = "${el["percent"]}%"
            topicView.topic.text = el["topic"]

            val percent = (0.7).times(el["percent"]?.toFloat()!!)

            val param = topicView.progressTopic.layoutParams
            (param as LinearLayout.LayoutParams).weight = (percent.toFloat()/100)
            topicView.progressTopic.layoutParams = param

            scaleView(topicView.progressTopic, 0f, 1f)

            layoutTopics.addView(topicView)
        }
    }

    private fun scaleView(v: View, startScale: Float, endScale: Float) {
        val anim: Animation = ScaleAnimation(
            startScale, endScale,  // Start and end values for the X axis scaling
            1f, 1f,  // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0f,  // Pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 1f
        ) // Pivot point of Y scaling
        anim.fillAfter = true // Needed to keep the result of the animation
        anim.duration = 1000
        v.startAnimation(anim)
    }

}