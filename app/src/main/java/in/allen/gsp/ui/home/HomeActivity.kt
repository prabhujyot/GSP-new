package `in`.allen.gsp.ui.home

import `in`.allen.gsp.*
import `in`.allen.gsp.data.entities.Banner
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.BannerRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityHomeBinding
import `in`.allen.gsp.ui.leaderboard.LeaderboardActivity
import `in`.allen.gsp.ui.message.NotificationActivity
import `in`.allen.gsp.ui.profile.ProfileActivity
import `in`.allen.gsp.ui.quiz.QuizActivity
import `in`.allen.gsp.ui.reward.RewardActivity
import `in`.allen.gsp.ui.videos.VideosActivity
import `in`.allen.gsp.utils.*
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import com.asksira.loopingviewpager.LoopingPagerAdapter
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.tasks.Task
import kotlinx.android.synthetic.main.icon_life.view.*
import kotlinx.android.synthetic.main.toolbar.view.*
import kotlinx.android.synthetic.main.toolbar_home.*
import kotlinx.android.synthetic.main.update.view.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*
import java.util.concurrent.TimeUnit


private const val REQUEST_UPDATE = 100
private const val APP_UPDATE_TYPE_SUPPORTED = AppUpdateType.FLEXIBLE

class HomeActivity : AppCompatActivity(), KodeinAware {

    private val TAG = HomeActivity::class.java.name
    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel

    override val kodein by kodein()
    private val userRepository: UserRepository by instance()
    private val bannerRepository: BannerRepository by instance()
    private val preferences: AppPreferences by instance()

    private lateinit var app: App

    private var autoscroll = "true"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        viewModel = HomeViewModel(userRepository, bannerRepository)

        setSupportActionBar(myToolbar)
        btnClose.setOnClickListener {
            onBackPressed()
        }

        app = application as App

        binding.rootLayout.setOnClickListener { hideSystemUI() }

        binding.viewpagerBanner.pageMargin = 16

//        if (BuildConfig.DEBUG) {
//            testingInAppUpdate()
//        }

        checkInAppUpdate()

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()

        lifecycleScope.launch {
            autoscroll = userRepository.config("banner-scroll")
        }

        userRepository.userLife.observe(this, {
            if (it != null) {
                binding.iconLife.life.text = "${it["life"]}"
                if (System.currentTimeMillis() < preferences.timestampLife) {
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

    override fun onPause() {
        super.onPause()
        binding.viewpagerBanner.pauseAutoScroll()
        if(::app.isInitialized) {
            app.getmServ()?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(::app.isInitialized) {
            app.unbindMusicService()
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        if(!autoscroll.equals("true",true)) {
            binding.viewpagerBanner.pauseAutoScroll()
        } else {
            binding.viewpagerBanner.resumeAutoScroll()
        }
//        if(::app.isInitialized) {
//            app.getmServ()?.playMusic("round", true)
//        }
    }

    override fun onBackPressed() {
        confirmDialog("Exit!", "Tap on Yes to exit.", {
            super.onBackPressed()
        }, {})
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (REQUEST_UPDATE == requestCode) {
            when (resultCode) {
                RESULT_OK -> {
                    if (APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.IMMEDIATE) {
                        toast(getString(R.string.toast_updated))
                    } else {
                        toast(getString(R.string.toast_started))
                    }
                }
                RESULT_CANCELED -> {
                    toast(getString(R.string.toast_cancelled))
                }
                ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> {
                    toast(getString(R.string.toast_failed))
                }
            }
            super.onActivityResult(requestCode, resultCode, data)
        }
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
                            val hms = String.format("%02d:%02d", m, s)
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
                                        this@HomeActivity,
                                        list,
                                        true
                                    )

                                    tag("autoscroll: $autoscroll")
                                    if(!autoscroll.equals("true",true)) {
                                        binding.viewpagerBanner.pauseAutoScroll()
                                    } else {
                                        binding.viewpagerBanner.resumeAutoScroll()
                                    }
//                                    autoscroll(true)
                                })
                            }
                        }
                    }
                }
            }
        })
    }

//    private class BannerAdapter(
//        private val inflater: LayoutInflater,
//        private val list: List<Banner>,
//        private val context: Context
//    ): PagerAdapter() {
//
//        override fun isViewFromObject(view: View, `object`: Any): Boolean {
//            return view === `object`
//        }
//
//        override fun getCount(): Int {
//            return list.size
//        }
//
//        override fun instantiateItem(container: ViewGroup, position: Int): Any {
//            val binding: ItemBannerBinding = DataBindingUtil.inflate(
//                inflater,
//                R.layout.item_banner,
//                container,
//                false
//            )
//
//            val item = list[position]
//            binding.image.loadImage("${BuildConfig.BASE_URL}gsp-admin/uploads/banners/${item.image}")
//            binding.image.setOnClickListener {
//                if(item.action.trim().length > 10) {
//                    val i = Intent()
//                    i.setClassName(context, item.action)
//                    if(item.meta.trim().length > 4) {
//                        val obj = JSONObject(item.meta)
//                        when {
//                            obj.has("url") -> {
//                                i.putExtra("url", obj.getString("url"))
//                            }
//                            obj.has("contest_id") -> {
//                                i.putExtra("contest_id", obj.getString("contest_id"))
//                            }
//                        }
//                    }
//                    context.startActivity(i)
//                }
//            }
//
//            container.addView(binding.root)
//            return binding.root
//        }
//
//        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
//            container.removeView(`object` as View)
//        }
//    }

    class BannerAdapter(
        context: Context,
        itemList: List<Banner>,
        isInfinite: Boolean
    ) : LoopingPagerAdapter<Banner>(context, itemList, isInfinite) {

        //This method will be triggered if the item View has not been inflated before.
        override fun inflateView(
            viewType: Int,
            container: ViewGroup,
            listPosition: Int
        ): View {
            return LayoutInflater.from(context).inflate(R.layout.item_banner, container, false)
        }

        //Bind your data with your item View here.
        //Below is just an example in the demo app.
        //You can assume convertView will not be null here.
        //You may also consider using a ViewHolder pattern.
        override fun bindView(
            convertView: View,
            listPosition: Int,
            viewType: Int
        ) {

            val item = itemList?.get(listPosition)
            val image = convertView.findViewById<ImageView>(R.id.image)
            image.loadImage("${BuildConfig.BASE_URL}gsp-admin/uploads/banners/${item?.image}")
            image.setOnClickListener {
                if(item?.action?.trim()!!.length > 10) {
                    val i = Intent()
                    i.setClassName(context, item.action)
                    if(item.meta.trim().length > 4) {
                        val obj = JSONObject(item.meta)
                        when {
                            obj.has("url") -> {
                                i.putExtra("url", obj.getString("url"))
                            }
                            obj.has("contest_id") -> {
                                i.putExtra("contest_id", obj.getString("contest_id"))
                            }
                        }
                    }
                    context.startActivity(i)
                }
            }
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



    /* In-App update */
    private lateinit var updateListener: InstallStateUpdatedListener

    private fun checkInAppUpdate() {
        val appUpdateManager = AppUpdateManagerFactory.create(baseContext)
        val appUpdateInfo = appUpdateManager.appUpdateInfo
        appUpdateInfo.addOnSuccessListener {
            handleUpdate(appUpdateManager, appUpdateInfo)
        }
    }

    private fun handleUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        if (APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.IMMEDIATE) {
            handleImmediateUpdate(manager, info)
        } else if (APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.FLEXIBLE) {
            handleFlexibleUpdate(manager, info)
        }
    }

    private fun handleImmediateUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        if ((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
            info.result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            manager.startUpdateFlowForResult(
                info.result,
                AppUpdateType.IMMEDIATE,
                this,
                REQUEST_UPDATE
            )
        }
    }

    private fun handleFlexibleUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        if ((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
            info.result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            binding.layoutUpdate.show()
            setUpdateAction(manager, info)
        }
    }

    private fun setUpdateAction(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        binding.layoutUpdate.btn_update.setOnClickListener {
            updateListener = InstallStateUpdatedListener {
                binding.layoutUpdate.btn_update.show(false)
                binding.layoutUpdate.status.show()
                when (it.installStatus()) {
                    InstallStatus.FAILED, InstallStatus.UNKNOWN -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_failed)
                        binding.layoutUpdate.btn_update.visibility = View.VISIBLE
                    }
                    InstallStatus.PENDING -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_pending)
                    }
                    InstallStatus.CANCELED -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_canceled)
                    }
                    InstallStatus.DOWNLOADING -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_downloading)
                    }
                    InstallStatus.DOWNLOADED -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_installing)
                        launchRestart(manager)
                    }
                    InstallStatus.INSTALLING -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_installing)
                    }
                    InstallStatus.INSTALLED -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_installed)
                        manager.unregisterListener(updateListener)
                    }
                    else -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_restart)
                    }
                }
            }
            manager.registerListener(updateListener)
            manager.startUpdateFlowForResult(
                info.result,
                AppUpdateType.FLEXIBLE,
                this,
                REQUEST_UPDATE
            )
        }
    }

    private fun launchRestart(manager: AppUpdateManager) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.update_title))
            .setMessage(getString(R.string.update_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.action_restart)) { _, _ ->
                manager.completeUpdate()
            }
            .create().show()
    }

    // testing in app update
    private fun testingInAppUpdate() {
        val appUpdateManager = FakeAppUpdateManager(baseContext)
        appUpdateManager.setUpdateAvailable(13)

        val appUpdateInfo = appUpdateManager.appUpdateInfo
        appUpdateInfo.addOnSuccessListener {
            handleTestingUpdate(appUpdateManager, appUpdateInfo)
        }
    }

    private fun handleTestingUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        if (APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.IMMEDIATE) {
            handleTestingImmediateUpdate(manager, info)
        } else if (APP_UPDATE_TYPE_SUPPORTED == AppUpdateType.FLEXIBLE) {
            handleTestingFlexibleUpdate(manager, info)
        }
    }

    private fun handleTestingImmediateUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        if ((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
            info.result.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
            manager.startUpdateFlowForResult(
                info.result,
                AppUpdateType.IMMEDIATE,
                this,
                REQUEST_UPDATE
            )
        }

        val fakeAppUpdate = manager as FakeAppUpdateManager
        if (fakeAppUpdate.isImmediateFlowVisible) {
            fakeAppUpdate.userAcceptsUpdate()
            fakeAppUpdate.downloadStarts()
            fakeAppUpdate.downloadCompletes()
            launchRestart(manager)
        }
    }

    private fun handleTestingFlexibleUpdate(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        if ((info.result.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE ||
                    info.result.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) &&
            info.result.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
            binding.layoutUpdate.show()
            setTestingUpdateAction(manager, info)
        }
    }

    private fun setTestingUpdateAction(manager: AppUpdateManager, info: Task<AppUpdateInfo>) {
        binding.layoutUpdate.btn_update.setOnClickListener {
            updateListener = InstallStateUpdatedListener {
                binding.layoutUpdate.btn_update.show(false)
                binding.layoutUpdate.status.show()
                when (it.installStatus()) {
                    InstallStatus.FAILED, InstallStatus.UNKNOWN -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_failed)
                        binding.layoutUpdate.btn_update.show()
                    }
                    InstallStatus.PENDING -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_pending)
                    }
                    InstallStatus.CANCELED -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_canceled)
                    }
                    InstallStatus.DOWNLOADING -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_downloading)
                    }
                    InstallStatus.DOWNLOADED -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_downloaded)
                        launchRestart(manager)
                    }
                    InstallStatus.INSTALLING -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_installing)
                    }
                    InstallStatus.INSTALLED -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_installed)
                        manager.unregisterListener(updateListener)
                    }
                    else -> {
                        binding.layoutUpdate.status.text = getString(R.string.info_restart)
                    }
                }
            }
            manager.registerListener(updateListener)
            manager.startUpdateFlowForResult(
                info.result,
                AppUpdateType.FLEXIBLE,
                this,
                REQUEST_UPDATE
            )

            val fakeAppUpdate = manager as FakeAppUpdateManager
            if (fakeAppUpdate.isConfirmationDialogVisible) {
                fakeAppUpdate.userAcceptsUpdate()
                fakeAppUpdate.downloadStarts()
                fakeAppUpdate.downloadCompletes()
                fakeAppUpdate.completeUpdate()
                fakeAppUpdate.installCompletes()
            }
        }
    }
    /* End In-App update */

//    private fun autoscroll(scroll:Boolean) {
//        tag("autoscroll $scroll")
//        Timer().schedule(object : TimerTask() {
//            // task to be scheduled
//            override fun run() {
//                val adt = binding.viewpagerBanner.adapter as BannerAdapter
//                tag("autoscroll adt.count: ${adt.count}")
//                var currentPage = binding.viewpagerBanner.currentItem
//                if(currentPage == adt.count) {
//                    currentPage = 0
//                }
//                binding.viewpagerBanner.currentItem = currentPage++
//                currentPage++
//                tag("autoscroll currentPage: $currentPage")
//            }
//        }, 3500, 3500)
//    }
}