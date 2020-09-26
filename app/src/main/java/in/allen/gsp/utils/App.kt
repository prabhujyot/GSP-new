package `in`.allen.gsp.utils

import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.NetworkConnectionInterceptor
import `in`.allen.gsp.data.network.YTApi
import `in`.allen.gsp.data.repositories.LeaderboardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.repositories.VideosRepository
import `in`.allen.gsp.ui.leaderboard.LeaderboardViewModelFactory
import `in`.allen.gsp.ui.splash.SplashViewModelFactory
import `in`.allen.gsp.ui.videos.VideosViewModelFactory
import android.app.Application
import android.content.Context
import android.os.Build
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

    override val kodein = Kodein.lazy {
        import(androidXModule(this@App))

        bind() from singleton { NetworkConnectionInterceptor(instance()) }
        bind() from singleton { Api(instance()) }
        bind() from singleton { AppDatabase(instance()) }
        bind() from singleton { UserRepository(instance(),instance()) }
        bind() from provider { SplashViewModelFactory(instance()) }

        bind() from singleton { LeaderboardRepository(instance(),instance()) }
        bind() from provider { LeaderboardViewModelFactory(instance()) }

        bind() from singleton { YTApi(instance()) }
        bind() from singleton { VideosRepository(instance(),instance()) }
        bind() from provider { VideosViewModelFactory(instance()) }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Networking Library
        if (Build.VERSION.SDK_INT == 19) {
            try {
                ProviderInstaller.installIfNeeded(this)
            } catch (ignored: Exception) {}
        }
    }

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

}