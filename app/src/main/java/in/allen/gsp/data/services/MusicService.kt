package `in`.allen.gsp.data.services

import `in`.allen.gsp.R
import `in`.allen.gsp.utils.AppPreferences
import `in`.allen.gsp.utils.tag
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class MusicService : Service(), KodeinAware, MediaPlayer.OnErrorListener {

    private val TAG = MusicService::class.java.name
    private val mBinder: IBinder = ServiceBinder()
    private var mediaPlayer: MediaPlayer? = null
    private val appPreferences: AppPreferences by instance()

    override val kodein by kodein()

    override fun onBind(p0: Intent?): IBinder {
        return mBinder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        return false
    }

    override fun onError(p0: MediaPlayer?, p1: Int, p2: Int): Boolean {
        tag("$TAG, music player failed")
        stopMusic()
        return false
    }

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayer()
        mediaPlayer!!.setOnErrorListener { _, _, _ -> true }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMusic()
    }

    fun playMusic(key: String, loop: Boolean) {
        tag("$TAG, Play: $key, loop: $loop")
        if (mediaPlayer != null) {
//            if (!loop) {
                pause()
                mediaPlayer!!.reset()
//            }
            when (key) {
                "fifty" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.fifty)
//                "round" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.round)
//                "google" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.google)
                "image" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.image)
                "correct" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.correct)
                "incorrect" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.incorrect)
                "lifeline" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.lifeline)
//                "question" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.question)
                "quit" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.quit)
//                "rules" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.rules)
//                "think_7" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.think_7)
                "think_8" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.think_8)
                "think_9" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.think_9)
//                "think_10" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.think_10)
//                "timer" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.timer)
                "timer_2" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.timer_2)
//                "times_up" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.times_up)
                "times_up_2" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.times_up_2)
//                "win" -> mediaPlayer = MediaPlayer.create(this@MusicService, R.raw.win)
            }

            volume(0.8f)
            play(0,loop)

            // is user already off the music
            if(appPreferences.appMusic) {
                isMute(false)
            } else {
                isMute(true)
            }

        } else {
            mediaPlayer = MediaPlayer()
            playMusic(key,loop)
        }
    }

    fun stopMusic() {
        tag("$TAG, stopMusic")
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.reset() //It requires again setDataSource for player object.
                mediaPlayer!!.release()
            } finally {
                mediaPlayer = null
            }
        }
    }

    private fun volume(vol: Float) {
        tag("$TAG, volume: $vol")
        if (mediaPlayer != null) {
            mediaPlayer!!.setVolume(vol, vol)
        }
    }

    fun pause(): Int {
        tag("$TAG, pause")
        var length = 0
        if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
            mediaPlayer!!.pause()
            length = mediaPlayer!!.currentPosition
        }
        return length
    }

    private fun play(seekTo: Int, loop: Boolean) {
        tag("$TAG, resume: $seekTo")
        if (mediaPlayer != null && !mediaPlayer!!.isPlaying) {
            mediaPlayer!!.seekTo(seekTo)
            mediaPlayer!!.start()
            //new code
            mediaPlayer!!.isLooping = loop
        }
    }

    private fun isMute(mute: Boolean): Boolean {
        tag("$TAG, isMute: $mute")
        return if (mediaPlayer != null) {
            if (mute) {
                mediaPlayer!!.setVolume(0f, 0f)
                true
            } else {
                mediaPlayer!!.setVolume(1f, 1f)
                false
            }
        } else false
    }

    inner class ServiceBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        val service: MusicService
            get() {
                return this@MusicService
            }
    }

}