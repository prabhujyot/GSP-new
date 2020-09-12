package `in`.allen.gsp

import `in`.allen.gsp.fragments.PlaylistFragment
import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import `in`.allen.gsp.helpers.services.WebServices
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_reward.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class VideosActivity : AppCompatActivity() {

    private val TAG = VideosActivity::class.java.name

    lateinit var app: App
    lateinit var webServices: WebServices
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_videos)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App
        webServices = WebServices()

        viewPager2.isUserInputEnabled = false
        viewPager2.adapter = FragmentAdapter(this)

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            if(position == 0) {
                tab.text = "Power of Knowledge"
            } else if(position == 1) {
                tab.text = "Incredible India"
            }
        }.attach()
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