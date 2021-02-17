package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.R
import `in`.allen.gsp.databinding.ActivityContestInstructionBinding
import `in`.allen.gsp.utils.*
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.toolbar.*
import org.json.JSONObject
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.util.concurrent.TimeUnit

class ContestInstructionActivity : AppCompatActivity(), KodeinAware {

    private val TAG = ContestInstructionActivity::class.java.name
    private lateinit var binding: ActivityContestInstructionBinding
    private lateinit var viewModel: ContestViewModel

    override val kodein by kodein()
    private val factory:ContestViewModelFactory by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_contest_instruction)
        viewModel = ViewModelProvider(this, factory).get(ContestViewModel::class.java)

        setSupportActionBar(myToolbar)
        btnBack.setOnClickListener {
            onBackPressed()
        }

        intent.getStringExtra("contest_id").let {
            viewModel.contestId = intent.getStringExtra("contest_id")!!.toInt()
            binding.webView.setBackgroundColor(Color.TRANSPARENT)
        }

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()
    }

    fun actionContestInstruction(view: View) {}

    private fun resetviews() {
        binding.wrapperCountdownEnrollment.visibility = View.GONE
        binding.wrapperCountdownContest.visibility = View.GONE
        binding.btEnrol.visibility = View.GONE
        binding.btPlay.visibility = View.GONE
    }

    private fun observeLoading() {
        viewModel.getLoading().observe(this, {
            tag("$TAG _loading: ${it.message}")
            binding.rootLayout.hideProgress()
            if (it.data is Boolean && it.data) {
                binding.rootLayout.showProgress()
            }
        })
    }

    private fun observeError() {
        viewModel.getError().observe(this, {
            tag("$TAG _error: ${it.message}")
            if (it != null) {
                when (it.message) {
                    "alert" -> {
                        it.data?.let { it1 ->
                            var action = ""
                            var str = ""
                            if(it1.startsWith("contestData:")) {
                                action = "finish"
                                str = it1.removePrefix("contestData:")
                            }
                            alertDialog("Error!", str) {
                                if(action.equals("finish",true)) {
                                    finish()
                                }
                            }
                        }
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
        })
    }

    private fun observeSuccess() {
        viewModel.getSuccess().observe(this, { it ->
            if (it != null) {
                binding.rootLayout.hideProgress()
                when (it.message) {
                    "user" -> {
                        viewModel.contestStatus()
                    }

                    "contestStatus" -> {
                        resetviews()

                        val dataObj = it.data as JSONObject
                        if(!dataObj.getString("contest").equals("false", true)) {
                            val contestObj = dataObj.getJSONObject("contest")
                            // contest enrolVisibilty
//                            tag(contestObj)
                            viewModel.enrolVisibility(contestObj)

                            // contest play visibility
                            if(binding.wrapperCountdownEnrollment.visibility != View.VISIBLE) {
                                viewModel.playVisibility(dataObj)
                            }
                            binding.webView.loadData(contestObj.getString("desc"), "text/html; charset=utf-8", "UTF-8")
                        }

                        if(dataObj.has("msg")) {
                            viewModel.setError(dataObj.getString("msg"),viewModel.SNACKBAR)
                        }
                    }

                    "enableContestEnrollment" -> {
                        enableContestEnrollment()
                    }

                    "enableContestPlay" -> {
                        tag("enableContestPlay")
                        enableContestPlay(it.data as JSONObject)
                    }

                    "startCountdownEnrollment" -> {
                        startCountdownEnrolment(it.data as Long)
                    }

                    "countdownEnrollment" -> {
                        binding.textCountdownenrollment.text = hmsTimeFormatter(it.data as Long)
                    }

                    "countdownEnrollmentFinish" -> {
                        viewModel.contestStatus()
                    }

                    "startCountdownContest" -> {
                        if(it.data is HashMap<*, *>) {
                            startCountdownContest(it.data["diff"] as Long,it.data["contestObj"] as JSONObject)
                        }
                    }

                    "countdownContest" -> {
                        binding.textCountdown.text = hmsTimeFormatter(it.data as Long)
                    }

                    "countdownContestFinish" -> {
                        tag("enableContestPlay finish")
                        binding.textCountdown.show(false)
                        viewModel.setSuccess(it.data as JSONObject,"enableContestPlay")
                    }
                }
            }
        })
    }

    private fun enableContestEnrollment() {
        binding.btEnrol.show()
        binding.btEnrol.isEnabled = true
        binding.btEnrol.setOnClickListener {
            binding.btEnrol.isEnabled = false
            viewModel.enrolContest()
        }
    }

    private fun enableContestPlay(contestObj: JSONObject) {
        tag("enableContestPlay fun")
        binding.wrapperCountdownEnrollment.visibility = View.GONE
        binding.wrapperCountdownContest.visibility = View.GONE

        binding.btEnrol.show(false)
        binding.btPlay.show()
        binding.btPlay.isEnabled = true
        binding.btPlay.setOnClickListener {
//            binding.btPlay.isEnabled = false
//            binding.btPlay.show(false)
            val i = Intent(this, ContestActivity::class.java)
            i.putExtra("contestObj", contestObj.toString())
            startActivity(i)
        }
    }

    private fun startCountdownEnrolment(data: Long) {
        binding.wrapperCountdownContest.show(false)
        binding.wrapperCountdownEnrollment.show()
        viewModel.countdownEnrollmentStart(data)
    }

    private fun startCountdownContest(data: Long, contestObj: JSONObject) {
        binding.wrapperCountdownEnrollment.show(false)
        binding.wrapperCountdownContest.show()
        binding.btEnrol.show(false)
        viewModel.countdownContestStart(data, contestObj)
    }

    private fun hmsTimeFormatter(milliSeconds: Long): String {
        return String.format("%02d:%02d:%02d:%02d",
            TimeUnit.MILLISECONDS.toDays(milliSeconds),
            TimeUnit.MILLISECONDS.toHours(milliSeconds) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(milliSeconds)),
            TimeUnit.MILLISECONDS.toMinutes(milliSeconds) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(milliSeconds)),
            TimeUnit.MILLISECONDS.toSeconds(milliSeconds) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(milliSeconds)))
    }
}