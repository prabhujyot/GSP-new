package `in`.allen.gsp.ui.reward

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import `in`.allen.gsp.WebActivity
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.databinding.ActivityRewardBinding
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.bottomsheet_redemption.view.*
import kotlinx.android.synthetic.main.checkin.view.*
import kotlinx.android.synthetic.main.fragment_prize.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance


class RewardActivity : AppCompatActivity(), KodeinAware {

    private val TAG = RewardActivity::class.java.name
    private lateinit var binding: ActivityRewardBinding
    private lateinit var viewModel: RewardViewModel

    override val kodein by kodein()
    private val factory:RewardViewModelFactory by instance()

    private lateinit var redeemSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_reward)
        viewModel = ViewModelProvider(this, factory).get(RewardViewModel::class.java)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        binding.viewPager2.isUserInputEnabled = false
        binding.viewPager2.adapter = FragmentAdapter(this)

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
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

        observeLoading()
        observeError()
        observeSuccess()

        viewModel.userData()
    }

    override fun onBackPressed() {
        if(::redeemSheetBehavior.isInitialized &&
            redeemSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            redeemSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.reward, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.menu_instructions) {
            Intent(this, WebActivity::class.java)
                .also { it1 ->
                    it1.putExtra(
                        "url",
                        BuildConfig.BASE_URL + "gsp-admin/index.php/site/page/coins-instructions"
                    )
                    startActivity(it1)
                }
        }
        if(id == R.id.menu_invite) {
            viewModel.setLoading(true)
            val url = BuildConfig.BASE_URL + "gsp-admin/uploads/banners/gsp_refer.png"
            val path = getExternalFilesDir(null).toString()
            PRDownloader.download(url, path, "refer.png")
                .build()
                .setOnStartOrResumeListener { }
                .setOnPauseListener { }
                .setOnCancelListener { }
                .setOnProgressListener { }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        viewModel.setSuccess("","share")
                    }
                    override fun onError(error: com.downloader.Error?) {
                        viewModel.setLoading(false)
                    }
                })
        }
        return super.onOptionsItemSelected(item)
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
                        binding.totalCoins.text = "${user.coins}"
                        binding.coinValue.text = "${user.coins.div(viewModel.coinsValue)} INR"

                        if (!viewModel.dailyReward) {
                            viewModel.getDailyReward()
                            viewModel.dailyReward = true
                        }
                        initBottomSheet()
                    }

                    "getDailyReward" -> {
                        if (it.data is JSONObject) {
                            val obj = it.data
                            setDailyCheckinViews(obj.getInt("diff"), obj.getInt("today_value"))
                        }
                    }

                    "setDailyReward" -> {
                        if (it.data is JSONObject) {
                            val obj = it.data
                            setDailyCheckinViews(obj.getInt("diff"), obj.getInt("today_value"))
                        }
                    }

                    "redeem" -> {
                        if (it.data is String) {
                            viewModel.setError(it.data, "snackbar")
                        }
                    }

                    "share" -> {
                        if (it.data is String) {
                            share(
                                "refer.png",
                                "Hey, I can’t stop playing this game. I believe you’d like it as well." +
                                        " Use my referral link to download 'Gyan Se Pehchan' app" +
                                        " and get coin benefits when you join and play! \n" + Uri.parse(
                                    viewModel.user?.referral_id?.let { it1 -> getReferralLink(it1) }
                                )
                            )
                        }
                    }
                }
            }
        })
    }

    private class FragmentAdapter(
        activity: AppCompatActivity
    ): FragmentStateAdapter(activity) {
        val fragmentList = ArrayList<Fragment>()

        override fun getItemCount(): Int {
            return 3
        }

        override fun createFragment(position: Int): Fragment {
            val frg: Fragment? = when (position) {
                0 -> {
                    PrizeFragment.newInstance(position)
                }
                1 -> {
                    StatementFragment.newInstance("earn")
                }
                else -> {
                    StatementFragment.newInstance("redeem")
                }
            }

            if(fragmentList.size < itemCount && !fragmentList.contains(frg)) {
                if (frg != null) {
                    fragmentList.add(frg)
                }
            }
            return frg!!
        }

        fun getFragment(position: Int): Fragment {
            Log.d("fragmentList", "" + fragmentList.size)
            return fragmentList[position]
        }
    }

    private fun initBottomSheet() {
        redeemSheetBehavior = BottomSheetBehavior.from(binding.rootLayout.bottomSheetRedeem)
        redeemSheetBehavior.isDraggable = false

        binding.rootLayout.bottomSheetRedeem.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.rootLayout.bottomSheetRedeem.progressBar.progress = newProgress
            }
        }

        binding.rootLayout.bottomSheetRedeem.webView.webViewClient = object : WebViewClient() {
        }

        binding.rootLayout.bottomSheetRedeem.webView.settings.javaScriptEnabled = true
        val url = BuildConfig.BASE_URL + "gsp-admin/index.php/site/page/redemption-rule"
        binding.rootLayout.bottomSheetRedeem.webView.loadUrl(url)

        binding.rootLayout.bottomSheetRedeem.card50.apply {
            val coins: Int = 50 * viewModel.coinsValue
            this.findViewById<TextView>(R.id.coins50).text = "$coins"
            setOnClickListener {
                confirmRedeem(coins)
            }
        }

        binding.rootLayout.bottomSheetRedeem.card100.apply {
            val coins: Int = 100 * viewModel.coinsValue
            this.findViewById<TextView>(R.id.coins100).text = "$coins"
            setOnClickListener {
                confirmRedeem(coins)
            }
        }

        binding.rootLayout.bottomSheetRedeem.card150.apply {
            val coins: Int = 150 * viewModel.coinsValue
            this.findViewById<TextView>(R.id.coins150).text = "$coins"
            setOnClickListener {
                confirmRedeem(coins)
            }
        }

        binding.rootLayout.bottomSheetRedeem.card200.apply {
            val coins: Int = 200 * viewModel.coinsValue
            this.findViewById<TextView>(R.id.coins200).text = "$coins"
            setOnClickListener {
                confirmRedeem(coins)
            }
        }

        binding.rootLayout.isClickable = false
    }

    private fun confirmRedeem(coins: Int) {
        confirmDialog(
            "Coins Redeem",
            "$coins coins will be redeem, are you agree with terms and conditions?",
            { viewModel.redeem(coins) },
            {}
        )
    }

    fun btnActionReward(view: View) {
        when (view.id) {
            R.id.btnRedeem -> {
                if (redeemSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    redeemSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            R.id.btnClose -> {
                if (redeemSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    redeemSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }

            R.id.btnCheckin -> {
                binding.rootLayout.btnCheckin.show(false)
                viewModel.setDailyReward(view.tag as Int)
            }

            R.id.card50 -> {
                if (redeemSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    redeemSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            R.id.card100 -> {
                if (redeemSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    redeemSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            R.id.card150 -> {
                if (redeemSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    redeemSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }

            R.id.card200 -> {
                if (redeemSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                    redeemSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
    }


    private fun setDailyCheckinViews(diff: Int, todayValue: Int) {
        binding.rootLayout.btnCheckin.show(false)
        if (diff > 0) {
            binding.rootLayout.btnCheckin.tag = todayValue
            binding.rootLayout.btnCheckin.show()
        }

        // Setting drawables
        for(i in 0 until 7) {
            val imgId = "imgDay" + (i+1)
            val textId = "textDay" + (i+1)
            val lblId = "lblDay" + (i+1)

            val imgView: ImageView = binding.rootLayout.findViewById(
                resources.getIdentifier(
                    imgId,
                    "id",
                    packageName
                )
            )
            val textView: TextView = binding.rootLayout.findViewById(
                resources.getIdentifier(
                    textId,
                    "id",
                    packageName
                )
            )
            val lblView: TextView = binding.rootLayout.findViewById(
                resources.getIdentifier(
                    lblId,
                    "id",
                    packageName
                )
            )

            if (textView.text.toString().toInt() < todayValue) {
                imgView.setImageDrawable(
                    ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.bg_btn_black,
                        null
                    )
                )
                lblView.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.ic_check,
                    null
                )
                ViewCompat.setBackgroundTintList(
                    lblView,
                    ColorStateList.valueOf(ResourcesCompat.getColor(resources, R.color.black, null))
                )
                lblView.text = null
            }
            if (textView.text.toString().toInt() == todayValue) {
                if (diff == 0) {
                    imgView.setImageDrawable(
                        ResourcesCompat.getDrawable(
                            resources,
                            R.drawable.bg_btn_black,
                            null
                        )
                    )
                    lblView.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.ic_check,
                        null
                    )
                    ViewCompat.setBackgroundTintList(
                        lblView,
                        ColorStateList.valueOf(
                            ResourcesCompat.getColor(
                                resources,
                                R.color.black,
                                null
                            )
                        )
                    )
                    lblView.text = null
                }
            }
        }
    }

}