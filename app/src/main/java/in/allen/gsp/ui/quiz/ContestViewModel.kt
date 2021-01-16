package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.data.entities.*
import `in`.allen.gsp.data.repositories.QuizRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.*
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri
import android.os.CountDownTimer
import android.view.View
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.squareup.picasso.Picasso
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.set
import kotlin.math.ceil


class ContestViewModel(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository,
    private val preferences: AppPreferences
): ViewModel() {

    val ALERT = "alert"
    val SNACKBAR = "snackbar"
    val TAG = "tag"
    val TOAST = "toast"

    var user: User?= null
    var contestId = 0
    var currentTime = Calendar.getInstance().timeInMillis

    var quiz: Quiz? = null
    lateinit var qset: ArrayList<Question>
    lateinit var wset: ArrayList<Question>
    lateinit var fset: ArrayList<Question>
    lateinit var attachmentList: ArrayList<Attachment>
    lateinit var currentq: Question

    private lateinit var start_time: String

    var index = 0
    var lang = "eng"
    var shuffleCoins = 1

    val lifeline = HashMap<String, Boolean>()
    val score = HashMap<Int, Long>()
    val xp = HashMap<Int, Long>()
    val statsData = ArrayList<HashMap<String, Int>>()

    private var multiplier: Long = 1
    private var timerRemainingTime:Long = 0
    private var lock = false
    private var isPause = false
    var isResumable = true

    var isPopupOpen = false
    var isDoubleDip = false

    var TIME_DELAY: Long = 1000
    val TIME_MOVE_TO_NEXT: Long = 2500
    val TIME_POPUP: Long = 2500

    private val firebaseStorage = FirebaseStorage.getInstance("gs://firebase-gyan-se-pehchan.appspot.com")
    private val storageReference = firebaseStorage.reference

    // interaction with activity
    private val _loading = MutableLiveData<Resource.Loading<Any>>()
    private val _success = MutableLiveData<Resource.Success<Any>>()
    private val _error = MutableLiveData<Resource.Error<String>>()

    fun getLoading(): LiveData<Resource.Loading<Any>> {
        return _loading
    }

    fun getError(): LiveData<Resource.Error<String>> {
        return _error
    }

    fun getSuccess(): LiveData<Resource.Success<Any>> {
        return _success
    }

    fun setLoading(loading: Boolean) {
        _loading.value = Resource.Loading(loading)
    }

    fun setError(data: String, filter: String) {
        _error.value = Resource.Error(filter, data)
    }

    fun setSuccess(data: Any, filter: String) {
        _success.value = Resource.Success(data, filter)
    }

    fun userData() {
        viewModelScope.launch {
            val dbUser = userRepository.getDBUser()
            if (dbUser != null) {
                user = dbUser
                setSuccess(dbUser, "user")
            } else {
                setError("Not Found", TAG)
            }
        }
    }

    fun contestStatus() {
        if(user != null && user!!.user_id > 0 && contestId > 0) {
            viewModelScope.launch {
                try {
                    setLoading(true)
                    val response = quizRepository.contestStatus(user!!.user_id,contestId)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if(responseObj.getInt("status") == 1) {
                            val dataObj = responseObj.getJSONObject("data")
                            setSuccess(dataObj,"contestStatus")
                        } else {
                            setError("contestData:${responseObj.getString("message")}", ALERT)
                        }
                    }
                } catch (e: Exception) {
                    setError("contestData:${e.message}",ALERT)
                }
            }
        }
    }

    fun enrolVisibility(contestObj: JSONObject) {
        val enrolStartTime = stringToMilis(
            contestObj.getString("enrollment_start_time"),
            "yyyy-MM-dd HH:mm:ss")
        val enrolEndTime = stringToMilis(
            contestObj.getString("enrollment_end_time"),
            "yyyy-MM-dd HH:mm:ss")
        if(!contestObj.has("isEnrolled")) {
            if (currentTime in enrolStartTime..enrolEndTime) {
                setSuccess(contestObj.getInt("id"),"enableContestEnrollment")
                val diff = enrolEndTime - currentTime
                if (diff > 0) {
                    setSuccess(diff,"startCountdownEnrollment")
                }
            }
        }
    }

    fun playVisibility(dataObj: JSONObject) {
        val contestObj = dataObj.getJSONObject("contest")
        val contestStartTime = stringToMilis(dataObj.getString("start_time"), "yyyy-MM-dd HH:mm:ss")
        val contestEndTime = stringToMilis(dataObj.getString("end_time"), "yyyy-MM-dd HH:mm:ss")
        if (currentTime in contestStartTime..contestEndTime) {
            setSuccess(contestObj,"enableContestPlay")
        } else {
            val diff = contestStartTime - currentTime
            tag("enableContestPlay $diff")
            if (diff > 0) {
                val hashmap = HashMap<String,Any>()
                hashmap["diff"] = diff
                hashmap["contestObj"] = contestObj
                setSuccess(hashmap,"startCountdownContest")
            }
        }
    }

    fun enrolContest() {
        if(user != null && user!!.user_id > 0 && contestId > 0) {
            viewModelScope.launch {
                try {
                    setLoading(true)
                    val response = quizRepository.contestEnrol(user!!.user_id,contestId)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if(responseObj.getInt("status") == 1) {
                            setError(responseObj.getString("message"),TOAST)
                            contestStatus()
                        } else {
                            setError("contestData:${responseObj.getString("message")}", ALERT)
                        }
                    }
                } catch (e: Exception) {
                    setError("contestData:${e.message}",ALERT)
                }
            }
        }
    }

    // enrolment timer
    private var countdownEnrollment: CountDownTimer?= null
    fun countdownEnrollmentStart(i: Long) {
        countdownEnrollment = object : CountDownTimer(i, 10) {
            override fun onTick(l: Long) {
                setSuccess(l, "countdownEnrollment")
            }

            override fun onFinish() {
                countdownEnrollmentCancel()
                setSuccess("", "countdownEnrollmentFinish")
            }
        }
        (countdownEnrollment as CountDownTimer).start()
    }

    fun countdownEnrollmentCancel() {
        countdownEnrollment?.cancel()
    }

    // contest timer
    private var countdownContest: CountDownTimer?= null
    fun countdownContestStart(i: Long, contestObj: JSONObject) {
//        val i1 = i.minus(86400000).minus(28800000).minus(480000)
        countdownContest = object : CountDownTimer(i, 1000) {
            override fun onTick(l: Long) {
                setSuccess(l, "countdownContest")
            }

            override fun onFinish() {
                countdownContestCancel()
                setSuccess(contestObj, "countdownContestFinish")
            }
        }
        (countdownContest as CountDownTimer).start()
    }

    fun countdownContestCancel() {
        countdownContest?.cancel()
    }



    // contest activity
    fun quizData() {
        setLoading(true)
        if(user != null && user!!.user_id > 0) {
            viewModelScope.launch {
                try {
//                    val res = quizRepository.contestGet(user!!.user_id,contestId)
                    val res = quizRepository.getPreview(user!!.user_id)
                    setQuizData(res)
                } catch (e: Exception) {
                    setError("contestData:${e.message}",ALERT)
                }
            }
        }
    }

    private fun setQuizData(quizData: Quiz) {
        tag("setQuizData");
        score.clear()
        xp.clear()
        lifeline.clear()
        statsData.clear()

        multiplier = 1
        timerRemainingTime = 0
        lock = false
        isPause = false
        isResumable = true

        isDoubleDip = false
        isPopupOpen = false
        index = 0
        start_time = milisToFormat(Calendar.getInstance().timeInMillis, "yyyy-MM-dd HH:mm:ss")

        multiplierTimerCancel()
        questionTimerCancel()
        attachmentTimerCancel()

        quiz = quizData
        qset = quizRepository.getQset(this.quiz!!)
        wset = quizRepository.getWset(this.quiz!!)
        fset = quizRepository.getFset(this.quiz!!)
        attachmentList = quizRepository.getAttachmentList(this.quiz!!)

        // save attachment
        setSuccess("downloadAttachment", "downloadAttachment")
    }

    // display quiz
    fun moveToNext() {
        viewModelScope.launch {
            delay(TIME_MOVE_TO_NEXT)
            if(index < qset.size.minus(1)) {
                index++
                currentq = qset[index]
                setSuccess("setQuestion", "quizStatus")
            } else {
                setSuccess("displayFinish", "quizStatus")
            }
        }
    }

    fun displayQuestion() {
        viewModelScope.launch {
            delay(TIME_DELAY)
            setSuccess("displayQuestion", "quizStatus")
        }
    }

    fun displayAttachment() {
        viewModelScope.launch {
            delay(TIME_DELAY)
            if(currentq.qattach.trim().isNotEmpty() && !currentq.qattach.trim().equals("null", true)) {
                isResumable = false
                isPopupOpen = true
                setSuccess("displayAttachment", "quizStatus")
            } else {
                displayOption()
            }
        }
    }

    fun displayOption() {
        viewModelScope.launch {
            delay(TIME_DELAY)
            isResumable = true
            setSuccess("displayOption","quizStatus")
        }
    }

    fun startTimer() {
        viewModelScope.launch {
            delay(TIME_DELAY)
            questionTimerStart((currentq.qTime * 1000).toLong())
            multiplierTimerStart(10 * 1000)
            lock(false)
        }
    }

    fun stopTimer() {
        lock(true)
        questionTimerCancel()
        multiplierTimerCancel()
    }

    fun calculateScore(duration: Int) {
        val stats = HashMap<String, Int>()
        stats["qid"] = currentq.qid
        stats["duration"] = if(duration == 0) 1 else duration

        // check existing qid before adding
        var exists = false
        for(sd in statsData) {
            if(sd.containsKey(currentq.qid)) {
                exists = true
                break
            }
        }
        if(!exists) {
            statsData.add(stats)
        }

        // set to quiz data
        user?.played_qid = user?.played_qid!!.plus("${currentq.qid},")
        Coroutines.io {
            tag("${currentq.qid} addingqids ${user?.played_qid}")
            user?.let { userRepository.setDBUser(it) }
        }

        xp[currentq.qno] = currentq.qTime.minus(duration).toLong()
        val cscore: Long = currentq.qTime.minus(duration)
            .times(
                if(multiplier < 100) 100 else multiplier
            )
        score[currentq.qno] = cscore

        setSuccess(score.values.sum(),"calculateScore")
    }


    fun finishQuiz() {
        viewModelScope.launch {
            delay(TIME_MOVE_TO_NEXT)
            isPopupOpen = true
            if(!user?.is_admin!!) {
                saveData()
            }
            setSuccess("displayFinish","quizStatus")
        }
    }

    private fun saveData() {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                val params = HashMap<String, String>()
                params["contest_id"] = contestId.toString()
                params["start_time"] = start_time
                params["end_time"] =
                    milisToFormat(Calendar.getInstance().timeInMillis, "yyyy-MM-dd HH:mm:ss")
                params["stats_data"] = Gson().toJson(statsData)
                params["life_lines"] = Gson().toJson(lifeline)
                params["score"] = score.values.sum().toString()
                params["points"] = xp.values.sum().toString()
                params["user_id"] = user?.user_id!!.toString()

                if (currentq.qno in 1..5) {
                    params["achieved_level"] = "1"
                }
                if (currentq.qno in 6..10) {
                    params["achieved_level"] = "2"
                }
                if (currentq.qno in 11..15) {
                    params["achieved_level"] = "3"
                }

                params["achieved_questions"] = statsData.size.toString()

                quizRepository.contestPost(params)
            }
        }
    }

    fun lock(status: Boolean) {
        tag("lock $status")
        lock = status
        setSuccess(status,"lock")
    }



    // multiplier timer
    private var multiplierTimer: CountDownTimer?= null
    private fun multiplierTimerStart(i: Long) {
        multiplierTimer = object : CountDownTimer(i, 10) {
            override fun onTick(l: Long) {
                if(l > 0) {
                    multiplier = l
                }
                setSuccess(l, "multiplierTimer")
            }

            override fun onFinish() {
                multiplierTimerCancel()
            }
        }
        (multiplierTimer as CountDownTimer).start()
    }

    fun multiplierTimerCancel() {
        multiplierTimer?.cancel()
    }


    // Question Timer
    private var questionTimer: CountDownTimer?= null
    private fun questionTimerStart(i: Long) {
        questionTimer = object : CountDownTimer(i, 10) {
            override fun onTick(l: Long) {
                timerRemainingTime = l
                setSuccess(l, "questionTimer")
            }

            override fun onFinish() {
                questionTimerCancel()
                finishQuiz()
            }
        }
        (questionTimer as CountDownTimer).start()
    }

    fun questionTimerCancel() {
        questionTimer?.cancel()
    }


    // attachment timer
    private var attachmentTimer: CountDownTimer?= null
    fun attachmentTimerStart(i: Long) {
        attachmentTimer = object : CountDownTimer(i, 10) {
            override fun onTick(l: Long) {
                setSuccess(l,"attachmentTimer")
            }

            override fun onFinish() {
                attachmentTimerCancel()
                closeAttachment()
            }
        }
        (attachmentTimer as CountDownTimer).start()
    }

    fun attachmentTimerCancel() {
        attachmentTimer?.cancel()
    }

    private fun closeAttachment() {
        setSuccess("closeAttachment","quizStatus")
    }


    fun onBackPressed() {
        tag("$TAG onBackPressed: $lock")
        if(!lock) {
            if (timerRemainingTime > 500) {
                isPopupOpen = true
                setSuccess("onBackPressed","quizStatus")
            }
        }
    }

    fun onPause() {
        isPause = true
        if(isPause) {
            stopTimer()
            if(!isResumable) {
                setSuccess("exit","quizStatus")
            }
        }
    }

    fun onResume() {
        // execute only when activity got resumed from pause
        if(isPause) {
            isPause = false

            if(isResumable) {
                if(!isPopupOpen) {
                    if (timerRemainingTime > 0) {
                        questionTimerStart(timerRemainingTime)
                        // release lock
                        lock(false)
                        timerRemainingTime = 0
                    }

                    if (multiplier > 100) {
                        multiplierTimerStart(multiplier)
                        multiplier = 1
                    }
                } else {
                    isPause = true
                }
            }
        }
    }

    fun validateLifeline(type: String) {
        if (lifeline[type]!! && !lock) {
            if (timerRemainingTime > 500) {
                lifeline[type] = false
                setSuccess(type,"validateLifeline")
            }
        }
    }

    fun flipQuestion() {
        if (fset.size > 2) {
            val qno = currentq.qno
            // check is segment is easy medium or hard
            currentq = when (currentq.qdifficulty_level) {
                1 -> {
                    fset[0]
                }
                2 -> {
                    fset[1]
                }
                else -> {
                    fset[2]
                }
            }
            currentq.qno = qno
        }
        tag("fset: $currentq")
        setSuccess("setQuestion", "quizStatus")
    }

    fun saveFile(destination: File, filepath: String) {
        val pathReference = storageReference.child(filepath)
        pathReference.getFile(destination).addOnSuccessListener {
            setSuccess(
                "success transferred: ${it.bytesTransferred}, total: ${it.totalByteCount}",
                "saveFile"
            )
        }.addOnCompleteListener {
            setSuccess("complete: ${it.isComplete}", "saveFile")
        }.addOnFailureListener {
            setSuccess("fail: ${it.message}", "saveFile")
        }.addOnProgressListener {
            setSuccess(
                "progress transferred: ${it.bytesTransferred}, total: ${it.totalByteCount}",
                "saveFile"
            )
        }
    }

}