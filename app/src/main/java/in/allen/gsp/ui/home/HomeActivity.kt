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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class HomeActivity : AppCompatActivity(), KodeinAware {

    private val TAG = HomeActivity::class.java.name
    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel

    override val kodein by kodein()
    private val userRepository: UserRepository by instance()
    private val bannerRepository: BannerRepository by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        viewModel = HomeViewModel(userRepository,bannerRepository)

        binding.viewpagerBanner.pageMargin = 16

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()
    }


    private fun observeLoading() {
        viewModel.stateLoading().observe(this, {
            tag("$TAG viewModel._loading: ${it.message}")
            binding.rootLayout.showProgress()
        })
    }

    private fun observeError() {
        viewModel.stateError().observe(this, {
            tag("$TAG viewModel._error: ${it.message}")
            binding.rootLayout.hideProgress()

            it.message?.let { it1 ->
                if(it1.isNotBlank())
                    binding.rootLayout.snackbar(it1.trim())
            }
        })
    }

    private fun observeSuccess() {
        viewModel.stateSuccess().observe(this, {
            tag("$TAG viewModel._success: ${it.data}")
            if(it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        val user = it.data as User
                        if (user.avatar.isNotBlank())
                            binding.btnProfileTop.loadImage(user.avatar, true)

                        viewModel.bannerData(user.user_id)
                    }

                    "banner" -> {
                        if(it.data is Deferred<*>) {
                            val deferredList = it.data as Deferred<LiveData<List<Banner>>>
                            lifecycleScope.launch {
                                deferredList.await().observe(this@HomeActivity, { list->
                                    binding.viewpagerBanner.adapter = BannerAdapter(layoutInflater,
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
            val binding: ItemBannerBinding = DataBindingUtil.inflate(inflater,R.layout.item_banner,container,false)

            val item = list[position]
            binding.image.loadImage("https://www.klipinterest.com/gsp-admin/uploads/banners/${item.image}")
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
                i.setClass(this@HomeActivity, PlayActivity::class.java)
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
                i.putExtra("url",BuildConfig.BASE_URL + "category/quiz-time/")
            }
            R.id.btnSetting -> {
                i.setClass(this@HomeActivity, RewardActivity::class.java)
            }
        }
        startActivity(i)
    }
}