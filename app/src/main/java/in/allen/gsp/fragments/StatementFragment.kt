package `in`.allen.gsp.fragments

import `in`.allen.gsp.R
import `in`.allen.gsp.RewardActivity
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.fragment_statement.view.*

class StatementFragment : Fragment() {

    private val TAG = StatementFragment::class.java.name

    private lateinit var parentActivity: RewardActivity
    private lateinit var rootView: View

    private var position = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            position = it.getInt("position")
        }

        parentActivity = activity as RewardActivity
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_statement, container, false)
        getLeaderboard()
        return rootView
    }

    companion object {
        @JvmStatic
        fun newInstance(position: Int) =
            StatementFragment().apply {
                arguments = Bundle().apply {
                    putInt("position", position)
                }
            }
    }


    private fun getLeaderboard() {
        val list = ArrayList<HashMap<String,String>>()
        var hashMap = HashMap<String,String>()
        hashMap["rank"] = "1"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "2"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["rank"] = "3"
        hashMap["rankAvatar"] = "https://encrypted-tbn0.gstatic.com/images?q=tbn%3AANd9GcTaqSCG-_PRudGH3PnjI5WD0NHRbqrioLFqJQ&usqp=CAU"
        hashMap["rankName"] = "Prabhujyot Singh Bamrah"
        hashMap["rankScore"] = "Score\n5550000"
        list.add(hashMap)

        hashMap = HashMap()
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

        rootView.recyclerView.layoutManager = LinearLayoutManager(parentActivity, LinearLayoutManager.VERTICAL, false)
        rootView.recyclerView.adapter = RecyclerViewAdapter(list, parentActivity)
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