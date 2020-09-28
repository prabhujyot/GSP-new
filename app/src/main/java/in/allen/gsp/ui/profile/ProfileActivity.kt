package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityProfileBinding
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.android.synthetic.main.item_topic_progress.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import kotlin.random.Random


class ProfileActivity : AppCompatActivity(), KodeinAware {

    private val TAG = ProfileActivity::class.java.name
    private lateinit var binding: ActivityProfileBinding
    private lateinit var viewModel: ProfileViewModel

    override val kodein by kodein()
    private val instance: UserRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        binding.lifecycleOwner = this
        viewModel = ProfileViewModel(instance)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }


        viewModel._user.observe(this, {
            tag("viewModel._user: ${it.data}")
            if (it.data != null) {
                if (it.data.avatar.isNotBlank())
                    binding.avatar.loadImage(it.data.avatar, true)
                binding.username.text = it.data.name
                binding.email.text = it.data.email
                if (it.data.mobile.length > 9)
                    binding.mobile.text = it.data.mobile
                binding.totalCoins.text = "${it.data.coins}"
            }
        })

        viewModel._stats.observe(this, {
            binding.rootLayout.hideProgress()
            tag("viewModel._stats: ${it.data}")
            if (it.data != null) {
                binding.totalGames.text = it.data.getString("total")
                setStatistics(it.data)
            }
        })

        viewModel._loading.observe(this, {
            tag("viewModel._loading: ${it.message}")
            binding.rootLayout.showProgress()
        })

        viewModel._error.observe(this, {
            tag("viewModel._error: ${it.message}")
            binding.rootLayout.hideProgress()
            it.message?.let { it1 ->
                if (it1.isNotBlank())
                    binding.rootLayout.snackbar(it1.trim())
            }
        })

        viewModel.statsData()
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
            val file = screenShot(statistics, "screenshot.jpg")
            share.putExtra(
                Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    this,
                    applicationContext.packageName + ".provider",
                    file
                )
            )
            startActivity(Intent.createChooser(share, "Share Image"))
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setStatistics(data: JSONObject) {
        // won graph
        val param1 = binding.layoutWin.layoutParams
        (param1 as LinearLayout.LayoutParams).weight = data.getDouble("won").toFloat()
        binding.layoutWin.layoutParams = param1
        binding.percentWin.text = "${data.getDouble("won")} %"

        // played graph
        val param2 = binding.layoutPlayed.layoutParams
        (param2 as LinearLayout.LayoutParams).weight = data.getDouble("played").toFloat()
        binding.layoutPlayed.layoutParams = param2
        binding.percentPlayed.text = "${data.getDouble("played")} %"

        var colorList = IntArray(2)
        colorList[0] = Color.rgb(253, 195, 0)
        colorList[1] = Color.rgb(112, 101, 193)
        binding.progressPlayed.background = drawaleGradiantColor(
            R.drawable.gradiant,
            colorList
        )

        // lose graph
        val param3 = binding.layoutLose.layoutParams
        (param3 as LinearLayout.LayoutParams).weight = data.getDouble("lose").toFloat()
        binding.layoutLose.layoutParams = param3
        binding.percentLose.text = "${data.getDouble("lose")} %"

        colorList = IntArray(2)
        colorList[0] = Color.rgb(112, 101, 193)
        colorList[1] = Color.rgb(0, 147, 234)
        binding.progressLose.background = drawaleGradiantColor(
            R.drawable.right_corner_radius,
            colorList
        )

        val topics = ArrayList<HashMap<String, String>>()
        if(data.has("stats")) {
            val stats = data.getJSONObject("stats")

            val iter: Iterator<*> = stats.keys()
            while (iter.hasNext()) {
                val key = iter.next() as String
                val value: String = stats.getString(key)
                val hashMap = HashMap<String, String>()
                hashMap["topic"] = key
                hashMap["percent"] = value
                topics.add(hashMap)
            }

            for(el in topics) {
                val topicView: View = layoutInflater.inflate(
                    R.layout.item_topic_progress,
                    layoutTopics,
                    false
                )
                colorList = IntArray(2)
                colorList[0] = Color.rgb(
                    Random.nextInt(255), Random.nextInt(255), Random.nextInt(
                        255
                    )
                )
                colorList[1] = Color.rgb(
                    Random.nextInt(255), Random.nextInt(255), Random.nextInt(
                        255
                    )
                )
                topicView.progressTopic.background = drawaleGradiantColor(
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