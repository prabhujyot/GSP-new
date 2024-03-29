package `in`.allen.gsp.ui.message

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Message
import `in`.allen.gsp.databinding.ActivityNotificationBinding
import `in`.allen.gsp.databinding.ItemNotificationBinding
import `in`.allen.gsp.utils.*
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class NotificationActivity : AppCompatActivity(), DIAware {

    private val TAG = NotificationActivity::class.java.name
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var viewModel: NotificationViewModel

    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val factory:NotificationViewModelFactory by instance()

    private var page = 1
    private var nomore = false
    private var loading = true
    private var pastVisibleItems: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

    private var list = ArrayList<Message>()
    private lateinit var recyclerAdapter: RecyclerViewAdapter

    private lateinit var bottomSheetMessage: ConstraintLayout
    private lateinit var messageSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_notification)
        viewModel = ViewModelProvider(this, factory)[NotificationViewModel::class.java]

        val toolbar = binding.root.findViewById<Toolbar>(R.id.myToolbar)

        setSupportActionBar(toolbar)
        toolbar.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        bottomSheetMessage = binding.root.findViewById(R.id.bottomSheetMessage)

        initBottomSheet()
        reset()

        observeError()
        observeSuccess()
    }

    override fun onBackPressed() {
        if(::messageSheetBehavior.isInitialized &&
            messageSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            messageSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    private fun observeError() {
        viewModel.getError().observe(this) {
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
        }
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(this) {
            tag("$TAG _success: ${it.data}")
            if (it != null) {
                when (it.message) {
                    "notifications" -> {
                        loading = true
                        if (it.data is HashMap<*, *>) {
                            val mList = ArrayList(it.data["list"] as List<Message>)
                            page = it.data["page"] as Int
                            if (mList.size > 0) {
                                list = mList
                                if (page == 2) {
                                    initRecyclerView()
                                } else {
                                    recyclerAdapter.notifyDataSetChanged()
                                }
                            } else {
                                nomore = true
                            }
                        }

                        if (list.isNotEmpty()) {
                            binding.noData.show(false)
                        }
                    }
                    "open" -> {
                        if (it.data is HashMap<*, *>) {
                            val msg = it.data["item"] as Message
                            val position = it.data["position"] as Int
                            bottomSheetMessage.findViewById<WebView>(R.id.webView).loadData(
                                msg.msg,
                                "text/html; charset=UTF-8",
                                null
                            )
                            if (messageSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                                messageSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                            }

                            if (::recyclerAdapter.isInitialized) {
                                list[position].status = 1
                                recyclerAdapter.notifyItemChanged(position)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun initBottomSheet() {
        messageSheetBehavior = BottomSheetBehavior.from(bottomSheetMessage)
        messageSheetBehavior.isDraggable = false

        val webView:WebView = bottomSheetMessage.findViewById(R.id.webView)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                bottomSheetMessage.findViewById<ProgressBar>(R.id.progressBar).progress = newProgress
            }
        }

        webView.webViewClient = object : WebViewClient() {
        }

        bottomSheetMessage.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            if(messageSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                messageSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.rootLayout.isClickable = false
    }

    fun openSheet(notificationId: Int, position: Int) {
        viewModel.openMessage(notificationId,position)
    }

    private fun reset() {
        page = 1
        list.clear()
        if (::recyclerAdapter.isInitialized){
            recyclerAdapter.notifyDataSetChanged()
        }
    }

    private fun initRecyclerView() {
        recyclerAdapter = RecyclerViewAdapter(list, this)
        binding.recyclerView.apply {
            layoutManager =  LinearLayoutManager(context)
            setHasFixedSize(true)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        visibleItemCount = (layoutManager as LinearLayoutManager).childCount
                        totalItemCount = (layoutManager as LinearLayoutManager).itemCount
                        pastVisibleItems =
                            (recyclerView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition()
                        if (loading) {
                            if (visibleItemCount + pastVisibleItems >= totalItemCount - 2) {
                                loading = false
                                if (!nomore) {
                                    viewModel.getNotifications(page)
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
        val items: ArrayList<Message>,
        val context: Context,
    ) : RecyclerView.Adapter<RecyclerViewAdapter.ItemViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val binding: ItemNotificationBinding = DataBindingUtil.inflate(
                LayoutInflater.from(context),
                R.layout.item_notification,
                parent,
                false
            )
            val parentActivity = context as NotificationActivity
            return ItemViewHolder(binding,parentActivity)
        }

        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = items[position]
            holder.bind(item,position)
            if(position == 0) {
                holder.binding.separator.show(false)
            } else {
                holder.binding.separator.show()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        class ItemViewHolder(
            val binding: ItemNotificationBinding,
            val activity: NotificationActivity
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(data: Message, position: Int) {
                tag("bind: $data")

                if(data.status == 0) {
                    binding.time.typeface = Typeface.DEFAULT_BOLD
                    binding.title.typeface = Typeface.DEFAULT_BOLD
                    binding.time.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.white, null))
                    binding.title.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.white, null))
                    binding.msg.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.white, null))
                } else {
                    binding.time.typeface = Typeface.DEFAULT
                    binding.title.typeface = Typeface.DEFAULT
                    binding.time.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.grey, null))
                    binding.title.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.grey, null))
                    binding.msg.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.grey, null))
                }

                binding.message = data
                binding.time.text = timeInAgo(data.date,"yyyy-MM-dd HH:mm:ss")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    binding.msg.text = Html.fromHtml(data.msg + "...", Html.FROM_HTML_MODE_COMPACT)
                } else {
                    binding.msg.text = Html.fromHtml(data.msg + "...")
                }

                binding.item.setOnClickListener {
                    activity.openSheet(data.id,position)
                }

                binding.executePendingBindings()
            }
        }
    }


}