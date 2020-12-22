package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.IntroActivity
import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityProfileBinding
import `in`.allen.gsp.databinding.ItemScratchcardBinding
import `in`.allen.gsp.databinding.ItemTopicProgressBinding
import `in`.allen.gsp.ui.splash.SplashActivity
import `in`.allen.gsp.utils.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.NestedScrollView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.auth.FirebaseAuth
import dev.skymansandy.scratchcardlayout.listener.ScratchListener
import dev.skymansandy.scratchcardlayout.ui.ScratchCardLayout
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.json.JSONArray
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
    private val repository: UserRepository by instance()
    private val rewardRepository: RewardRepository by instance()

    private var page = 1
    private var nomore = false
    private var loading = true
    private var pastVisibleItems: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

    private val list = ArrayList<HashMap<String,String>>()
    private lateinit var recyclerAdapter: RecyclerViewAdapter

    // bottomsheets
    private lateinit var scratchcardSheetBehavior: BottomSheetBehavior<FrameLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile)
        viewModel = ProfileViewModel(repository,rewardRepository)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        scratchcardSheetBehavior = BottomSheetBehavior.from(binding.layoutScratchcard.bottomSheetScratchcard)
        scratchcardSheetBehavior.isDraggable = false
        binding.layoutScratchcard.btnClose.setOnClickListener {
            if (scratchcardSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                scratchcardSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()
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
        if(id == R.id.menu_logout) {
            val auth = FirebaseAuth.getInstance()
            auth.signOut()
            Intent(this, SplashActivity::class.java)
                .also { it1 ->
                    it1.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(it1)
                }
        }
        if(id == R.id.menu_share) {
            val share = Intent(Intent.ACTION_SEND)
            share.type = "image/jpeg"
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val file = screenShot(binding.statistics, "screenshot.jpg")
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

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private fun observeLoading() {
        viewModel.getLoading().observe(this, {
            tag("$TAG _loading: ${it.message}")
            binding.rootLayout.hideProgress()
            if (it.data is Boolean && it.data) {
                binding.rootLayout.showProgress()
            }
        })
    }

    private fun observeError() {
        viewModel.getError().observe(this, {
            tag("$TAG _error: ${it.message}")
            if (it != null) {
                when (it.message) {
                    "alert" -> {
                        it.data?.let { it1 -> alertDialog("Error", it1) {} }
                    }
                    "tag" -> {
                        it.data?.let { it1 -> tag("$TAG $it1") }
                    }
                    "toast" -> {
                        it.data?.let { it1 -> toast(it1) }
                    }
                    "snackbar" -> {
                        it.data?.let { it1 -> binding.rootLayout.snackbar(it1) }
                    }
                }
            }
        })
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(this, {
            if(it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        val user = it.data as User
                        if (user.avatar.isNotBlank())
                            binding.avatar.loadImage(user.avatar, true)
                        binding.username.text = user.name
                        binding.email.text = user.email
                        if (user.mobile.length > 9) {
                            binding.mobile.text = "Mob. ${user.mobile} "
                            if(user.is_verified == 1) {
                                binding.mobile.setCompoundDrawablesWithIntrinsicBounds(
                                    null,
                                    null,
                                    ResourcesCompat.getDrawable(resources,R.drawable.ic_check,null),
                                    null
                                )
                            }
                        }
                        binding.referralId.text = "Referral Code: ${user.referral_id}"

                        viewModel.statsData(user.user_id)

                        if (!::recyclerAdapter.isInitialized){
                            initRecyclerView()
                            reset()
                            viewModel.scratchcards(page)
                        }
                    }

                    "stats" -> {
                        val obj = it.data as JSONObject
                        binding.totalQuiz.text = obj.getString("total")
                        binding.totalXP.text = obj.getString("xp")
                        setStatistics(obj)
                    }

                    "scratchcards" -> {
                        loading = true
                        if(it.data is JSONObject) {
                            val data = it.data
                            if (!data.getString("cards").equals("false",true)) {
                                val arr: JSONArray = data.getJSONArray("cards")
                                if (arr.length() > 0) {
                                    for (i in 0 until arr.length()) {
                                        val obj = arr[i] as JSONObject
                                        if (obj.getString("value")
                                                .equals("0", ignoreCase = true) && obj.getString("status")
                                                .equals("1", ignoreCase = true)
                                        ) {
                                            continue
                                        }
                                        val hashMap = HashMap<String,String>()
                                        hashMap["id"] = obj.getString("id")
                                        hashMap["type"] = obj.getString("type")
                                        hashMap["description"] = obj.getString("description")
                                        hashMap["value"] = obj.getString("value")
                                        hashMap["create_date"] = obj.getString("create_date")
                                        hashMap["update_date"] = obj.getString("update_date")
                                        hashMap["status"] = obj.getString("status")
                                        list.add(hashMap)
                                    }
                                }

                                page = data.getInt("page")
                                recyclerAdapter.notifyDataSetChanged()
                            } else {
                                nomore = true
                            }
                        }
                    }
                }
            }
        })
    }

    private fun setStatistics(data: JSONObject) {
        // won graph
        val param1 = binding.layoutWin.layoutParams
        (param1 as LinearLayout.LayoutParams).weight = data.getDouble("won").toFloat()
        binding.layoutWin.layoutParams = param1
        binding.percentWin.text = "${formatNumber("#.##",data.getDouble("won").times(100))} %"

        // played graph
        val param2 = binding.layoutPlayed.layoutParams
        (param2 as LinearLayout.LayoutParams).weight = data.getDouble("played").toFloat()
        binding.layoutPlayed.layoutParams = param2
        binding.percentPlayed.text = "${formatNumber("#.##",data.getDouble("played").times(100))} %"

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
        binding.percentLose.text = "${formatNumber("#.##",data.getDouble("lose").times(100))} %"

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
                val topicView: ItemTopicProgressBinding = DataBindingUtil.inflate(
                    layoutInflater, R.layout.item_topic_progress, binding.layoutTopics, false
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
                topicView.topic.text = "${el["topic"]} (${el["percent"]}%)"
                val param = topicView.progressTopic.layoutParams
                (param as LinearLayout.LayoutParams).weight = (el["percent"]?.toFloat()!!/100)
                topicView.progressTopic.layoutParams = param
                binding.layoutTopics.addView(topicView.root)
                scaleView(topicView.progressTopic, 0f, 1f)
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


    private fun initRecyclerView() {
        recyclerAdapter = RecyclerViewAdapter(list,this)
        binding.recyclerView.apply {
            layoutManager =  GridLayoutManager(context,3)
            setHasFixedSize(true)

            binding.scrollView.setOnScrollChangeListener(
                NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, oldScrollY ->
                    tag("scrollY: $scrollY")
                if(v?.getChildAt(v.childCount - 1) != null) {
                    if ((scrollY >= (v.getChildAt(v.childCount - 1).measuredHeight - v.measuredHeight)) &&
                        scrollY > oldScrollY) {
                        visibleItemCount = (layoutManager as GridLayoutManager).childCount
                        totalItemCount = (layoutManager as GridLayoutManager).itemCount
                        pastVisibleItems =
                            (layoutManager as GridLayoutManager?)!!.findFirstVisibleItemPosition()

                        if (loading) {
                            if ((visibleItemCount + pastVisibleItems) >= totalItemCount) {
                                loading = false
                                if (!nomore) {
                                    viewModel.scratchcards(page)
                                }
                            }
                        }
                    }
                }
            })

            adapter = recyclerAdapter
        }
    }

    private class RecyclerViewAdapter(
        val items: ArrayList<HashMap<String, String>>,
        val context: Context,
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder>(){
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val itemBinding: ItemScratchcardBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.item_scratchcard,
                parent,
                false
            )
            return ItemViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            holder.bind(items[position], context)

            holder.binding.root.setOnClickListener {
                showScratchcardSheet(position)
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        class ItemViewHolder(
            val binding: ItemScratchcardBinding
        ): RecyclerView.ViewHolder(binding.root) {
            fun bind(
                data: HashMap<String, String>,
                context: Context
            ) {
                if(data["status"].equals("1",true)) {
                    binding.tileItem.show()
                    binding.tileValue.text = data["value"]
                    binding.tileValue.show()
                    binding.tileCoin.show()
                    binding.tileLayout.background = null
                } else {
                    binding.tileLayout.background = ResourcesCompat.getDrawable(
                        context.resources,R.drawable.scratched_false,null
                    )
                    binding.tileItem.show(false)
                    binding.tileValue.show(false)
                    binding.tileCoin.show(false)
                }

                binding.root.tag = data
                binding.executePendingBindings()
            }
        }

        private fun showScratchcardSheet(position: Int) {
            val ctx = context as ProfileActivity
            val itemData = ctx.list[position]
            ctx.binding.layoutScratchcard.createDate.text = timeInAgo(itemData["create_date"],"yyyy-MM-dd hh:mm:ss")
            ctx.binding.layoutScratchcard.refId.text = "Ref #: ${itemData["id"]}"
            ctx.binding.layoutScratchcard.description.text = "Ref #: ${itemData["description"]}"

            if(itemData["value"].equals("0",true)) {
                ctx.binding.layoutScratchcard.woohoo.text = "Better luck next time!"
            } else {
                ctx.binding.layoutScratchcard.woohoo.text = "Woohoo!!!"
            }

            if(itemData["status"].equals("1",true)) {
                ctx.binding.layoutScratchcard.scratchCardLayout.show(false)
                ctx.binding.layoutScratchcard.itemScratchcard.tileValue.text = itemData["value"]
                ctx.binding.layoutScratchcard.itemScratchcard.root.show()
                ctx.binding.layoutScratchcard.linearLayout.show()
            } else {
                ctx.binding.layoutScratchcard.scratchCardLayout.resetScratch()
                ctx.binding.layoutScratchcard.scratchCardLayout.show()
                ctx.binding.layoutScratchcard.itemScratchcard.root.show(false)
                ctx.binding.layoutScratchcard.linearLayout.show(false)
                ctx.binding.layoutScratchcard.scratchValue.text = itemData["value"]

                ctx.viewModel.flagScratchcard = true
            }

            if (ctx.scratchcardSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                ctx.scratchcardSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                ctx.binding.appBar.show(false)
            }

            ctx.binding.layoutScratchcard.scratchCardLayout.setScratchListener(object:
                ScratchListener {
                override fun onScratchComplete() {
                    ctx.binding.layoutScratchcard.linearLayout.show()

                    if(ctx.viewModel.flagScratchcard) {
                        ctx.viewModel.flagScratchcard = false
                        if(itemData["value"].equals("0",true)) {
                            ctx.list.removeAt(position)
                            ctx.recyclerAdapter.notifyItemRemoved(position)
                            ctx.recyclerAdapter.notifyItemRangeChanged(position,ctx.list.size)
                        } else {
                            ctx.recyclerAdapter.notifyItemChanged(position)
                            ctx.list[position]["status"] = "1"
                        }
                        ctx.viewModel.setScratchcard(itemData["id"]!!.toInt(), 1)
                    }
                }

                override fun onScratchProgress(
                    scratchCardLayout: ScratchCardLayout,
                    atLeastScratchedPercent: Int
                ) {
                    if(atLeastScratchedPercent > 25) {
                        onScratchComplete()
                    }
                }

                override fun onScratchStarted() {
                }
            })

            ctx.binding.layoutScratchcard.btnClose.setOnClickListener {
                if (ctx.scratchcardSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    ctx.scratchcardSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    ctx.binding.appBar.show()
                }
            }
        }
    }

    private fun reset() {
        page = 1
        list.clear()
        if (::recyclerAdapter.isInitialized){
            recyclerAdapter.notifyDataSetChanged()
        }
    }

}