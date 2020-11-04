package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.data.entities.Attachment
import `in`.allen.gsp.data.entities.Question
import `in`.allen.gsp.data.entities.Quiz
import `in`.allen.gsp.data.entities.User
import `in`.allen.gsp.data.repositories.QuizRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.Resource
import `in`.allen.gsp.utils.lazyDeferred
import `in`.allen.gsp.utils.tag
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.ceil


class QuizViewModel(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository
): ViewModel() {

    val ALERT = "alert"
    val SNACKBAR = "snackbar"
    val TAG = "tag"
    val TOAST = "toast"

    var user: User?= null
    var quiz: Quiz? = null
    lateinit var qset: ArrayList<Question>
    lateinit var wset: ArrayList<Question>
    lateinit var fset: ArrayList<Question>
    lateinit var attachmentList: ArrayList<Attachment>
    lateinit var currentq: Question
    var index = 0
    var lang = "eng"
    private var multiplier: Long = 1

    val lifeline = HashMap<String, Boolean>()
    val score = HashMap<Int, Long>()

    private var timerRemainingTime:Long = 0
    private var lock = false
    private var isPause = false
    private var isResumable = true

    var isPopupOpen = false

    var acorrect = 0
    var isLastQuestion: Boolean = false


    var TIME_DELAY: Long = 1000
    var TIME_READING: Long = 2000
    val TIME_MOVE_TO_NEXT: Long = 2500
    val TIME_POPUP: Long = 2500
    val TIME_EXTRA_POPUP: Long = 0
    val TIME_OPTIONS_SHOWING: Long = 1500
    var TIME_ATTACHMENT: Long = 15000

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

    fun previewData() {
        setLoading(true)
        if(user != null && user!!.user_id > 0) {
            val response by lazyDeferred {
                quizRepository.getPreview(user!!.user_id)
            }
            setSuccess(response, "quizData")
        }
    }

    fun setQuizData(quizData: Quiz) {
        index = 0
        score.clear()
        lifeline.clear()

        quiz = quizData
        qset = quizRepository.getQset(quiz!!)
        wset = quizRepository.getWset(quiz!!)
        fset = quizRepository.getFset(quiz!!)
        attachmentList = quizRepository.getAttachmentList(quiz!!)

        // save attachment
        setSuccess(attachmentList, "downloadAttachment")
    }

    // display quiz
    fun moveToNext() {
        viewModelScope.launch {
            delay(TIME_MOVE_TO_NEXT)
            if(index < qset.size) {
                index ++
                currentq = qset[index]

                setSuccess("setQuestion", "quizStatus")
            } else {
                setSuccess("finish", "quizStatus")
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
            delay(TIME_READING)
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
            delay(300)
            isResumable = true
            setSuccess("displayOption", "quizStatus")
        }
    }

    fun calculateScore(duration: Int) {
        val stats = HashMap<String, Int>()
        stats["qid"] = currentq.qid
        stats["duration"] = if(duration == 0) 1 else duration

        // set to quiz data
        user?.played_qid.plus("${currentq.qid},")

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
            setSuccess("displayFinish","quizStatus")
        }
    }

    fun lock(status: Boolean) {
        lock = status
        setSuccess(status,"lock")
    }



    // multiplier timer
    private var multiplierTimer: CountDownTimer?= null
    fun multiplierTimerStart(i: Long) {
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
    fun questionTimerStart(i: Long) {
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
            questionTimerCancel()
            multiplierTimerCancel()

            lock(true)

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

    fun readingTime(text: String): Long {
        val wpm = 180 // readable words per minute
        val wordLength = 5 // standardized number of chars in calculable word
        val words = text.length / wordLength
        val wordsTime = words * 60 * 1000 / wpm.toLong()
        val delay = 1000 // milliseconds before user starts reading
        val bonus = 1000 // extra time
        return ceil(delay + wordsTime + bonus.toDouble()).toLong()
    }

    fun shuffle(input: String): String {
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

}