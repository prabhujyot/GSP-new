package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.data.entities.*
import `in`.allen.gsp.data.repositories.QuizRepository
import `in`.allen.gsp.data.repositories.RewardRepository
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.utils.*
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.*
import kotlin.collections.set


class QuizViewModel(
    private val userRepository: UserRepository,
    private val quizRepository: QuizRepository,
    private val rewardRepository: RewardRepository,
    private val preferences: AppPreferences
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

    private lateinit var start_time: String

    var index = 0
    var lang = "eng"
    var shuffleCoins = 1

    val lifeline = HashMap<String, Boolean>()
    val score = HashMap<Int, Long>()
    val xp = HashMap<Int, Long>()
    val statsData = ArrayList<HashMap<String, Int>>()
    private val questionSet = ArrayList<Int>()

    private var multiplier: Long = 1
    private var timerRemainingTime:Long = 0
    private var lock = false
    private var isPause = false
    var isResumable = true

    var isWild = false
    var isPopupOpen = false
    var isDoubleDip = false

    var TIME_DELAY: Long = 1000
    var TIME_READING: Long = 2000
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
                if(userRepository.config("shuffle-value").isNotEmpty()) {
                    shuffleCoins = userRepository.config("shuffle-value").toInt()
                }
                setSuccess(dbUser, "user")
            } else {
                setError("Not Found", TAG)
            }
        }
    }

    fun quizData() {
        setLoading(true)
        if(user != null && user!!.user_id > 0) {
            viewModelScope.launch {
                try {
                    if(user?.life!! > 0) {
                        if(!preferences.previewMode) {
                            val res = quizRepository.getQuiz(user!!.user_id)
                            setQuizData(res)
                        } else {
                            val res = quizRepository.getPreview(user!!.user_id)
                            setQuizData(res)
                        }
                    } else {
                        setSuccess("showOffers","quizStatus")
                    }
                } catch (e: Exception) {
                    setError("quizdata:${e.message}",ALERT)
                }
            }
        }
    }

    private fun setQuizData(quizData: Quiz) {
        tag("$TAG, setQuizData");
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
        isWild = false
        isPopupOpen = false

        index = 0
        start_time = milisToFormat(Calendar.getInstance().timeInMillis, "yyyy-MM-dd HH:mm:ss")

        multiplierTimerCancel()
        questionTimerCancel()

        quiz = quizData
        qset = quizRepository.getQset(this.quiz!!)
        wset = quizRepository.getWset(this.quiz!!)
        fset = quizRepository.getFset(this.quiz!!)
        attachmentList = quizRepository.getAttachmentList(this.quiz!!)

        questionSet.clear()
        for(q in qset) {
            questionSet.add(q.qid)
        }
        for(f in fset) {
            questionSet.add(f.qid)
        }

        // save attachment
        setSuccess("downloadAttachment", "downloadAttachment")
    }

    // display quiz
    fun moveToNext() {
        viewModelScope.launch {
            delay(TIME_MOVE_TO_NEXT)
            if(index < qset.size.minus(1)) {
                //check level achievement
                if(!isWild && (index == 4 || index == 9)) {
                    setSuccess("displayWild","quizStatus")
                } else {
                    // reset current question to qset if current question is wild
                    if(isWild && index == 4) {
                        // store wild qset
                        getWild()
                    }
                    isWild = false

                    index++
                    currentq = qset[index]
                    setSuccess("setQuestion", "quizStatus")
                }
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
        val dur = if(duration == 0) 1 else duration
        stats["duration"] = dur

        // check existing qid before adding
        var exists = false
        for(sd in statsData) {
            if(sd.containsKey(currentq.qid.toString())) {
                exists = true
                break
            }
        }
        if(!exists) {
            statsData.add(stats)
        }

        // set to quiz data
        appendPlayedQid()

        xp[currentq.qno] = currentq.qTime.minus(dur).toLong()
        val cscore: Long = currentq.qTime.minus(dur)
            .times(
                if(multiplier < 100) 100 else multiplier
            )
        score[currentq.qno] = cscore

        setSuccess(score.values.sum(),"calculateScore")
    }

    fun appendPlayedQid() {
        user?.played_qid = user?.played_qid!!.plus("${currentq.qid},")
        Coroutines.io {
            tag("$TAG, ${currentq.qid} addingqids ${user?.played_qid}")
            user?.let { userRepository.setDBUser(it) }
        }
    }

    fun setWildQuestion(question: Question) {
        isWild = true
        val qno = currentq.qno
        currentq = question
        currentq.qno = qno
        currentq.qTime = 30
        setSuccess("setQuestion", "quizStatus")
    }

    fun shuffleWild(coins: Int) {
        if(user != null && user?.user_id!! > 0) {
            if(coins <= user?.coins!!) {
                viewModelScope.launch {
                    setLoading(true)
                    lock(true)
                    try {
                        val response = quizRepository.getWildQuiz(user?.user_id!!, coins)
                        setLoading(false)
                        lock(false)
                        if (response != null) {
                            val responseObj = JSONObject(response)
                            if(responseObj.getInt("status") == 1) {
                                val dataObj = responseObj.getJSONObject("data")
                                var arr = dataObj.getJSONArray("qset")
                                if(arr.length() > 0) {
                                    wset.clear()
                                    for(i in 0 until arr.length()) {
                                        val item = arr.get(i) as JSONObject

                                        // setting options
                                        val optionsArr: JSONArray = item.getJSONArray("options")
                                        val options: MutableList<Option> = ArrayList()
                                        for (j in 0 until optionsArr.length()) {
                                            val obj2 = optionsArr[j] as JSONObject
                                            val option = Option(
                                                obj2.getInt("aid"),
                                                obj2.getInt("aqid"),
                                                obj2.getString("adesc"),
                                                obj2.getString("adesc_hindi"),
                                                "",
                                                obj2.getInt("acorrect")
                                            )
                                            options.add(option)
                                        }

                                        val qset = Question(
                                            item.getInt("qid"),
                                            item.getString("qdesc"),
                                            item.getString("qdesc_hindi"),
                                            item.getString("qtype"),
                                            item.getString("qcat"),
                                            item.getString("qattach"),
                                            item.getString("qsummary"),
                                            item.getInt("qdifficulty_level"),
                                            item.getString("qformat"),
                                            item.getString("qfile"),
                                            1,
                                            options
                                        )
                                        wset.add(qset)
                                    }
                                }

                                // attachment
                                if(!dataObj.getString("attachment").equals("false",true)) {
                                    arr = dataObj.getJSONArray("attachment")
                                    if(arr.length() > 0) {
                                        for(i in 0 until arr.length()) {
                                            val item = arr.get(i) as JSONObject
                                            val attachment = Attachment(
                                                item.getInt("qid"),
                                                item.getString("type"),
                                                item.getString("data"),
                                                item.getString("filename")
                                            )
                                            attachmentList.add(attachment)
                                        }
                                    }
                                }
                                user?.coins = dataObj.getInt("coins")
                                userRepository.setDBUser(user!!)
                                setSuccess(wset,"shuffleWild")
                            } else {
                                setError(responseObj.getString("message"), ALERT)
                            }
                        }
                    } catch (e: Exception) {
                        setLoading(false)
                        lock(false)
                        setError("${e.message}", ALERT)
                    }
                }
            } else {
                setError("$coins GSP Coins required to shuffle", ALERT)
            }
        }
    }

    fun offerPurchase(offer: String, coins: Int) {
        if(user != null && user?.user_id!! > 0) {
            if(coins <= user?.coins!!) {
                viewModelScope.launch {
                    setLoading(true)
                    lock(true)
                    try {
                        val response = quizRepository.purchaseOffer(user?.user_id!!, coins)
                        setLoading(false)
//                        lock(false)
                        if (response != null) {
                            val responseObj = JSONObject(response)
                            if(responseObj.getInt("status") == 1) {
                                val dataObj = responseObj.getJSONObject("data")
                                when {
                                    offer.equals("1",true) -> {
                                        user?.life = user?.life!!.plus(1)
                                    }
                                    offer.equals("5",true) -> {
                                        user?.life = user?.life!!.plus(5)
                                    }
                                    offer.equals("1h",true) -> {
                                        user?.life = user?.life!!.plus(5)
                                    }
                                }

                                val maxChance = userRepository.config("max-game-chance")
                                var maxLife = 5
                                if(maxChance.isNotEmpty() && !maxChance.equals("0",true)) {
                                    maxLife = maxChance.toInt()
                                }
                                if(user?.life!! > maxLife) {
                                    user?.life = maxLife
                                }
                                user?.update_at = System.currentTimeMillis()

                                user?.coins = dataObj.getInt("coins")
                                userRepository.setDBUser(user!!)
                                setSuccess(offer,"offerPurchase")
                            } else {
                                setError("quizdata:${responseObj.getString("message")}", ALERT)
                            }
                        }
                    } catch (e: Exception) {
                        setLoading(false)
                        lock(false)
                        setError("quizdata:${e.message}", ALERT)
                    }
                }
            } else {
                setError("quizdata:$coins GSP Coins required", ALERT)
            }
        }
    }

    private fun getWild() {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                try {
                    val response = quizRepository.getWildQuiz(user?.user_id!!, 0)
                    if (response != null) {
                        val responseObj = JSONObject(response)
                        if(responseObj.getInt("status") == 1) {
                            val dataObj = responseObj.getJSONObject("data")
                            var arr = dataObj.getJSONArray("qset")
                            if(arr.length() > 0) {
                                wset.clear()
                                for(i in 0 until arr.length()) {
                                    val item = arr.get(i) as JSONObject

                                    // setting options
                                    val optionsArr: JSONArray = item.getJSONArray("options")
                                    val options: MutableList<Option> = ArrayList()
                                    for (j in 0 until optionsArr.length()) {
                                        val obj2 = optionsArr[j] as JSONObject
                                        val option = Option(
                                            obj2.getInt("aid"),
                                            obj2.getInt("aqid"),
                                            obj2.getString("adesc"),
                                            obj2.getString("adesc_hindi"),
                                            "",
                                            obj2.getInt("acorrect")
                                        )
                                        options.add(option)
                                    }

                                    val qset = Question(
                                        item.getInt("qid"),
                                        item.getString("qdesc"),
                                        item.getString("qdesc_hindi"),
                                        item.getString("qtype"),
                                        item.getString("qcat"),
                                        item.getString("qattach"),
                                        item.getString("qsummary"),
                                        item.getInt("qdifficulty_level"),
                                        item.getString("qformat"),
                                        item.getString("qfile"),
                                        1,
                                        options
                                    )
                                    wset.add(qset)
                                }
                            }

                            // attachment
                            if(!dataObj.getString("attachment").equals("false",true)) {
                                arr = dataObj.getJSONArray("attachment")
                                if(arr.length() > 0) {
                                    for(i in 0 until arr.length()) {
                                        val item = arr.get(i) as JSONObject
                                        val attachment = Attachment(
                                            item.getInt("qid"),
                                            item.getString("type"),
                                            item.getString("data"),
                                            item.getString("filename")
                                        )
                                        attachmentList.add(attachment)
                                    }
                                }
                            }
                            user?.coins = dataObj.getInt("coins")
                            userRepository.setDBUser(user!!)
                        } else {
                            setError(responseObj.getString("message"), TAG)
                        }
                    }
                } catch (e: Exception) {
                    setError("${e.message}", TAG)
                }
            }
        }
    }


    fun finishQuiz() {
        viewModelScope.launch {
            delay(TIME_MOVE_TO_NEXT)
            isPopupOpen = true
            if(!preferences.previewMode) {
                saveData()
            }
            setSuccess("displayFinish","quizStatus")
        }
    }

    private fun saveData() {
        if(user != null && user?.user_id!! > 0) {
            viewModelScope.launch {
                // update user data
                if (user?.life!! > 0 && System.currentTimeMillis() > preferences.timestampLife) {
                    user?.life = user?.life!!.minus(1)
                    user?.update_at = System.currentTimeMillis()
                    userRepository.setDBUser(user!!)
                }

                val params = HashMap<String, String>()
                params["start_time"] = start_time
                params["end_time"] =
                    milisToFormat(Calendar.getInstance().timeInMillis, "yyyy-MM-dd HH:mm:ss")
                params["stats_data"] = Gson().toJson(statsData)
                params["life_lines"] = Gson().toJson(lifeline)
                params["score"] = score.values.sum().toString()
                params["xp"] = xp.values.sum().toString()
                params["user_id"] = user?.user_id!!.toString()
                params["question_set"] = Gson().toJson(questionSet)

                val usr = userRepository.getDBUser()
                params["played_qid"] = usr?.played_qid!!

                if (currentq.qno in 1..5) {
                    params["level"] = "1"
                }
                if (currentq.qno in 6..10) {
                    params["level"] = "2"
                }
                if (currentq.qno in 11..15) {
                    params["level"] = "3"
                }

                tag("$TAG, $params")
                quizRepository.saveQuiz(params)

                // reward scratchcard
                if(currentq.qno == 15) {
                    rewardRepository.setScratchcard(user?.user_id!!,3)
                }
            }
        }
    }

    fun lock(status: Boolean) {
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
        tag("$TAG, questionTimerStart")
        questionTimer = object : CountDownTimer(i, 10) {
            override fun onTick(l: Long) {
                timerRemainingTime = l
                setSuccess(l, "questionTimer")
            }

            override fun onFinish() {
                setSuccess("timeUp","quizStatus")
                questionTimerCancel()
                finishQuiz()
            }
        }
        (questionTimer as CountDownTimer).start()
        viewModelScope.launch {
            tag("$TAG, questionTimerStart delay")
            delay(200)
            setSuccess("timeStart","quizStatus")
        }
    }

    fun questionTimerCancel() {
        questionTimer?.cancel()
    }

    fun onBackPressed() {
        tag("$TAG, onBackPressed: $lock")
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
        tag("$TAG, onResume, isPause: $isPause, isResumable: $isResumable, isPopupOpen: $isPopupOpen")
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
        tag("$TAG, fset: $currentq")
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