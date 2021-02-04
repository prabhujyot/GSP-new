package `in`.allen.gsp.utils

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.NetworkConnectionInterceptor
import `in`.allen.gsp.data.network.YTApi
import `in`.allen.gsp.data.repositories.*
import `in`.allen.gsp.data.services.MusicService
import `in`.allen.gsp.ui.message.NotificationViewModelFactory
import `in`.allen.gsp.ui.quiz.ContestViewModelFactory
import `in`.allen.gsp.ui.quiz.QuizViewModelFactory
import `in`.allen.gsp.ui.reward.RewardViewModelFactory
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.multidex.MultiDex
import com.google.android.gms.security.ProviderInstaller
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton

class App: Application(), KodeinAware {
    private val TAG = App::class.java

    override val kodein = Kodein.lazy {
        import(androidXModule(this@App))

        bind() from singleton { NetworkConnectionInterceptor(instance()) }
        bind() from singleton { Api(instance()) }
        bind() from singleton { YTApi(instance()) }
        bind() from singleton { AppDatabase(instance()) }
        bind() from singleton { AppPreferences(instance()) }
        bind() from singleton { UserRepository(instance(), instance()) }
        bind() from singleton { MessageRepository(instance()) }
        bind() from singleton { BannerRepository(instance(), instance()) }
        bind() from singleton { LeaderboardRepository(instance(), instance(), instance()) }
        bind() from singleton { VideosRepository(instance(), instance(), instance(), instance()) }
        bind() from singleton { RewardRepository(instance()) }
        bind() from provider { RewardViewModelFactory(instance(), instance()) }
        bind() from provider { NotificationViewModelFactory(instance(), instance()) }
        bind() from singleton { QuizRepository(instance(), instance()) }
        bind() from provider { QuizViewModelFactory(instance(), instance(), instance(), instance()) }
        bind() from provider { ContestViewModelFactory(instance(), instance(), instance()) }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Networking Library
        if (Build.VERSION.SDK_INT == 19) {
            try {
                ProviderInstaller.installIfNeeded(this)
            } catch (ignored: Exception) {}
        }

        bindMusicService()
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
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

    public fun getmServ(): MusicService? {
        return mServ
    }

}