package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import android.view.View
import androidx.lifecycle.ViewModel

class ProfileViewModel: ViewModel() {

    private val TAG = ProfileViewModel::class.java.name

    var username: String? = null
    var mobile: String? = null
    var location: String? = null
    var quote: String? = null

    var profileListener: ProfileListener? = null

    fun btnActionProfileEdit(view: View) {
        when (view.id) {
            R.id.btnCancel -> {
                profileListener?.onFailed("cancel")
            }
            R.id.btnSave -> {

            }
        }
    }

}