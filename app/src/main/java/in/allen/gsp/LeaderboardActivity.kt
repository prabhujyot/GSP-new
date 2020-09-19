package `in`.allen.gsp

import `in`.allen.gsp.utils.App
import `in`.allen.gsp.utils.AppPreferences
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_leaderboard.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class LeaderboardActivity : AppCompatActivity() {

    private val TAG = LeaderboardActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        getLeaderboard()
    }

    private fun getLeaderboard() {
        val list = ArrayList<HashMap<String,String>>()
        var hashMap = HashMap<String,String>()
        hashMap["rank"] = "4"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "5"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "6"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "7"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "8"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "9"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "10"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = RecyclerViewAdapter(list, this)
    }

    private class RecyclerViewAdapter(
        val items: ArrayList<HashMap<String, String>>,
        val context: Context
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent,false)
            )
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val itemViewHolder: ItemViewHolder = holder as ItemViewHolder
            val item = items[position]

            if(position == 0) {
                itemViewHolder.v.visibility = View.GONE
            } else {
                itemViewHolder.v.visibility = View.VISIBLE
            }

            itemViewHolder.rank.text = "${item["rank"]}"
            itemViewHolder.rankName.text = "${item["rankName"]}"
            itemViewHolder.rankScore.text = "${item["rankScore"]}"

            Glide.with(context)
                .load(item["rankAvatar"])
                .circleCrop()
                .into(itemViewHolder.rankAvatar)
        }

        override fun getItemCount(): Int {
            return items.size
        }

        class ItemViewHolder (view: View) : RecyclerView.ViewHolder(view) {
            val v: View = itemView.findViewById(R.id.separator)
            val rankAvatar: ImageView = itemView.findViewById(R.id.rank_avatar)
            val rank: TextView = itemView.findViewById(R.id.rank)
            val rankName: TextView = itemView.findViewById(R.id.rank_name)
            val rankScore: TextView = itemView.findViewById(R.id.rank_score)
        }

    }
}