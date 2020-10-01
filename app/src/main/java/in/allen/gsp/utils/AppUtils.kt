package `in`.allen.gsp.utils

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Layout
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

fun Context.toast(message: String) {
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}

fun View.snackbar(message: String) {
    Snackbar.make(this,message,Snackbar.LENGTH_INDEFINITE).also { snackbar ->
        snackbar.setAction("Ok") {
            snackbar.dismiss()
        }
    }.show()
}

fun tag(data: Any) {
    Log.e("GSP","$data")
}

fun View.showProgress() {
    this.findViewById<ConstraintLayout>(R.id.layoutProgress).visibility = View.VISIBLE
}

fun View.hideProgress() {
    this.findViewById<ConstraintLayout>(R.id.layoutProgress).visibility = View.GONE
}

fun View.show(visibilty: Boolean = true) {
    if(visibilty) {
        this.visibility = View.VISIBLE
    } else {
        this.visibility = View.GONE
    }
}

fun ImageView.loadImage(url: String, circular: Boolean = false) {
    if(circular) {
        Glide.with(this)
            .load(url)
            .circleCrop()
            .into(this)
    } else {
        Glide.with(this)
            .load(url)
            .centerCrop()
            .into(this)
    }
}

fun milisToFormat(milliseconds: Long, format: String): String {
    val sdf = SimpleDateFormat(format)
    val resultdate = Date(milliseconds)
    return sdf.format(resultdate)
}

fun timeInAgo(dateTime: String?, format: String): String {
    val ago: String
    val sdf = SimpleDateFormat(format)
    //sdf.timeZone = TimeZone.getTimeZone("GMT")

    val time = sdf.parse(dateTime).time
    val now = System.currentTimeMillis()
    ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS) as String
    return ago
}

fun Context.writeStringToFile(data: String, fileName: String){
    val file = File(getExternalFilesDir(null), fileName)
    tag("Writing to : $file")
    file.writeText(data)
}

fun Context.readStringFromFile(fileName: String): String {
    var content = ""
    val file = File(getExternalFilesDir(null), fileName)
    if(file.exists()) {
        content = file.readText()
        tag("Reading from : $file")
    }
    return content
}

fun Context.drawaleGradiantColor(drawableId: Int, colorList: IntArray?): GradientDrawable {
    val background = ResourcesCompat.getDrawable(this.resources, drawableId, null) as GradientDrawable
    background.mutate()
    background.colors = colorList
    return background
}

fun Context.screenShot(view: View, fileName: String): File {
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

fun Context.shareLink(url: String, extra: String) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND;
    sendIntent.putExtra(Intent.EXTRA_TEXT, "$extra $url")
    sendIntent.type = "text/plain"
    startActivity(Intent.createChooser(sendIntent, "Select"))
}

fun Context.printKeyHash() {
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

fun getReferralLink(referralId: String) {
    val dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
        .setLink(Uri.parse("https://gsp.allen.in?referral_id=$referralId"))
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
        tag("$topic Subscription: $msg")
    }
}