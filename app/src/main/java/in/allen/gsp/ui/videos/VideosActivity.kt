package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.repositories.VideosRepository
import `in`.allen.gsp.databinding.ActivityVideosBinding
import `in`.allen.gsp.utils.*
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_videos.view.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.json.JSONArray
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class VideosActivity : AppCompatActivity(), KodeinAware {

    private val TAG = VideosActivity::class.java.name
    private lateinit var binding: ActivityVideosBinding
    private lateinit var viewModel: VideosViewModel

    override val kodein by kodein()
    private val userRepository: UserRepository by instance()
    private val videoRepository: VideosRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_videos)
        viewModel = VideosViewModel(userRepository, videoRepository)

        binding.rootLayout.toolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        observeSuccess()
        observeLoading()
        observeError()
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
                binding.rootLayout.hideProgress()
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
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        val user = it.data as User
                        val channelList = readStringFromFile("channelList")
                        if (channelList.trim().length < 5) {
                            viewModel.channelList(user.user_id)
                        } else {
                            viewModel.setSuccess(channelList, "displayChannels")
                        }
                    }
                    "channelList" -> {
                        writeStringToFile(it.data.toString(), "channelList")
                        viewModel.setSuccess(it.data.toString(), "displayChannels")
                    }
                    "displayChannels" -> {
                        if (it.data is String) {
                            val arr = JSONArray(it.data)
                            if (arr.length() > 0) {
                                tabs(arr)
                            }
                        }
                    }
                }
            }
        })
    }

    // tabs
    private fun tabs(list: JSONArray) {
        binding.viewPager2.adapter = FragmentAdapter(this, list)
        binding.viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val item = list[position] as JSONObject
                val channelThumb: String = item.getString("channelThumb")
                binding.channelItem.tileItem.loadImage(channelThumb, true, true)
            }
        })

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            val item = list[position] as JSONObject
            tab.text = item.getString("channelTitle")
        }.attach()

        for (i in 0 until binding.tabLayout.tabCount) {
            val tab = (binding.tabLayout.getChildAt(0) as ViewGroup).getChildAt(i)
            val p = tab.layoutParams as MarginLayoutParams
            p.setMargins(8, 0, 8, 8)
            tab.requestLayout()
        }
    }

    private class FragmentAdapter(
        activity: AppCompatActivity,
        val list: JSONArray
    ): FragmentStateAdapter(activity) {
        val fragmentList = ArrayList<Fragment>()

        override fun getItemCount(): Int {
            return list.length()
        }

        override fun createFragment(position: Int): Fragment {
            val item = list[position] as JSONObject
            val channelId: String = item.getString("channelId")

            val frg = PlaylistFragment.newInstance(channelId)

            if(fragmentList.size < itemCount && !fragmentList.contains(frg)) {
                fragmentList.add(frg)
            }
            return frg
        }

        fun getFragment(position: Int): Fragment {
            Log.d("fragmentList", "" + fragmentList.size)
            return fragmentList[position]
        }
    }

}