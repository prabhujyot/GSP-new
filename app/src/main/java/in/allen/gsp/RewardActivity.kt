package `in`.allen.gsp

import `in`.allen.gsp.fragments.PrizeFragment
import `in`.allen.gsp.fragments.StatementFragment
import `in`.allen.gsp.helpers.App
import `in`.allen.gsp.helpers.AppPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_reward.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*

class RewardActivity : AppCompatActivity() {

    private val TAG = RewardActivity::class.java.name

    private lateinit var app: App
    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reward)

        setSupportActionBar(toolbar)
        toolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        app = application as App

        viewPager2.isUserInputEnabled = false
        viewPager2.adapter = FragmentAdapter(this)

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Prizes"
                }
                1 -> {
                    tab.text = "Earned"
                }
                2 -> {
                    tab.text = "Redeemed"
                }
            }
        }.attach()
    }

    private class FragmentAdapter(
        activity: AppCompatActivity
    ): FragmentStateAdapter(activity) {
        val fragmentList = ArrayList<Fragment>()

        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            var frg:Fragment ?= null

            if(position == 0) {
                frg = PrizeFragment.newInstance(position)
            } else if(position > 0) {
                frg = StatementFragment.newInstance(position)
            }

            if(fragmentList.size < itemCount && !fragmentList.contains(frg)) {
                fragmentList.add(frg!!)
            }
            return frg!!
        }

        fun getFragment(position: Int): Fragment {
            Log.d("fragmentList", "" + fragmentList.size)
            return fragmentList[position]
        }

    }

    fun btnActionReward(view: View) {}

}