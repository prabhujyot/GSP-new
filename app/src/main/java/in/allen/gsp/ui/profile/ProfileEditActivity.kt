package `in`.allen.gsp.ui.profile

import `in`.allen.gsp.R
import `in`.allen.gsp.data.db.entities.User
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.databinding.ActivityProfileEditBinding
import `in`.allen.gsp.utils.*
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.bottomsheet_verification.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.android.synthetic.main.toolbar.view.*
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance

class ProfileEditActivity : AppCompatActivity(), KodeinAware {

    private val TAG = ProfileEditActivity::class.java.name
    private lateinit var binding: ActivityProfileEditBinding
    private lateinit var viewModel: ProfileViewModel

    override val kodein by kodein()
    private val repository: UserRepository by instance()

    private lateinit var otpSheetBehavior: BottomSheetBehavior<View>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_profile_edit)
        viewModel = ProfileViewModel(repository)

        setSupportActionBar(myToolbar)
        myToolbar.btnBack.setOnClickListener {
            onBackPressed()
        }

        // bottomsheet for otp
        otpSheetBehavior = BottomSheetBehavior.from(binding.rootLayout.bottomSheetOTP)
        otpSheetBehavior.isDraggable = false
        binding.rootLayout.otp.addTextChangedListener(textWatcher)

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()
    }

    private fun observeLoading() {
        viewModel.stateLoading().observe(this, {
            tag("$TAG viewModel._loading: ${it.message}")
            binding.rootLayout.showProgress()
        })
    }

    private fun observeError() {
        viewModel.stateError().observe(this, {
            tag("$TAG viewModel._error: ${it.message}")
            binding.rootLayout.hideProgress()
            it.message?.let { it1 ->
                if (it1.isNotBlank()) {
                    binding.rootLayout.snackbar(it1.trim())
                }
            }
        })
    }

    private fun observeSuccess() {
        viewModel.stateSuccess().observe(this, {
            if(it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        val user = it.data as User
                        if (user.avatar.isNotBlank())
                            binding.avatar.loadImage(user.avatar, true)
                        binding.username.text = user.name
                        if (user.mobile.length > 9) {
                            binding.mobile.setText(user.mobile)
                            if(user.is_verified == 0) {
                                binding.verificationStatus.text = "Not Verified"
                                binding.btnVerify.text = "Verify"
                            } else {
                                binding.verificationStatus.text = "Verified"
                                binding.btnVerify.text = "Update"
                            }
                        }
                        if (!user.location.equals("null",true))
                            binding.location.setText(user.location)
                        if (!user.about.equals("null",true))
                            binding.about.setText(user.about)
                    }
                    "verifyMobile" -> {
                        if(otpSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                            otpSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                        binding.btnVerify.text = "Update"
                        binding.btnVerify.isEnabled = true
                        binding.verificationStatus.text = "Verified"
                        viewModel.countdownCancel()
                        toast("${it.data}")
                    }
                    "getOTP" -> {
                        if(otpSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                            otpSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                        }

                        binding.verificationStatus.text = "Not Verified"
                        viewModel.countdownStart(5 * 60 * 1000)
                    }
                    "disableOTP" -> {
                        binding.rootLayout.btnResendOtp.isEnabled = false
                        binding.rootLayout.btnResendOtp.text = it.data as String

                        binding.btnVerify.isEnabled = false
                        binding.btnVerify.text = it.data
                    }
                    "enableOTP" -> {
                        binding.rootLayout.btnResendOtp.isEnabled = true
                        binding.rootLayout.btnResendOtp.text = it.data as String

                        binding.btnVerify.isEnabled = true
                        binding.btnVerify.text = "Verify"
                    }
                }
            }
        })
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
                val otp = binding.rootLayout.otp.text.toString()
                viewModel.verifyMobile(mobile,otp)
            }
        }
    }


    private val textWatcher = object: TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            binding.rootLayout.btnSubmit.isEnabled = false
            if(binding.rootLayout.otp.text.toString().length == 4) {
                binding.rootLayout.btnSubmit.isEnabled = true
            }
        }

        override fun afterTextChanged(p0: Editable?) {}
    }

}