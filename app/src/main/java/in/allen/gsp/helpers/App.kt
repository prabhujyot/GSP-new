package `in`.allen.gsp.helpers

import `in`.allen.gsp.BuildConfig
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
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.multidex.MultiDex
import com.google.android.gms.security.ProviderInstaller
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class App: Application() {

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

    fun writeStringToFile(data: String, fileName: String){
        val file = File(getExternalFilesDir(null), fileName)
        Log.d(tag,"Writing to : $file")
        file.writeText(data)
    }

    fun readStringFromFile(fileName: String): String {
        var content = ""
        val file = File(getExternalFilesDir(null), fileName)
        if(file.exists()) {
            content = file.readText()
            Log.d(tag,"Reading from : $file")
        }
        return content
    }

    fun drawaleGradiantColor(drawableId: Int, colorList: IntArray?): GradientDrawable {
        val background = ResourcesCompat.getDrawable(resources, drawableId,null) as GradientDrawable
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

    fun shareLink(context: Context, url: String, extra:String) {
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
                PackageManager.GET_SIGNATURES)
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
            .setAndroidParameters(DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).build())
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