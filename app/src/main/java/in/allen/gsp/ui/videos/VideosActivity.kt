package `in`.allen.gsp.ui.videos

import `in`.allen.gsp.R
import `in`.allen.gsp.databinding.ActivityVideosBinding
import `in`.allen.gsp.utils.hideSystemUI
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.toolbar.view.*

class VideosActivity : AppCompatActivity() {

    private val TAG = VideosActivity::class.java.name
    private lateinit var binding: ActivityVideosBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_videos)

        setSupportActionBar(binding.rootLayout.myToolbar)
        binding.rootLayout.myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.adapter = FragmentAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            if(position == 0) {
                tab.text = "Power of Knowledge"
            } else if(position == 1) {
                tab.text = "Incredible India"
            }
        }.attach()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    private class FragmentAdapter(
        activity: AppCompatActivity
    ): FragmentStateAdapter(activity) {
        val fragmentList = ArrayList<Fragment>()

        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment {
            val playlistId: String = if(position == 0) {
                "PLQ2YKhBryYBzh3NJW1uj1BWZhSODodI8r"
            } else {
                "PLQ2YKhBryYByhl0Zh-gluJ0uHpq3frZWy"
            }

            val frg = PlaylistFragment.newInstance(playlistId)

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