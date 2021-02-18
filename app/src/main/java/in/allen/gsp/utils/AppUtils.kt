package `in`.allen.gsp.utils

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.dynamiclinks.DynamicLink
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.math.RoundingMode
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil


fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun View.snackbar(message: String) {
    Snackbar.make(this, message, Snackbar.LENGTH_INDEFINITE).also { snackbar ->
        snackbar.setAction("Ok") {
            snackbar.dismiss()
        }
    }.show()
}

fun Activity.confirmDialog(
    title: String,
    message: String,
    actionYes: () -> Unit,
    actionNo: () -> Unit
) {
    val dialogBuilder = AlertDialog.Builder(this)
    val inflater = this.layoutInflater
    val dialogView = inflater.inflate(R.layout.dialog_confirm, null)
    dialogBuilder.setView(dialogView)

    val btnYes: Button = dialogView.findViewById(R.id.btnYes)
    val btnNo: Button = dialogView.findViewById(R.id.btnNo)
    val ttle: TextView = dialogView.findViewById(R.id.title)
    val msg: TextView = dialogView.findViewById(R.id.msg)
    val alertDialog = dialogBuilder.create()

    ttle.text = title
    msg.text = message

    btnYes.setOnClickListener {
        alertDialog.dismiss()
        actionYes()
    }

    btnNo.setOnClickListener {
        alertDialog.dismiss()
        actionNo()
    }

    //In Android, AlertDialog insert into another container, to avoid that, we need to make back ground transparent
    alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    alertDialog.setCanceledOnTouchOutside(false)
    alertDialog.setCancelable(false)

    alertDialog.window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )
    alertDialog.show()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        alertDialog.window?.setDecorFitsSystemWindows(false)
    } else {
        alertDialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
    alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
}

fun Activity.alertDialog(title: String, message: String, alertAction: () -> Unit) {
    val dialogBuilder = AlertDialog.Builder(this)
    val inflater = this.layoutInflater
    val dialogView = inflater.inflate(R.layout.dialog_alert, null)
    dialogBuilder.setView(dialogView)

    val btnOk: Button = dialogView.findViewById(R.id.btnOk)
    val ttle: TextView = dialogView.findViewById(R.id.title)
    val msg: TextView = dialogView.findViewById(R.id.msg)
    val alertDialog = dialogBuilder.create()

    ttle.text = title
    msg.text = message

    btnOk.setOnClickListener {
        alertDialog.dismiss()
        alertAction()
    }

    //In Android, AlertDialog insert into another container, to avoid that, we need to make back ground transparent
    alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    alertDialog.setCanceledOnTouchOutside(false)
    alertDialog.setCancelable(false)

    alertDialog.window?.setFlags(
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    )
    alertDialog.show()
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        alertDialog.window?.setDecorFitsSystemWindows(false)
    } else {
        alertDialog.window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
    alertDialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
}

fun Activity.showSystemUI() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(true)
    } else {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }
}

fun Activity.hideSystemUI() {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        window.setDecorFitsSystemWindows(false)
    } else {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}

fun Activity.hideStatusBar() {
    // hide statusbar
    window.setFlags(
        WindowManager.LayoutParams.FLAG_FULLSCREEN,
        WindowManager.LayoutParams.FLAG_FULLSCREEN
    )
}

fun Context.densityFactor(): Float {
    return resources.displayMetrics.density
}

fun tag(data: Any) {
    Log.e("GSP", "$data")
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

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Context.showKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(view, 0)
}

fun ImageView.loadImage(url: String, circular: Boolean = false, centerInside: Boolean = false) {
    if(circular) {
        Glide.with(this)
            .load(url)
            .transition(DrawableTransitionOptions.withCrossFade(1000))
            .circleCrop()
            .into(this)
    } else {
        if(centerInside) {
            Glide.with(this)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .centerInside()
                .into(this)
        } else {
            Glide.with(this)
                .load(url)
                .transition(DrawableTransitionOptions.withCrossFade(1000))
                .centerCrop()
                .into(this)
        }
    }
}

fun getMediaDuration(path: String?): Long {
    var timeInMillisec:Long = 0
    val retriever = MediaMetadataRetriever()
    retriever.setDataSource(path, HashMap())
    val time: String? = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
    if(time != null) {
        timeInMillisec = time.toLong()
    }
    retriever.release()
    return timeInMillisec
}

fun stringToMilis(date: String, pattern: String): Long {
    val sdf = SimpleDateFormat(pattern)
    val c = Calendar.getInstance()
    c.time = sdf.parse(date)
    return c.timeInMillis
}

fun milisToFormat(milliseconds: Long, format: String): String {
    val sdf = SimpleDateFormat(format)
    val resultdate = Date(milliseconds)
    return sdf.format(resultdate)
}

fun timeInAgo(dateTime: String?, format: String): String {
    val ago: String
    val sdf = SimpleDateFormat(format)
    sdf.timeZone = TimeZone.getTimeZone("GMT+05:30")

    val time = sdf.parse(dateTime).time
    val now = System.currentTimeMillis()
    ago = DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS) as String
    return ago
}

fun formatNumber(pattern: String, num: Number): String? {
    val df = DecimalFormat(pattern)
    df.roundingMode = RoundingMode.CEILING
    return df.format(num)
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

fun Context.saveStream(body: ResponseBody, fileName: String): File {
    val input: InputStream = body.byteStream()
    val file = File(getExternalFilesDir(null), fileName)
    val stream = FileOutputStream(file)
    stream.use { output ->
        val buffer = ByteArray(4 * 1024) // or other buffer size
        var read: Int
        while (input.read(buffer).also { read = it } != -1) {
            output.write(buffer, 0, read)
        }
        output.flush()
    }
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
            tag("KeyHash: ${Base64.encodeToString(md.digest(), Base64.DEFAULT)}")
        }
    } catch (e: PackageManager.NameNotFoundException) {
    } catch (e: NoSuchAlgorithmException) {
    }
}

fun shuffleString(input: String): String {
    val characters: MutableList<Char> = ArrayList()
    for (c in input.toCharArray()) {
        characters.add(c)
    }
    val output = StringBuilder(input.length)
    while (characters.size != 0) {
        val randPicker = (Math.random() * characters.size).toInt()
        output.append(characters.removeAt(randPicker))
    }
    return output.toString()
}

fun readingTime(text: String): Long {
    val wpm = 180 // readable words per minute
    val wordLength = 5 // standardized number of chars in calculable word
    val words = text.length / wordLength
    val wordsTime = words * 60 * 1000 / wpm.toLong()
    val delay = 1000 // milliseconds before user starts reading
    val bonus = 1000 // extra time
    return ceil(delay + wordsTime + bonus.toDouble()).toLong()
}

fun getReferralLink(referralId: String): String {
    val dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
        .setLink(Uri.parse("https://gsp.allen.in?referral_id=$referralId"))
        .setDomainUriPrefix("https://allengsp.page.link") // Open links with this app on Android
        .setAndroidParameters(
            DynamicLink.AndroidParameters.Builder(BuildConfig.APPLICATION_ID).build()
        )
        .buildDynamicLink()
    return dynamicLink.uri.toString()
}

fun Context.share(image: String, message: String) {
    val file = File(getExternalFilesDir(null), image)
    val share = Intent(Intent.ACTION_SEND)
    share.type = "image/*"
    share.putExtra(
        Intent.EXTRA_STREAM, FileProvider.getUriForFile(
            this,
            applicationContext.packageName + ".provider",
            file
        )
    )
    share.putExtra(Intent.EXTRA_TEXT, message)
    startActivity(Intent.createChooser(share, "Share with"))
}

fun urlToBitmap(url: String): Bitmap? {
    var bmp: Bitmap? = null
    try {
        val url = URL(url)
        bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
    } catch (e: IOException) {
        tag(e)
    }
    return bmp
}