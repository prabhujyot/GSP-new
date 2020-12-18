package `in`.allen.gsp.ui.message

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Message
import `in`.allen.gsp.databinding.ActivityNotificationBinding
import `in`.allen.gsp.databinding.ItemNotificationBinding
import `in`.allen.gsp.utils.*
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.bottomsheet_message.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class NotificationActivity : AppCompatActivity(), KodeinAware {

    private val TAG = NotificationActivity::class.java.name
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var viewModel: NotificationViewModel

    override val kodein by kodein()
    private val factory:NotificationViewModelFactory by instance()

    private var page = 1
    private var nomore = false
    private var loading = true
    private var pastVisibleItems: Int = 0
    private var visibleItemCount: Int = 0
    private var totalItemCount: Int = 0

    private var list = ArrayList<Message>()
    private lateinit var recyclerAdapter: RecyclerViewAdapter

    private lateinit var messageSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_notification)
        viewModel = ViewModelProvider(this, factory).get(NotificationViewModel::class.java)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        initBottomSheet()
        reset()

        observeError()
        observeSuccess()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
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
            tag("$TAG _success: ${it.data}")
            if (it != null) {
                when (it.message) {
                    "notifications" -> {
                        loading = true
                        if(it.data is HashMap<*, *>) {
                            val mList = ArrayList(it.data["list"] as List<Message>)
                            page = it.data["page"] as Int
                            if(mList.size > 0) {
                                list = mList
                                if(page == 2) {
                                    initRecyclerView()
                                } else {
                                    recyclerAdapter.notifyDataSetChanged()
                                }
                            } else {
                                nomore = true
                            }
                        }
                    }
                    "open" -> {
                        val msg = it.data as Message
                        binding.rootLayout.bottomSheetMessage.webView.loadData(
                            msg.msg,
                            "text/html; charset=UTF-8",
                            null)
                        if(messageSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                            messageSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }
                    }
                }
            }
        })
    }

    private fun initBottomSheet() {
        messageSheetBehavior = BottomSheetBehavior.from(binding.rootLayout.bottomSheetMessage)
        messageSheetBehavior.isDraggable = false

        binding.rootLayout.bottomSheetMessage.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.rootLayout.bottomSheetMessage.progressBar.progress = newProgress
            }
        }

        binding.rootLayout.bottomSheetMessage.webView.webViewClient = object : WebViewClient() {
        }

        binding.rootLayout.bottomSheetMessage.btnClose.setOnClickListener {
            if(messageSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                messageSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.rootLayout.isClickable = false
    }

    fun openSheet(notificationId: Int) {
        viewModel.openMessage(notificationId)
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
            holder.bind(items[position])
            if(position == 0) {
                holder.separator.show(false)
            } else {
                holder.separator.show()
            }
        }

        override fun getItemCount(): Int {
            return items.size
        }

        class ItemViewHolder(
            val binding: ItemNotificationBinding,
            val activity: NotificationActivity
        ) : RecyclerView.ViewHolder(binding.root) {
            fun bind(data: Message) {
                tag("bind: $data")
                if(data.status == 0) {
                    binding.time.typeface = Typeface.DEFAULT_BOLD
                    binding.title.typeface = Typeface.DEFAULT_BOLD
                    binding.time.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.white, null))
                    binding.title.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.white, null))
                    binding.msg.setTextColor(ResourcesCompat.getColor(activity.resources, R.color.white, null))
                }
                binding.message = data
                binding.time.text = timeInAgo(data.date,"yyyy-MM-dd hh:mm:ss")

                binding.item.setOnClickListener {
                    activity.openSheet(data.id)
                }

                binding.executePendingBindings()
            }
            val separator: View = binding.separator
        }
    }


}