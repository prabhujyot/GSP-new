package `in`.allen.gsp.ui.home

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import `in`.allen.gsp.SettingsActivity
import `in`.allen.gsp.WebActivity
import `in`.allen.gsp.data.entities.Banner
import `in`.allen.gsp.data.entities.Leaderboard
import `in`.allen.gsp.data.entities.Tile
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.MessageRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityHomeBinding
import `in`.allen.gsp.databinding.ItemTileBinding
import `in`.allen.gsp.ui.leaderboard.LeaderboardActivity
import `in`.allen.gsp.ui.message.NotificationActivity
import `in`.allen.gsp.ui.profile.ProfileActivity
import `in`.allen.gsp.ui.quiz.QuizActivity
import `in`.allen.gsp.ui.reward.RewardActivity
import `in`.allen.gsp.ui.videos.VideosActivity
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.asksira.loopingviewpager.LoopingPagerAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.util.concurrent.TimeUnit


private const val REQUEST_UPDATE = 100
private const val APP_UPDATE_TYPE_SUPPORTED = AppUpdateType.FLEXIBLE

class HomeActivity : AppCompatActivity(), DIAware {

    private val TAG = HomeActivity::class.java.name
    private lateinit var binding: ActivityHomeBinding
    private lateinit var viewModel: HomeViewModel

    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val factory:HomeViewModelFactory by instance()
    private val userRepository: UserRepository by instance()
    private val messageRepository: MessageRepository by instance()
    private val preferences: AppPreferences by instance()

    private lateinit var app: App

    private var autoscroll = "true"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_home)
        viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]

        val toolbar = binding.root.findViewById<Toolbar>(R.id.myToolbar)

        setSupportActionBar(toolbar)
        toolbar.findViewById<ImageButton>(R.id.btnClose).setOnClickListener {
            onBackPressed()
        }

        toolbar.findViewById<ImageButton>(R.id.menuNotification).setOnClickListener {
            val i = Intent()
            i.setClass(this@HomeActivity, NotificationActivity::class.java)
            startActivity(i)
        }

        binding.viewpagerBanner.pageMargin = 8


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

        userRepository.userLife.observe(this) {
            if (it != null) {
                val life = binding.root.findViewById<TextView>(R.id.life)
                life.text = "${it["life"]}"
                if (System.currentTimeMillis() < preferences.timestampLife) {
                    life.text = getString(R.string.infinity)
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
        }

        messageRepository.unreadMsg.observe(this) {
            val msgCounter = toolbar.findViewById<TextView>(R.id.msgCounter)
            msgCounter.show(false)
            if (it > 0) {
                if (it < 10) {
                    msgCounter.text = it.toString()
                } else {
                    msgCounter.text = "9+"
                }
                msgCounter.show()
            }
        }
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
        if(!autoscroll.equals("true",true)) {
            binding.viewpagerBanner.pauseAutoScroll()
        } else {
            binding.viewpagerBanner.resumeAutoScroll()
        }
    }

    override fun onBackPressed() {
        confirmDialog("Exit!", "Tap on Yes to exit.", {
            super.onBackPressed()
        }, {})
    }

    @Deprecated("Deprecated in Java")
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
        viewModel.getLoading().observe(this) {
            tag("$TAG _loading: ${it.message}")
            binding.rootLayout.hideProgress()
            if (it.data is Boolean && it.data) {
                binding.rootLayout.showProgress()
            }
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
                            binding.fabProfile.loadImage(user.avatar, true, true)
                        }
                        viewModel.bannerData(user.user_id)
                        viewModel.leaderboard()
                    }

                    "banner" -> {
                        tag("banner data")
                        if (it.data is Deferred<*>) {
                            val deferredList = it.data as Deferred<LiveData<List<Banner>>>
                            lifecycleScope.launch {
                                deferredList.await().observe(this@HomeActivity) { list ->
                                    binding.viewpagerBanner.adapter = BannerAdapter(
                                        list,
                                        true
                                    )

                                    viewModel.tileData()

                                    tag("autoscroll: $autoscroll")
                                    if (!autoscroll.equals("true", true)) {
                                        binding.viewpagerBanner.pauseAutoScroll()
                                    } else {
                                        binding.viewpagerBanner.resumeAutoScroll()
                                    }
                                }
                            }
                        }
                    }

                    "tiles" -> {
                        tag("$TAG, tiles")
                        if (it.data is List<*>) {
                            val list = it.data as List<Tile>
                            if (list.isNotEmpty()) {
                                setTiles(list)
                            }
                        }
                    }

                    "leaderboard" -> {
                        if (it.data is Deferred<*>) {
                            val deferredList = it.data as Deferred<LiveData<List<Leaderboard>>>
                            lifecycleScope.launch {
                                deferredList.await().observe(this@HomeActivity) { list ->
                                    if (list.size > 4) {
                                        setLeaderboard(list)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    class BannerAdapter(
        itemList: List<Banner>,
        isInfinite: Boolean
    ) : LoopingPagerAdapter<Banner>(itemList, isInfinite) {

        //This method will be triggered if the item View has not been inflated before.
        override fun inflateView(
            viewType: Int,
            container: ViewGroup,
            listPosition: Int
        ): View {
            return LayoutInflater.from(container.context).inflate(R.layout.item_banner, container, false)
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
                    try {
                        val i = Intent()
                        i.setClassName(convertView.context, item.action)
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
                        convertView.context.startActivity(i)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    private fun setLeaderboard(list: List<Leaderboard>) {
        val listTop = list.subList(0,3)

        // rank 1
        binding.layoutRanks.findViewById<TextView>(R.id.rank_first_name).text = listTop[0].name
        binding.layoutRanks.findViewById<TextView>(R.id.rank_first_score).text = "S. ${listTop[0].score}"
        listTop[0].avatar.let { it1 -> binding.layoutRanks.findViewById<FloatingActionButton>(R.id.rank_first_avatar).loadImage(it1,true) }

        // rank 2
        binding.layoutRanks.findViewById<TextView>(R.id.rank_second_name).text = listTop[1].name
        binding.layoutRanks.findViewById<TextView>(R.id.rank_second_score).text = "S. ${listTop[1].score}"
        listTop[1].avatar.let { it1 -> binding.layoutRanks.findViewById<FloatingActionButton>(R.id.rank_second_avatar).loadImage(it1,true) }

        // rank 3
        binding.layoutRanks.findViewById<TextView>(R.id.rank_third_name).text = listTop[2].name
        binding.layoutRanks.findViewById<TextView>(R.id.rank_third_score).text = "S. ${listTop[2].score}"
        listTop[2].avatar.let { it1 -> binding.layoutRanks.findViewById<FloatingActionButton>(R.id.rank_third_avatar).loadImage(it1,true) }

        binding.layoutRanks.show()
    }

    private fun setTiles(list: List<Tile>) {
        tag("$TAG, setTiles : ${list.size}")
        binding.hScrollContainer.removeAllViews()
        for (item in list) {
            val bindingTiles: ItemTileBinding = DataBindingUtil.inflate(
                layoutInflater, R.layout.item_tile, binding.hScrollContainer, false
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                bindingTiles.title.text = Html.fromHtml(item.text, Html.FROM_HTML_MODE_COMPACT)
            } else {
                bindingTiles.title.text = Html.fromHtml(item.text)
            }
            bindingTiles.image.loadImage(
                "${BuildConfig.BASE_URL}gsp-admin/uploads/tiles/${item.image}",
                false,
                centerInside = true,
            )
            binding.hScrollContainer.addView(bindingTiles.root)
        }
    }

    fun btnActionHome(view: View) {
        val i = Intent()
        when (view.id) {
            R.id.layoutVideo -> {
                i.setClass(this@HomeActivity, VideosActivity::class.java)
            }
            R.id.layoutLeaderboard -> {
                i.setClass(this@HomeActivity, LeaderboardActivity::class.java)
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
            R.id.btnSetting -> {
                i.setClass(this@HomeActivity, SettingsActivity::class.java)
                i.putExtra("is_admin", viewModel.admin)
            }
            R.id.btnContests -> {
                i.setClass(this@HomeActivity, WebActivity::class.java)
                i.putExtra("url", "${BuildConfig.BASE_URL}quizzes-and-games/")
            }
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
        val btn_update: Button = binding.root.findViewById(R.id.btn_update)
        val status: TextView = binding.root.findViewById(R.id.status)
        btn_update.setOnClickListener {
            updateListener = InstallStateUpdatedListener {
                btn_update.show(false)
                status.show()
                when (it.installStatus()) {
                    InstallStatus.FAILED, InstallStatus.UNKNOWN -> {
                        status.text = getString(R.string.info_failed)
                        btn_update.visibility = View.VISIBLE
                    }
                    InstallStatus.PENDING -> {
                        status.text = getString(R.string.info_pending)
                    }
                    InstallStatus.CANCELED -> {
                        status.text = getString(R.string.info_canceled)
                    }
                    InstallStatus.DOWNLOADING -> {
                        status.text = getString(R.string.info_downloading)
                    }
                    InstallStatus.DOWNLOADED -> {
                        status.text = getString(R.string.info_installing)
                        launchRestart(manager)
                    }
                    InstallStatus.INSTALLING -> {
                        status.text = getString(R.string.info_installing)
                    }
                    InstallStatus.INSTALLED -> {
                        status.text = getString(R.string.info_installed)
                        manager.unregisterListener(updateListener)
                    }
                    else -> {
                        status.text = getString(R.string.info_restart)
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
        appUpdateManager.setUpdateAvailable(14)

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
        val btn_update: Button = binding.root.findViewById(R.id.btn_update)
        val status: TextView = binding.root.findViewById(R.id.status)

        btn_update.setOnClickListener {
            updateListener = InstallStateUpdatedListener {
                btn_update.show(false)
                status.show()
                when (it.installStatus()) {
                    InstallStatus.FAILED, InstallStatus.UNKNOWN -> {
                        status.text = getString(R.string.info_failed)
                        btn_update.show()
                    }
                    InstallStatus.PENDING -> {
                        status.text = getString(R.string.info_pending)
                    }
                    InstallStatus.CANCELED -> {
                        status.text = getString(R.string.info_canceled)
                    }
                    InstallStatus.DOWNLOADING -> {
                        status.text = getString(R.string.info_downloading)
                    }
                    InstallStatus.DOWNLOADED -> {
                        status.text = getString(R.string.info_downloaded)
                        launchRestart(manager)
                    }
                    InstallStatus.INSTALLING -> {
                        status.text = getString(R.string.info_installing)
                    }
                    InstallStatus.INSTALLED -> {
                        status.text = getString(R.string.info_installed)
                        manager.unregisterListener(updateListener)
                    }
                    else -> {
                        status.text = getString(R.string.info_restart)
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
}