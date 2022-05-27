package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityProfileEditBinding
import `in`.allen.gsp.utils.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import com.chaos.view.PinView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance

class ProfileEditActivity : AppCompatActivity(), DIAware {

    private val TAG = ProfileEditActivity::class.java.name
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var viewModel: ProfileViewModel

    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val repository: UserRepository by instance()
    private val rewardRepository: RewardRepository by instance()

    private lateinit var bottomSheetOTP: FrameLayout
    private lateinit var otpSheetBehavior: BottomSheetBehavior<View>
    private lateinit var otp: PinView
    private lateinit var btnResendOtp: Button
    private lateinit var btnSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_edit)
        viewModel = ProfileViewModel(repository,rewardRepository)

        val toolbar = binding.root.findViewById<Toolbar>(R.id.myToolbar)

        setSupportActionBar(toolbar)
        toolbar.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // bottomsheet for otp
        bottomSheetOTP = binding.root.findViewById(R.id.bottomSheetOTP)
        otpSheetBehavior = BottomSheetBehavior.from(bottomSheetOTP)
        otpSheetBehavior.isDraggable = false

        otp = bottomSheetOTP.findViewById(R.id.otp)
        otp.addTextChangedListener(textWatcher)

        btnResendOtp = bottomSheetOTP.findViewById(R.id.btnResendOtp)
        btnSubmit = bottomSheetOTP.findViewById(R.id.btnSubmit)

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()

        binding.mobile.requestFocus()
    }

    private fun observeLoading() {
        viewModel.getLoading().observe(this) {
            tag("$TAG _loading: ${it.message}")
            binding.rootLayout.hideProgress()
            if (it.data is Boolean && it.data) {
                binding.rootLayout.showProgress()
            }
        }
    }

    private fun observeError() {
        viewModel.getError().observe(this) {
            tag("$TAG _error: ${it.message}")
            if (it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "alert" -> {
                        it.data?.let { it1 -> alertDialog("Error", it1) {} }
                    }
                    "tag" -> {
                        it.data?.let { it1 -> tag("$TAG $it1") }
                    }
                    "toast" -> {
                        it.data?.let { it1 -> toast(it1) }
                    }
                    "snackbar" -> {
                        it.data?.let { it1 -> binding.rootLayout.snackbar(it1) }
                    }
                }
            }
        }
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(this) {
            if (it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        val user = it.data as User
                        if (user.avatar.isNotBlank())
                            binding.avatar.loadImage(user.avatar, true)
                        binding.username.text = user.name
                        if (user.mobile.length > 9) {
                            binding.mobile.setText(user.mobile)
                            if (user.is_verified == 0) {
                                binding.verificationStatus.text = "Not Verified"
                                binding.btnVerify.text = "Verify"
                            } else {
                                binding.verificationStatus.text = "Verified"
                                binding.btnVerify.text = "Update"
                            }
                        }
                        if (!user.location.equals("null", true))
                            binding.location.setText(user.location)
                        if (!user.about.equals("null", true))
                            binding.about.setText(user.about)
                    }
                    "verifyMobile" -> {
                        if (otpSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                            otpSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                        binding.btnVerify.text = "Update"
                        binding.btnVerify.isEnabled = true
                        binding.verificationStatus.text = "Verified"
                        viewModel.countdownCancel()
                        toast("${it.data}")
                    }
                    "getOTP" -> {
                        if (otpSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                            otpSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        bottomSheetOTP.findViewById<PinView>(R.id.otp).setText("")
                        binding.verificationStatus.text = "Not Verified"
                        viewModel.countdownStart(5 * 60 * 1000)
                    }
                    "disableOTP" -> {
                        btnResendOtp.isEnabled = false
                        btnResendOtp.text = it.data as String

                        binding.btnVerify.isEnabled = false
                        binding.btnVerify.text = it.data
                    }
                    "enableOTP" -> {
                        btnResendOtp.isEnabled = true
                        btnResendOtp.text = it.data as String

                        binding.btnVerify.isEnabled = true
                        binding.btnVerify.text = "Verify"
                    }
                    "updateProfile" -> {
                        otp.requestFocus()
                        viewModel.setError("Profile updated", viewModel.SNACKBAR)
                    }
                }
            }
        }
    }

    fun btnActionProfileEdit(view: View) {
        val mobile = binding.mobile.text.toString()
        val location = binding.location.text.toString()
        val about = binding.about.text.toString()

        when (view.id) {
            R.id.btnVerify -> {
                if(mobile.trim().length < 10) {
                    binding.mobile.error = "Enter valid mobile"
                } else {
                    viewModel.getOTP(mobile)
                }
            }
            R.id.btnSave -> {
                var flag = true
                if(location.trim().length < 3
                    && about.trim().length < 3) {
                    flag = false
                }

                if(flag) {
                    val params = HashMap<String, String>()
                    params["location"] = location
                    params["about"] = about
                    viewModel.updateProfile(params)
                }
            }
            R.id.btnClose -> {
                if(otpSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    otpSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
            R.id.btnResendOtp -> {
                viewModel.getOTP(mobile)
            }
            R.id.btnSubmit -> {
                hideKeyboard(view)
                val otp = otp.text.toString()
                viewModel.verifyMobile(mobile,otp)
            }
        }
    }


    private val textWatcher = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            btnSubmit.isEnabled = false
            if(otp.text.toString().length == 4) {
                btnSubmit.isEnabled = true
            }
        }

        override fun afterTextChanged(p0: Editable?) {}
    }

}