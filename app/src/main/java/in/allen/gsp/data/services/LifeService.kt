package `in`.allen.gsp.data.services

import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.tag
import android.app.Service
import android.content.Intent
import android.os.IBinder
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

import java.util.*


class LifeService : Service(), DIAware {

//    override val di: DI by subDI(di) {}
    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val userRepository: UserRepository by instance()

    private lateinit var timer: Timer
    private lateinit var taskRegular: TimerTask
    private lateinit var taskInfinite: TimerTask
    private val data = HashMap<String, Long>()
    var memintent = Intent()

    var taskRegularRunning = false
    var taskInfiniteRunning = false


    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    private fun setData(life:Long, remainingMilis:Long) {
        data["life"] = life
        data["remaining"] = remainingMilis
    }

    private fun getData(): HashMap<String,Long> {
        return data
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        tag("life service started.. ")
        if (intent != null) {
            memintent = intent

            val bundle = intent.getBundleExtra("bundle")
            val user = bundle?.getParcelable<User>("user")
            val timestampLife = bundle?.getLong("timestampLife")

            tag("life service started.. System.currentTimeMillis(): ${System.currentTimeMillis()} , timestampLife: $timestampLife")

            if(!::timer.isInitialized) {
                timer = Timer()
            }

            setData(user?.life!!.toLong(),0)

            if(System.currentTimeMillis() > timestampLife!!) {
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

//                    interval = 2
//                    maxLife = 5

                    // check life
                    tag("service user: life: ${user.life} ::timer.isInitialized: ${::timer.isInitialized}")

                    if(taskInfiniteRunning) {
                        taskInfinite.cancel()
                        taskInfiniteRunning = false
                    }

                    if(!taskRegularRunning) {
                        taskRegular = taskRegular(user,interval,maxLife)
                        timer.schedule(taskRegular, 0, 1000)
                        taskRegularRunning = true
                    }
                }
            } else {
                tag("service infinity life")
                if(taskRegularRunning) {
                    taskRegular.cancel()
                    taskRegularRunning = false
                }

                if(!taskInfiniteRunning) {
                    taskInfinite = taskInfinite(timestampLife)
                    timer.schedule(taskInfinite, 0, 1000)
                    taskInfiniteRunning = true
                }
            }
        }
        return START_STICKY
    }

    private fun taskInfinite(timestampLife: Long): TimerTask {
        return object : TimerTask() {
            override fun run() {
                tag("service infinity timer")
                val remainingMilis = timestampLife.minus(System.currentTimeMillis())
                setData(5, remainingMilis)
                userRepository.userLife.postValue(getData())

                if(System.currentTimeMillis() > timestampLife) {
                    cancel()
                }
            }
        }
    }

    private fun taskRegular(user: User,interval: Int,maxLife: Int): TimerTask {
        var remainingMilis: Long
        val intervalMilis = interval.times(60).times(1000)
        return object : TimerTask() {
            override fun run() {
                tag("service taskRegular started")
                if (System.currentTimeMillis() >= user.update_at) {
                    val diff: Long = System.currentTimeMillis() - user.update_at
                    remainingMilis = intervalMilis.minus(diff)

                    val life = getData()["life"]
                    if (life != null) {
                        user.life = life.toInt()
                    }

                    setData(user.life.toLong(),remainingMilis)
                    tag("service taskRegular: diff: $diff (user.life: ${user.life}")

                    if (diff >= interval.times(maxLife).times(60).times(1000)) {
                        user.life = maxLife
                        user.update_at = System.currentTimeMillis()

                        Coroutines.io {
                            userRepository.setDBUser(user)
                        }
                        setData(user.life.toLong(),remainingMilis)
                    } else {
                        val l = diff/intervalMilis

                        if(l > 0) {
                            user.life = user.life.plus(1)
                            user.update_at = System.currentTimeMillis()
                            if (user.life > maxLife) {
                                user.life = maxLife
                            }

                            Coroutines.io {
                                userRepository.setDBUser(user)
                            }
                            setData(user.life.toLong(),remainingMilis)
                        }
                    }
                }

                if(user.life >= maxLife) {
                    tag("service taskRegular cancelled life full")
                    cancel()
                    stopSelf()
                }

                userRepository.userLife.postValue(getData())
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
//        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        val restartServiceIntent = memintent
        restartServiceIntent.setPackage(packageName)
        startService(restartServiceIntent)
        super.onTaskRemoved(rootIntent)
    }
}