package `in`.allen.gsp.utils

import `in`.allen.gsp.R
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.snackbar.Snackbar

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
