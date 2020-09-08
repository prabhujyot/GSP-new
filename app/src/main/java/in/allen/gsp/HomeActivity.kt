package `in`.allen.gsp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.PagerAdapter
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity() {

    private val TAG = HomeActivity::class.java.name

    private val contestList = ArrayList<HashMap<String,String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewpagerContest.pageMargin = 16
        viewpagerContest.adapter = ContestAdapter(this,layoutInflater,contestList)

        getContestList()
    }

    private fun getContestList() {
        contestList.clear()
        var hashMap = HashMap<String,String>()
        hashMap["image"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["title"] = "Rakshabandhan contest"
        contestList.add(hashMap)
        hashMap = HashMap()
        hashMap["image"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["title"] = "Diwali contest"
        contestList.add(hashMap)
        hashMap = HashMap()
        hashMap["image"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["title"] = "Christmas contest"
        contestList.add(hashMap)

        viewpagerContest.adapter?.notifyDataSetChanged()
    }

    private class ContestAdapter(
        private val context: Context,
        private val inflater: LayoutInflater,
        private val list: ArrayList<HashMap<String,String>>
    ): PagerAdapter() {

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val v: View = inflater.inflate(R.layout.item_contest, null)

            val image = v.findViewById<ImageView>(R.id.image)
            val title = v.findViewById<TextView>(R.id.title)
            val btnGo = v.findViewById<ImageButton>(R.id.btnGo)

            val item = list[position]
            Glide.with(context)
                .load(item["image"])
                .centerCrop()
                .into(image)
            title.text = item["title"]
//            btnGo.setOnClickListener {  }

            container.addView(v)
            return v
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }

    fun btnActionHome(view: View) {
        when (view.id) {
            R.id.btnVideos -> {
                startActivity(Intent(this@HomeActivity, VideosActivity::class.java))
            }
            R.id.btnLeaderboard -> {
                startActivity(Intent(this@HomeActivity, LeaderboardActivity::class.java))
            }
            R.id.btnProfileTop -> {
                startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
            }
            R.id.btnPlay -> {
                startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
            }
            R.id.btnProfile -> {
                startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
            }
            R.id.btnCoins -> {
                startActivity(Intent(this@HomeActivity, RewardActivity::class.java))
            }
            R.id.btnNotification -> {
                startActivity(Intent(this@HomeActivity, RewardActivity::class.java))
            }
            R.id.btnContests -> {
                startActivity(Intent(this@HomeActivity, RewardActivity::class.java))
            }
            R.id.btnSetting -> {
                startActivity(Intent(this@HomeActivity, RewardActivity::class.java))
            }
        }
    }
}