package `in`.allen.gsp.data.services

import `in`.allen.gsp.ui.message.NotificationActivity
import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Message
import `in`.allen.gsp.data.repositories.MessageRepository
import `in`.allen.gsp.utils.AppPreferences
import `in`.allen.gsp.utils.Coroutines
import `in`.allen.gsp.utils.milisToFormat
import `in`.allen.gsp.utils.tag
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.*

class MyFirebaseMessagingService: FirebaseMessagingService(), KodeinAware {

    override val kodein by kodein()
    private val preferences: AppPreferences by instance()
    private val messageRepository: MessageRepository by instance()

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        tag("$TAG From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            tag("$TAG Message data payload: ${remoteMessage.data}")

            val key = remoteMessage.data["key"]
            if(key.equals("gsp_notification",true)) {
                sendNotification(remoteMessage.data["title"]!!,remoteMessage.data["body"]!!)
            }
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            tag("$TAG Message Notification Body: ${it.body}")
            if(it.title != null && it.body != null) {
                sendNotification(it.title!!, it.body!!)
            }
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        tag("$TAG Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        preferences.firebaseToken = token
    }
    // [END on_new_token]

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageTitle: String, messageBody: String) {
        var notificationId = 0
        Coroutines.io {
            val m = messageRepository.getLastItem()
            notificationId = m.id.plus(1)

            val message = Message(
                notificationId,
                messageTitle,
                messageBody,
                milisToFormat(Calendar.getInstance().timeInMillis, "yyyy-MM-dd HH:mm:ss"),
                0)
            messageRepository.setItem(message)

            val intent = Intent(this, NotificationActivity::class.java)
            intent.putExtra("title",messageTitle)
            intent.putExtra("body",messageBody)
            intent.putExtra("notificationId",notificationId)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
                PendingIntent.FLAG_ONE_SHOT)

            val channelId = getString(R.string.default_notification_channel_id)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Since android Oreo notification channel is needed.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(channelId,
                    "GSP Notification Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }

    companion object {
        private const val TAG = "FirebaseMessagingService"
    }

}