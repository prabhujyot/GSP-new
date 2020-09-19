package `in`.allen.gsp.fragments

import `in`.allen.gsp.R
import `in`.allen.gsp.VideosActivity
import `in`.allen.gsp.YTPlayerActivity
import `in`.allen.gsp.utils.services.WebServices
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class PlaylistFragment : Fragment() {
    private lateinit var playlistId: String

    private lateinit var parentActivity: VideosActivity
    private lateinit var rootView: View

    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var adapter: RecyclerViewAdapter
    private val mContentItems = ArrayList<HashMap<String, String>>()
    private var pageToken = ""
    private var nomore = false
    private var loading = true
    private var pastVisiblesItems = 0
    private var visibleItemCount = 0
    private var totalItemCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getString("playlistId").toString()
        }

        parentActivity = activity as VideosActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_statement, container, false)

        progressBar = rootView.findViewById(R.id.progressBar)
        recyclerView = rootView.findViewById(R.id.recyclerView)
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        recyclerView.layoutManager = layoutManager
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0) {
                    visibleItemCount = layoutManager.childCount
                    totalItemCount = layoutManager.itemCount
                    pastVisiblesItems = (recyclerView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
                    if (loading) {
                        if (visibleItemCount + pastVisiblesItems >= totalItemCount - 2) {
                            loading = false
                            if (!nomore) {
                                getPlaylist(playlistId)
                            }
                        }
                    }
                }
            }
        })

        adapter = RecyclerViewAdapter(mContentItems, parentActivity)
        reset()
        getPlaylist(playlistId)

        return rootView
    }

    private fun getPlaylist(playlistId: String) {
        progressBar.visibility = View.GONE
        if (mContentItems.isEmpty()) {
            progressBar.visibility = View.VISIBLE
        }
        val url = "https://www.googleapis.com/youtube/v3/playlistItems?part=snippet&maxResults=50&playlistId=$playlistId&key=AIzaSyDbpMioiHvdMHmA41UETZy6sCO1txortVc"
        parentActivity.webServices.task("getPlaylist",url,null, object: WebServices.WebServicesResponse {
            override fun onSuccess(response: JSONObject) {
                progressBar.visibility = View.GONE
                loading = true

                if(!response.has("error")) {
                    val arr = response.getJSONArray("items")
                    if(arr.length() > 0) {
                        for(i in 0..arr.length().minus(1)) {
                            val item = arr.get(i) as JSONObject
                            val hashMap = HashMap<String,String>()
                            hashMap["title"] = item.getJSONObject("snippet").getString("title")
                            hashMap["publishedAt"] = item.getJSONObject("snippet").getString("publishedAt")
                            hashMap["description"] = item.getJSONObject("snippet").getString("description")
                            hashMap["channelTitle"] = item.getJSONObject("snippet").getString("channelTitle")
                            hashMap["videoId"] = item.getJSONObject("snippet").getJSONObject("resourceId").getString("videoId")

                            var thumbnails = ""
                            if(item.getJSONObject("snippet").getJSONObject("thumbnails").has("medium")) {
                                thumbnails = item.getJSONObject("snippet").getJSONObject("thumbnails").getJSONObject("medium").getString("url")
                            }
                            hashMap["thumbnails"] = thumbnails
                            mContentItems.add(hashMap)
                        }
                    }
                    if (pageToken.isEmpty()) {
                        recyclerView.adapter = adapter
                    }

                    adapter.notifyDataSetChanged()

                    if(response.has("nextPageToken")) {
                        pageToken = response.getString("nextPageToken")
                    } else {
                        nomore = true
                    }
                } else {
                    nomore = true
                }
            }

            override fun onFailure(response: String) {
                progressBar.visibility = View.GONE
                loading = true
            }
        })
    }

    private fun reset() {
        mContentItems.clear()
        adapter.notifyDataSetChanged()
        pageToken = ""
        nomore = false
    }

    private class RecyclerViewAdapter(
            private val list: ArrayList<HashMap<String, String>>,
            val parentActivity: VideosActivity
    ) : RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {
        override fun onCreateViewHolder(viewGroup: ViewGroup, i: Int): RecyclerViewHolder {
            val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.item_playlist, viewGroup, false)
            return RecyclerViewHolder(view)
        }

        override fun onBindViewHolder(recyclerViewHolder: RecyclerViewHolder, i: Int) {
            recyclerViewHolder.channel.text = "${list[i]["channelTitle"]}"
            recyclerViewHolder.title.text = "${list[i]["title"]}"
            recyclerViewHolder.date.text = "${list[i]["publishedAt"]}"
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val cal = Calendar.getInstance()
            val date: Date?
            try {
                date = sdf.parse(list[i]["publishedAt"])
                cal.time = date
                val dat = parentActivity.app.milisToFormat(cal.timeInMillis, "dd MMM yyyy")
                recyclerViewHolder.date.text = dat
            } catch (e: ParseException) {
                e.printStackTrace()
            }
            if(list[i]["thumbnails"]!!.isNotEmpty())
                Glide.with(parentActivity)
                    .load(list[i]["thumbnails"])
                    .centerCrop()
                    .into(recyclerViewHolder.thumb)

            recyclerViewHolder.ytItem.setOnClickListener {
                val act = Intent(parentActivity, YTPlayerActivity::class.java)
                act.putExtra("videoId", list[i]["videoId"])
                parentActivity.startActivity(act)
            }
        }

        override fun getItemCount(): Int {
            return list.size
        }

        private class RecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ytItem: ConstraintLayout = itemView.findViewById(R.id.yt_item)
            val thumb: ImageView = itemView.findViewById(R.id.thumb)
            val title: TextView = itemView.findViewById(R.id.title)
            val channel: TextView = itemView.findViewById(R.id.channel)
            val date: TextView = itemView.findViewById(R.id.date)
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(playlistId: String) =
                PlaylistFragment().apply {
                    arguments = Bundle().apply {
                        putString("playlistId", playlistId)
                    }
                }
    }
}