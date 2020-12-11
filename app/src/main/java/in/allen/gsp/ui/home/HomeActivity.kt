package `in`.allen.gsp.ui.home

import `in`.allen.gsp.*
import `in`.allen.gsp.data.entities.Banner
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.BannerRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityHomeBinding
import `in`.allen.gsp.databinding.ItemBannerBinding
import `in`.allen.gsp.ui.leaderboard.LeaderboardActivity
import `in`.allen.gsp.ui.profile.ProfileActivity
import `in`.allen.gsp.ui.quiz.QuizActivity
import `in`.allen.gsp.ui.reward.RewardActivity
import `in`.allen.gsp.ui.videos.VideosActivity
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.viewpager.widget.PagerAdapter
import kotlinx.android.synthetic.main.icon_life.view.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.android.synthetic.main.toolbar_home.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.concurrent.TimeUnit


class HomeActivity : AppCompatActivity(), KodeinAware {

    private val TAG = HomeActivity::class.java.name
    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel

    override val kodein by kodein()
    private val userRepository: UserRepository by instance()
    private val bannerRepository: BannerRepository by instance()
    private val preferences: AppPreferences by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        viewModel = HomeViewModel(userRepository, bannerRepository)

        setSupportActionBar(myToolbar)
        btnClose.setOnClickListener {
            onBackPressed()
        }

        binding.rootLayout.setOnClickListener { hideSystemUI() }

        binding.viewpagerBanner.pageMargin = 16

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()

        userRepository.userLife.observe(this, {
            if(it != null) {
                binding.iconLife.life.text = "${it["life"]}"
                if(System.currentTimeMillis() < preferences.timestampLife) {
                    binding.iconLife.life.text = getString(R.string.infinity)
                    it["life"] = 0
                }
                if (it["life"]!!.toInt() == 5) {
                    binding.lifeTimer.text = "Full"
                } else {
                    it["remaining"]?.let { it1 ->
                        val m =
                            TimeUnit.MILLISECONDS.toMinutes(it1) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(it1)
                            )
                        val s =
                            TimeUnit.MILLISECONDS.toSeconds(it1) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(it1)
                            )
                        val hms = String.format("%02d:%02d", m, s)
                        binding.lifeTimer.text = "$hms"
                    }
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
    }

    override fun onBackPressed() {
        confirmDialog("Exit!", "Tap on Yes to exit.", {
            super.onBackPressed()
        }, {})
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
            tag("$TAG ._success: ${it.data}")
            if (it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "lifeTimer" -> {
                        if (it.data is Long) {
                            val m =
                                TimeUnit.MILLISECONDS.toMinutes(it.data) - TimeUnit.HOURS.toMinutes(
                                    TimeUnit.MILLISECONDS.toHours(it.data)
                                )
                            val s =
                                TimeUnit.MILLISECONDS.toSeconds(it.data) - TimeUnit.MINUTES.toSeconds(
                                    TimeUnit.MILLISECONDS.toMinutes(it.data)
                                )
                            val hms = String.format("%02d:%02d",m,s)
                            binding.lifeTimer.text = "$hms"
                        }
                    }

                    "user" -> {
                        val user = it.data as User
                        if (user.avatar.isNotBlank()) {
                            binding.btnProfileTop.loadImage(user.avatar, true)
                        }
                        viewModel.bannerData(user.user_id)
                    }

                    "banner" -> {
                        if (it.data is Deferred<*>) {
                            val deferredList = it.data as Deferred<LiveData<List<Banner>>>
                            lifecycleScope.launch {
                                deferredList.await().observe(this@HomeActivity, { list ->
                                    binding.viewpagerBanner.adapter = BannerAdapter(
                                        layoutInflater,
                                        list
                                    )
                                })
                            }
                        }
                    }
                }
            }
        })
    }

    private class BannerAdapter(
        private val inflater: LayoutInflater,
        private val list: List<Banner>
    ): PagerAdapter() {

        override fun isViewFromObject(view: View, `object`: Any): Boolean {
            return view === `object`
        }

        override fun getCount(): Int {
            return list.size
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val binding: ItemBannerBinding = DataBindingUtil.inflate(
                inflater,
                R.layout.item_banner,
                container,
                false
            )

            val item = list[position]
            binding.image.loadImage("${BuildConfig.BASE_URL}gsp-admin/uploads/banners/${item.image}")
            binding.title.text = item.title
            binding.btnGo.setOnClickListener {  }

            container.addView(binding.root)
            return binding.root
        }

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }
    }

    fun btnActionHome(view: View) {
        val i = Intent()
        when (view.id) {
            R.id.btnVideos -> {
                i.setClass(this@HomeActivity, VideosActivity::class.java)
            }
            R.id.btnLeaderboard -> {
                i.setClass(this@HomeActivity, LeaderboardActivity::class.java)
            }
            R.id.btnProfileTop -> {
                i.setClass(this@HomeActivity, ProfileActivity::class.java)
            }
            R.id.btnPlay -> {
                i.setClass(this@HomeActivity, QuizActivity::class.java)
            }
            R.id.btnProfile -> {
                i.setClass(this@HomeActivity, ProfileActivity::class.java)
            }
            R.id.btnCoins -> {
                i.setClass(this@HomeActivity, RewardActivity::class.java)
            }
            R.id.btnNotification -> {
                i.setClass(this@HomeActivity, NotificationActivity::class.java)
            }
            R.id.btnContests -> {
                i.setClass(this@HomeActivity, WebActivity::class.java)
                i.putExtra("url", "${BuildConfig.BASE_URL}category/quiz-time/")
            }
//            R.id.btnSetting -> {
//                i.setClass(this@HomeActivity, RewardActivity::class.java)
//            }
        }
        startActivity(i)
    }
}