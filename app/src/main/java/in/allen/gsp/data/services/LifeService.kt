package `in`.allen.gsp.data.services

import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.tag
import `in`.allen.gsp.utils.toast
import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap


class LifeService : Service(), KodeinAware {

    override val kodein by kodein()
    private val userRepository: UserRepository by instance()
    private lateinit var timer: Timer
    var memintent = Intent()


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tag("service started.. ")
        if (intent != null) {
            memintent = intent
            Coroutines.io {
                var interval = 30
                var maxLife = 0

                val maxChance = userRepository.config("max-game-chance")
                val intervalSession = userRepository.config("game-session-interval")
                if(intervalSession.isNotEmpty() && maxChance.isNotEmpty() && !maxChance.equals(
                        "0",
                        true
                    )) {
                    maxLife = maxChance.toInt()
                    interval = intervalSession.toInt().div(maxLife)
                }

                val user = userRepository.getDBUser()
                if(user != null && !::timer.isInitialized) {
                    // check life
                    var remaining = 0L
                    timer = Timer()
                    timer.schedule(object : TimerTask() {
                        override fun run() {
                            tag("service timer")
                            if (System.currentTimeMillis() >= user.update_at) {
                                val diff: Long = System.currentTimeMillis() - user.update_at
                                remaining = interval.times(60).times(1000).minus(diff)

                                if (diff >= interval.times(maxLife).times(60).times(1000)) {
                                    user.life = maxLife
                                    user.update_at = System.currentTimeMillis()

                                    Coroutines.io {
                                        userRepository.setDBUser(user)
                                    }
                                    tag("timer cancelled")
                                    timer.cancel()

                                    val data = HashMap<String,Long>()
                                    data["life"] = user.life.toLong()
                                    data["remaining"] = remaining
                                    userRepository.userLife.postValue(data)

                                    stopSelf()
                                } else {
                                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                                    val l = minutes/interval
//                                    remaining = minutes % interval


                                    tag("service timer running $minutes $l $remaining")
                                    user.life = user.life + l.toInt()
                                    if (user.life > maxLife) {
                                        user.life = maxLife
                                    }
                                    user.update_at = System.currentTimeMillis()

//                                    Coroutines.io {
//                                        userRepository.setDBUser(user)
//                                    }

                                    val data = HashMap<String,Long>()
                                    data["life"] = user.life.toLong()
                                    data["remaining"] = remaining
                                    userRepository.userLife.postValue(data)
                                }
                            }
                        }
                    }, 0, 1000)
                }
                tag("service started ${user?.life} CurrentTime: ${System.currentTimeMillis()} >= ${user?.update_at}")
            }
        }
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
//        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        val restartServiceIntent = memintent
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}