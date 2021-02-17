package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Comment
import `in`.allen.gsp.data.repositories.VideosRepository
import `in`.allen.gsp.databinding.ActivityYtPlayerBinding
import `in`.allen.gsp.databinding.ItemCommentBinding
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.databinding.DataBindingUtil
import com.google.android.youtube.player.YouTubeBaseActivity
import com.google.android.youtube.player.YouTubeInitializationResult
import com.google.android.youtube.player.YouTubePlayer
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


class YTPlayerActivity: YouTubeBaseActivity(),
    YouTubePlayer.OnInitializedListener, KodeinAware {

    private val TAG = YTPlayerActivity::class.java.name
    private lateinit var binding: ActivityYtPlayerBinding

    override val kodein by kodein()
    private val videosRepository: VideosRepository by instance()

    private var videoId = ""
    private val RECOVERY_REQUEST = 1

    private lateinit var loadMore: Button
    private var pageToken = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_yt_player)

        this.setFinishOnTouchOutside(false)
        binding.youtubeView.initialize(getString(R.string.yt_api_key), this)

        if(intent.getStringExtra("videoId") != null) {
            videoId = intent.getStringExtra("videoId")!!
            reset()

            // get video
            getVideo()

            // get comments
            getComments()
        }

        binding.commentLayout.setOnClickListener {
            showSystemUI()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RECOVERY_REQUEST) {
            // Retry initialization if user performed a recovery action
            binding.youtubeView.initialize(getString(R.string.yt_api_key), this)
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
        binding.commentLayout.removeAllViews()
        pageToken = ""
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

        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
        val output = SimpleDateFormat("dd MMM yyyy")

        var d: Date? = null
        try {
            d = input.parse(v_date)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        val formatted = output.format(d)

        val cmtBinding:ItemCommentBinding = DataBindingUtil.inflate(
            layoutInflater,
            R.layout.item_comment,
            binding.commentLayout,
            false)

        val cmt = Comment(v_name,v_comment,formatted,v_thumb)

        cmtBinding.comment = cmt

        if(cmt.thumb.isNotEmpty()) {
            cmtBinding.thumb.loadImage(cmt.thumb,true)
        }

        binding.commentLayout.addView(cmtBinding.root)
    }

    fun loadMore(view: View) {
        view.isEnabled = false
        getComments()
    }

    private fun getVideo() {
        val hashMap = HashMap<String,String>()
        hashMap["part"] = "snippet,statistics"
        hashMap["id"] = videoId
        hashMap["key"] = "AIzaSyDbpMioiHvdMHmA41UETZy6sCO1txortVc"

        Coroutines.main {
            try {
                val response = videosRepository.getVideoDetails(hashMap)
                if (response != null) {
                    val responseObj = JSONObject(response)
                    tag("responseObj: $responseObj")
                    if (!responseObj.has("error")) {
                        val data = responseObj.getJSONArray("items")
                        if (data.length() > 0) {
                            val item = data[0] as JSONObject

                            binding.title.text = item.getJSONObject("snippet").getString("title")
                            binding.views.text =
                                "${item.getJSONObject("statistics").getString("viewCount")} . views"
                            binding.likes.text = "Likes: ${
                                item.getJSONObject("statistics").getString("favoriteCount")
                            }"

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                binding.description.text = Html.fromHtml(
                                    item.getJSONObject("snippet").getString("description"),
                                    Html.FROM_HTML_MODE_COMPACT
                                )
                            } else {
                                binding.description.text = Html.fromHtml(
                                    item.getJSONObject("snippet").getString("description")
                                )
                            }
                        }
                    } else {
                        binding.rootLayout.snackbar(
                            responseObj.getJSONObject("error").getString("message")
                        )
                    }
                }
            } catch (e: Exception) {
                tag("$TAG ${e.message}")
            }
        }
    }

    private fun getComments() {
        val hashMap2 = HashMap<String,String>()
        hashMap2["part"] = "snippet"
        hashMap2["maxResults"] = "50"
        hashMap2["videoId"] = videoId
        hashMap2["pageToken"] = pageToken
        hashMap2["key"] = "AIzaSyDbpMioiHvdMHmA41UETZy6sCO1txortVc"

        Coroutines.main {
            try {
                val response = videosRepository.getComments(hashMap2)
                if (response != null) {
                    pageToken = ""
                    binding.loadMore.show(false)

                    val responseObj = JSONObject(response)
                    tag("responseObj: $responseObj")
                    if (!responseObj.has("error")) {
                        val arr = responseObj.getJSONArray("items")
                        if (arr.length() > 0) {
                            for (i in 0..arr.length().minus(1)) {
                                val item = arr.get(i) as JSONObject
                                addCommentView(item)
                            }
                        }

                        if (responseObj.has("nextPageToken")) {
                            pageToken = responseObj.getString("nextPageToken")
                            binding.loadMore.isEnabled = true
                            binding.loadMore.show()
                        }
                    }
                }
            } catch (e: Exception) {
                tag("$TAG ${e.message}")
            }
        }
    }
}