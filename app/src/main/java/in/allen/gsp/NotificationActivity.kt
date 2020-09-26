package `in`.allen.gsp

import `in`.allen.gsp.utils.App
import `in`.allen.gsp.utils.timeInAgo
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_notification.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class NotificationActivity : AppCompatActivity() {

    private val TAG = NotificationActivity::class.java.name

    private lateinit var app: App

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App

        getNotifications()
    }

    private fun getNotifications() {
        val list = ArrayList<HashMap<String,String>>()
        var hashMap = HashMap<String,String>()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 14:20:00"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 16:30:00"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 10:00:00"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 10:00:00"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 13:00:00"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 10:00:00"
        list.add(hashMap)

        hashMap = HashMap()
        hashMap["title"] = "Testing notifcation"
        hashMap["msg"] = "Testing notifcation message Testing notifcation message Testing notifcation message Testing notifcation message"
        hashMap["time"] = "2020-09-14 10:00:00"
        list.add(hashMap)

        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = RecyclerViewAdapter(list,this, app)
    }

    private class RecyclerViewAdapter(
        val items: ArrayList<HashMap<String, String>>,
        val context: Context,
        val app: App
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return ItemViewHolder(
                LayoutInflater.from(context).inflate(R.layout.item_notification, parent,false)
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

            itemViewHolder.title.text = "${item["title"]}"
            itemViewHolder.msg.text = "${item["msg"]}"
            itemViewHolder.time.text = timeInAgo(item["time"],"yyyy-MM-dd hh:mm:ss")
        }

        override fun getItemCount(): Int {
            return items.size
        }

        class ItemViewHolder (view: View) : RecyclerView.ViewHolder(view) {
            val v: View = itemView.findViewById(R.id.separator)
            val title: TextView = itemView.findViewById(R.id.title)
            val msg: TextView = itemView.findViewById(R.id.msg)
            val time: TextView = itemView.findViewById(R.id.time)
        }

    }
}