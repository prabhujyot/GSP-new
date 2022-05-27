package `in`.allen.gsp.utils

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.NetworkConnectionInterceptor
import `in`.allen.gsp.data.network.YTApi
import `in`.allen.gsp.data.repositories.*
import `in`.allen.gsp.data.services.MusicService
import `in`.allen.gsp.ui.home.HomeViewModelFactory
import `in`.allen.gsp.ui.message.NotificationViewModelFactory
import `in`.allen.gsp.ui.quiz.ContestViewModelFactory
import `in`.allen.gsp.ui.quiz.QuizViewModelFactory
import `in`.allen.gsp.ui.reward.RewardViewModelFactory
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import com.downloader.PRDownloader
import com.google.android.gms.security.ProviderInstaller
import org.kodein.di.*
import org.kodein.di.android.x.androidXModule


class App: Application(), DIAware {
    private val TAG = App::class.java

    override val di by DI.lazy {
        import(androidXModule(this@App))

        bind<NetworkConnectionInterceptor>() with singleton { NetworkConnectionInterceptor(instance()) }
        bind<Api>() with singleton { Api(instance()) }
        bind<YTApi>() with singleton { YTApi(instance()) }
        bind<AppDatabase>() with singleton { AppDatabase(instance()) }
        bind<AppPreferences>() with singleton { AppPreferences(instance()) }
        bind<UserRepository>() with singleton { UserRepository(instance(), instance()) }
        bind<MessageRepository>() with singleton { MessageRepository(instance()) }
        bind<BannerRepository>() with singleton { BannerRepository(instance(), instance()) }
        bind<LeaderboardRepository>() with singleton { LeaderboardRepository(instance(), instance(), instance()) }
        bind<VideosRepository>() with singleton { VideosRepository(instance(), instance(), instance(), instance()) }
        bind<RewardRepository>() with singleton { RewardRepository(instance()) }
        bind<RewardViewModelFactory>() with provider { RewardViewModelFactory(instance(), instance()) }
        bind<HomeViewModelFactory>() with provider { HomeViewModelFactory(instance(), instance(), instance(), instance()) }
        bind<NotificationViewModelFactory>() with provider { NotificationViewModelFactory(instance(), instance()) }
        bind<QuizRepository>() with singleton { QuizRepository(instance(), instance()) }
        bind<QuizViewModelFactory>() with provider { QuizViewModelFactory(instance(), instance(), instance(), instance()) }
        bind<ContestViewModelFactory>() with provider { ContestViewModelFactory(instance(), instance(), instance()) }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Networking Library
        if (Build.VERSION.SDK_INT == 19) {
            try {
                ProviderInstaller.installIfNeeded(this)
            } catch (ignored: Exception) {}
        }

        PRDownloader.initialize(applicationContext)
    }


    // Music Service
    private var mIsBound: Boolean = false
    private var mServ: MusicService? = null

    private var Scon: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            mServ = (binder as MusicService.ServiceBinder).service
            tag("$TAG, mServ: $mServ")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mServ = null
        }
    }

    fun bindMusicService() {
        if (!mIsBound) {
            val ms = Intent(this, MusicService::class.java)
            startService(ms)
            bindService(
                Intent(this, MusicService::class.java),
                Scon,
                BIND_AUTO_CREATE
            )
            mIsBound = true
        }
    }

    fun unbindMusicService() {
        if (mIsBound) {
            unbindService(Scon)
            getmServ()?.stopMusic()
            stopService(Intent(this, MusicService::class.java))
            mIsBound = false
        }
    }

    fun getmServ(): MusicService? {
        return mServ
    }

}