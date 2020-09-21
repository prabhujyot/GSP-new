package `in`.allen.gsp.utils

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import `in`.allen.gsp.data.db.AppDatabase
import `in`.allen.gsp.data.network.Api
import `in`.allen.gsp.data.network.NetworkConnectionInterceptor
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.ui.splash.SplashViewModelFactory
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.multidex.MultiDex
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.messaging.FirebaseMessaging
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.androidXModule
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.provider
import org.kodein.di.generic.singleton
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

class App: Application(), KodeinAware {

    override val kodein = Kodein.lazy {
        import(androidXModule(this@App))

        bind() from singleton { NetworkConnectionInterceptor(instance()) }
        bind() from singleton { Api(instance()) }
        bind() from singleton { AppDatabase(instance()) }
        bind() from singleton { UserRepository(instance(), instance()) }
        bind() from provider { SplashViewModelFactory(instance(), initGoogle()) }
    }

    private fun initGoogle(): GoogleSignInClient {
        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private val tag = App::class.java.name

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

    fun milisToFormat(milliseconds: Long, format: String): String {
        val sdf = SimpleDateFormat(format)
        val resultdate = Date(milliseconds)
        return sdf.format(resultdate)
    }

    fun timeInAgo(dateTime: String?, format: String): String {
        var ago: String
        val sdf = SimpleDateFormat(format)
        //sdf.timeZone = TimeZone.getTimeZone("GMT")

        val time = sdf.parse(dateTime).time
        val now = System.currentTimeMillis()
        ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS) as String
        return ago
    }

    fun writeStringToFile(data: String, fileName: String){
        val file = File(getExternalFilesDir(null), fileName)
        Log.d(tag, "Writing to : $file")
        file.writeText(data)
    }

    fun readStringFromFile(fileName: String): String {
        var content = ""
        val file = File(getExternalFilesDir(null), fileName)
        if(file.exists()) {
            content = file.readText()
            Log.d(tag, "Reading from : $file")
        }
        return content
    }

    fun drawaleGradiantColor(drawableId: Int, colorList: IntArray?): GradientDrawable {
        val background = ResourcesCompat.getDrawable(resources, drawableId, null) as GradientDrawable
        background.mutate()
        background.colors = colorList
        return background
    }

    fun screenShot(view: View, fileName: String): File {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgDrawable = view.background
        if (bgDrawable!=null)
            bgDrawable.draw(canvas)
        else
            canvas.drawColor(Color.WHITE)
        view.draw(canvas)
//		return bitmap

        val file = File(getExternalFilesDir(null), fileName)
        val stream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()
        return file
    }

    fun shareLink(context: Context, url: String, extra: String) {
        val sendIntent = Intent()
        sendIntent.action = Intent.ACTION_SEND;
        sendIntent.putExtra(Intent.EXTRA_TEXT, "$extra $url")
        sendIntent.type = "text/plain"
        context.startActivity(Intent.createChooser(sendIntent, "Select"))
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun printKeyHash() {
        // Add code to print out the key hash
        try {
            val info = packageManager.getPackageInfo(
                BuildConfig.APPLICATION_ID,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                Log.d("KeyHash: ", Base64.encodeToString(md.digest(), Base64.DEFAULT))
            }
        } catch (e: PackageManager.NameNotFoundException) {
        } catch (e: NoSuchAlgorithmException) {
        }
    }

    fun getReferralLink() {
        val dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLink(Uri.parse("https://gsp.allen.in?referral_id=" + "preferences.userID"))
            .setDomainUriPrefix("https://gsp.page.link") // Open links with this app on Android
            .setAndroidParameters(
                DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).build()
            )
            .buildDynamicLink()
        FirebaseDynamicLinks.getInstance().createDynamicLink()
            .setLongLink(Uri.parse(dynamicLink.uri.toString()))
            .buildShortDynamicLink()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val shortLink = task.result!!.shortLink
                    val flowchartLink = task.result!!.previewLink
                }
            }
    }

    fun subscribeFirebaseTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener {
            val msg = it.isSuccessful
            Log.d("$topic Subscription: ", msg.toString())
        }
    }

}