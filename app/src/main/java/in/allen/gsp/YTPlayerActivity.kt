package `in`.allen.gsp

import `in`.allen.gsp.utils.App
import `in`.allen.gsp.utils.services.WebServices
import `in`.allen.gsp.utils.toast
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayerView
import org.json.JSONObject
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class YTPlayerActivity : YouTubeBaseActivity(), YouTubePlayer.OnInitializedListener {

    private val TAG = YTPlayerActivity::class.java.name

    private lateinit var app: App
    private lateinit var webServices: WebServices

    private var youtube_view: YouTubePlayerView? = null
    private var videoId = ""
    private val RECOVERY_REQUEST = 1
    private lateinit var title: TextView
    private lateinit var views: TextView
    private lateinit var likes: TextView
    private lateinit var description: TextView

//    private lateinit var comment: EditText
//    private lateinit var btnCancel: Button
//    private lateinit var btnComment: Button
//    private lateinit var commentActionLayout: LinearLayout

    private lateinit var loadMore: Button
    private lateinit var commentLayout: LinearLayout
    private var pageToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yt_player)

        this.setFinishOnTouchOutside(false)

        app = application as App
        webServices = WebServices()

        youtube_view = findViewById(R.id.youtube_view)
        youtube_view?.initialize(getString(R.string.yt_api_key), this)

        title = findViewById(R.id.title)
        views = findViewById(R.id.views)
        likes = findViewById(R.id.likes)
        description = findViewById(R.id.description)

//        comment = findViewById(R.id.comment)
//        commentActionLayout = findViewById(R.id.commentActionLayout)
//        btnCancel = findViewById(R.id.btnCancel)
//        btnComment = findViewById(R.id.btnComment)
//        comment.setOnFocusChangeListener { p0, _ ->
//            if (p0?.hasFocus()!!) {
//                commentActionLayout.visibility = View.VISIBLE
//            }
//        }
//        comment.setOnClickListener {
//            if(it.hasFocus())
//            commentActionLayout.visibility = View.VISIBLE
//        }
//        btnCancel.setOnClickListener {
//            commentActionLayout.visibility = View.GONE
//        }

        commentLayout = findViewById(R.id.commentLayout)
        loadMore = findViewById(R.id.loadMore)

        if(intent.getStringExtra("videoId") != null) {
            videoId = intent.getStringExtra("videoId")!!
            reset()
            getVideoData()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RECOVERY_REQUEST) {
            // Retry initialization if user performed a recovery action
            youtube_view?.initialize(getString(R.string.yt_api_key), this)
        }
    }

    override fun onInitializationSuccess(p0: YouTubePlayer.Provider?, p1: YouTubePlayer?, p2: Boolean) {
        Log.d(TAG, "onInitializationSuccess: $p2")
        if(!p2) {
            p1?.cueVideo(videoId)
        }
    }

    override fun onInitializationFailure(p0: YouTubePlayer.Provider?, p1: YouTubeInitializationResult?) {
        Log.d(TAG, "onInitializationFailure: $p1")
        if(p1?.isUserRecoverableError!!) {
            p1.getErrorDialog(this, RECOVERY_REQUEST).show()
        } else {
            val error = String.format("Error initializing YouTube player: %s", p1.toString())
            toast(error)
        }
    }

    private fun reset() {
        commentLayout.removeAllViews()
        pageToken = ""
    }

    private fun getVideoData() {
        val url = "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&id=$videoId&key=" + getString(R.string.yt_api_key)
        webServices.task("getVideoData",url,null, object: WebServices.WebServicesResponse {
            override fun onSuccess(response: JSONObject) {
                if(!response.has("error")) {
                    val arr = response.getJSONArray("items")
                    if(arr.length() > 0) {
                        val item = arr.get(0) as JSONObject
                        title.text = item.getJSONObject("snippet").getString("title")
                        views.text = "${item.getJSONObject("statistics").getString("viewCount")} . views"
                        likes.text = "Likes: ${item.getJSONObject("statistics").getString("favoriteCount")}"

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            description.text = Html.fromHtml(item.getJSONObject("snippet").getString("description"), Html.FROM_HTML_MODE_COMPACT)
                        } else {
                            description.text = Html.fromHtml(item.getJSONObject("snippet").getString("description"))
                        }
                    }
                    getComments()
                } else {
                    toast(response.getJSONObject("error").getString("message"))
                }
            }

            override fun onFailure(response: String) {}
        })
    }

    private fun getComments() {
        val url = "https://www.googleapis.com/youtube/v3/commentThreads?part=snippet&maxResults=50&videoId=$videoId&pageToken=$pageToken&key=" + getString(R.string.yt_api_key)
        webServices.task("getComments",url,null, object: WebServices.WebServicesResponse {
            override fun onSuccess(response: JSONObject) {
                if(!response.has("error")) {
                    val arr = response.getJSONArray("items")
                    if(arr.length() > 0) {
                        for(i in 0..arr.length().minus(1)) {
                            val item = arr.get(i) as JSONObject
                            addCommentView(item)
                        }
                    }

                    if(response.has("nextPageToken")) {
                        pageToken = response.getString("nextPageToken")
                        loadMore.isEnabled = true
                        loadMore.visibility =View.VISIBLE
                    } else {
                        loadMore.visibility =View.GONE
                        pageToken = ""
                    }
                } else {
                    toast(response.getJSONObject("error").getString("message"))
                }
            }

            override fun onFailure(response: String) {}
        })
    }

    private fun addCommentView(item: JSONObject) {
        val v_thumb = item.getJSONObject("snippet")
                .getJSONObject("topLevelComment")
                .getJSONObject("snippet").getString("authorProfileImageUrl")

        val v_name = item.getJSONObject("snippet")
                .getJSONObject("topLevelComment")
                .getJSONObject("snippet").getString("authorDisplayName")

        val v_comment = item.getJSONObject("snippet")
                .getJSONObject("topLevelComment")
                .getJSONObject("snippet").getString("textDisplay")

        val v_date = item.getJSONObject("snippet")
                .getJSONObject("topLevelComment")
                .getJSONObject("snippet").getString("publishedAt")

        val v_likes = item.getJSONObject("snippet")
                .getJSONObject("topLevelComment")
                .getJSONObject("snippet").getString("likeCount")

        val v_reply_count = item.getJSONObject("snippet")
                .getString("totalReplyCount")

        val itemView = LayoutInflater.from(this).inflate(R.layout.item_comment, commentLayout, false)
        val thumb: ImageView = itemView.findViewById(R.id.thumb)
        val name: TextView = itemView.findViewById(R.id.name)
        val date: TextView = itemView.findViewById(R.id.date)
        val comment: TextView = itemView.findViewById(R.id.comment)
//        val likes: TextView = itemView.findViewById(R.id.likes)
//        val replies: Button = itemView.findViewById(R.id.replies)
//        val replyLayout: LinearLayout = itemView.findViewById(R.id.replyLayout)

        if(v_thumb.isNotEmpty()) {
            Glide.with(this@YTPlayerActivity)
                .load(v_thumb)
                .circleCrop()
                .into(thumb)
        }
        name.text = "$v_name"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            comment.text = "${Html.fromHtml(v_comment, Html.FROM_HTML_MODE_COMPACT)}"
        } else {
            comment.text = Html.fromHtml(v_comment)
        }
//        likes.text = " " + v_likes

        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val output = SimpleDateFormat("dd MMM yyyy")

        var d: Date? = null
        try {
            d = input.parse(v_date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val formatted = output.format(d)
        date.text = formatted

//        replies.visibility = View.GONE
//        if(v_reply_count.toInt() > 0) {
//            replies.visibility = View.VISIBLE
//                recyclerViewHolder.replies.setOnClickListener({
//
//                })
//        }

        commentLayout.addView(itemView)
    }

    fun loadMore(view: View) {
        view.isEnabled = false
        getComments()
    }

}