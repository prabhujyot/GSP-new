package `in`.allen.gsp

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.bumptech.glide.Glide

class HomeActivity : AppCompatActivity() {

    private val tag = HomeActivity::class.java.name
    private lateinit var viewpagerContest: ViewPager

    private val contestList = ArrayList<HashMap<String,String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        viewpagerContest = findViewById(R.id.viewpagerContest)
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

    fun btnAction(view: View) {
        val id = view.id
        if(id == R.id.btnProfileTop) {
            startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
        } else if(id == R.id.btnProfile) {
            startActivity(Intent(this@HomeActivity, ProfileActivity::class.java))
        }
    }
}