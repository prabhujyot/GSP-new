package `in`.allen.gsp.helpers

import android.content.Context
import android.util.Log
import android.widget.Toast

fun Context.toast(message: String) {
    Toast.makeText(this,message,Toast.LENGTH_LONG).show()
}

fun Context.tag(TAG:String,data:Any) {
    Log.d(TAG,"$data")
}