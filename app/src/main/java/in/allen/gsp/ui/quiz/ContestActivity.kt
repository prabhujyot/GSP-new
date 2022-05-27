package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Question
import `in`.allen.gsp.databinding.ActivityContestBinding
import `in`.allen.gsp.databinding.OptionLinearBinding
import `in`.allen.gsp.databinding.OptionSpellingBinding
import `in`.allen.gsp.databinding.OptionTrueFalseBinding
import `in`.allen.gsp.utils.*
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.DI
import org.kodein.di.DIAware
import org.kodein.di.instance
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class ContestActivity : AppCompatActivity(), DIAware {

    private val TAG = ContestActivity::class.java.name
    private lateinit var binding: ActivityContestBinding
    private lateinit var bindingSingleChoice: OptionLinearBinding
    private lateinit var bindingTrueFalse: OptionTrueFalseBinding
    private lateinit var viewModel: ContestViewModel

    override val di: DI by lazy { (applicationContext as DIAware).di }
    private val factory:ContestViewModelFactory by instance()

    // animations
    private lateinit var animFadeIn: Animation
    private lateinit var animBlink: Animation

    private lateinit var mp: MediaPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideStatusBar()

        binding = DataBindingUtil.setContentView(this, R.layout.activity_contest)
        viewModel = ViewModelProvider(this, factory)[ContestViewModel::class.java]

        initAnimations()

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()
    }

    override fun onBackPressed() {
        viewModel.onBackPressed()
    }

    override fun onPause() {
        viewModel.onPause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }

    private fun initAnimations() {
        animFadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fadein)
        animBlink = AnimationUtils.loadAnimation(applicationContext, R.anim.blink)
        binding.quizLayout.show(false)
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
                when (it.message) {
                    "user" -> {
                        viewModel.quizData()
                    }

                    "lock" -> {
                        tag("$TAG lock: ${it.data}")
                        binding.lock.show(it.data as Boolean)
                    }

                    "downloadAttachment" -> {
                        if(viewModel.attachmentList.size > 0) {
                            for (file in viewModel.attachmentList) {
                                downloadFile(file.filename, file.qid.toString())
                            }
                        }
                        initQuiz()
                    }

                    "questionTimer" -> {
                        if (it.data is Long) {
                            val hms = String.format(
                                "%02d",
                                TimeUnit.MILLISECONDS.toSeconds(it.data) - TimeUnit.MINUTES.toSeconds(
                                    TimeUnit.MILLISECONDS.toMinutes(it.data)
                                )
                            )
                            binding.textTimer.text = hms
                            binding.progressTimer.progress =
                                (binding.progressTimer.max - it.data).toInt()
                        }
                    }

                    "multiplierTimer" -> {
                        if (it.data is Long) {
                            binding.progressMultiplier.progress = it.data.toInt()
                        }
                    }

                    "attachmentTimer" -> {
                        if (it.data is Long) {
//                            binding.layoutAttachment.progressTimer.progress = it.data.toInt()
                        }
                    }

                    "calculateScore" -> {
                        if (it.data is Long) {
                            binding.textScore.text = "${it.data}"
                            viewModel.moveToNext()
                        }
                    }

                    "quizStatus" -> {
                        if (it.data is String) {
                            if (it.data.equals("setQuestion", true)) {
                                setQuestion(viewModel.currentq)
                                viewModel.displayQuestion()
                            }
                            if (it.data.equals("displayQuestion", true)) {
                                displayQuestion(viewModel.currentq)
                            }
                            if (it.data.equals("displayAttachment", true)) {
                                displayAttachment(viewModel.currentq)
                            }
                            if (it.data.equals("closeAttachment", true)) {
//                                binding.layoutAttachment.btnClose.performClick()
                            }
                            if (it.data.equals("displayOption", true)) {
                                displayOption(viewModel.currentq)
                            }
                            if (it.data.equals("displayFinish", true)) {
                                displayFinish()
                            }
                            if (it.data.equals("onBackPressed", true)) {
                                onPause()
                                confirmDialog(
                                    "Exit!",
                                    "Are you sure want to exit, you will lose all your progress",
                                    {
                                        viewModel.setSuccess("exit", "quizStatus")
                                    },
                                    {
                                        viewModel.isPopupOpen = false
                                        onResume()
                                    }
                                )
                            }
                            if (it.data.equals("exit", true)) {
                                try {
                                    if (mp.isPlaying) {
                                        mp.stop()
                                        mp.release()
                                    }
                                } catch (e: Exception) {}
                                finish()
                            }
                        }
                    }

                    "validateLifeline" -> {
                        if (it.data is String) {
                            onPause()
                            displayLifeline(it.data)
                        }
                    }

                    "performLifeline" -> {
                        if (it.data is String) {
                            if (it.data.equals("fifty_fifty", true)) {
                                val temp = ArrayList<Any>()
                                if (bindingSingleChoice.optionA.tag != 1) {
                                    temp.add(bindingSingleChoice.optionA)
                                }
                                if (bindingSingleChoice.optionB.tag != 1) {
                                    temp.add(bindingSingleChoice.optionB)
                                }
                                if (bindingSingleChoice.optionC.tag != 1) {
                                    temp.add(bindingSingleChoice.optionC)
                                }
                                if (bindingSingleChoice.optionD.tag != 1) {
                                    temp.add(bindingSingleChoice.optionD)
                                }

                                val random = Random()

                                val intSet = HashSet<Int>()
                                while (intSet.size < 2) {
                                    intSet.add(random.nextInt(temp.size))
                                }
                                val iter = intSet.iterator()
                                while (iter.hasNext()) {
                                    val i = iter.next()
                                    val v = temp[i] as AppCompatTextView
                                    v.setTextColor(
                                        ResourcesCompat.getColor(
                                            resources,
                                            R.color.disable,
                                            null
                                        )
                                    )
                                    v.isClickable = false
                                }
                                onResume()
                            }

                            if (it.data.equals("double_dip", true)) {
                                viewModel.isDoubleDip = true
                                onResume()
                            }

                            if (it.data.equals("flip", true)) {
                                viewModel.flipQuestion()
                            }

                            if (it.data.equals("quit", true)) {
                                viewModel.finishQuiz()
                            }
                        }
                    }
                }
            }
        })
    }

    fun btnActionContest(view: View) {
        when (view.id) {
            R.id.btnDoubleDip -> {
                viewModel.validateLifeline("double_dip")
            }
            R.id.btnFiftyFifty -> {
                viewModel.validateLifeline("fifty_fifty")
            }
            R.id.btnFlip -> {
                viewModel.validateLifeline("flip")
            }
            R.id.btnQuit -> {
                viewModel.validateLifeline("quit")
            }
            R.id.btnTranslate -> {
                if(viewModel.lang.equals("eng", true)) {
                    viewModel.lang = "hindi"
                } else {
                    viewModel.lang = "eng"
                }

                switchLanguage(viewModel.currentq)
            }
        }
    }


    private fun initQuiz() {
        tag("$TAG initQuiz")
        binding.quizLayout.show(false)
        initLifelines()
        binding.textScore.text = "0"

        viewModel.currentq = viewModel.qset[viewModel.index]
        viewModel.setSuccess("setQuestion", "quizStatus")
    }

    private fun initLifelines() {
        tag("$TAG initLifelines")
        viewModel.lifeline["double_dip"] = true
        viewModel.lifeline["fifty_fifty"] = true
        viewModel.lifeline["flip"] = true
        viewModel.lifeline["quit"] = true

        stateLifeline(binding.btnDoubleDip,true)
        stateLifeline(binding.btnFiftyFifty,true)
        stateLifeline(binding.btnQuit,true)
        stateLifeline(binding.btnFlip,true)
        if(viewModel.fset.size < 3) {
            stateLifeline(binding.btnFlip,false)
        }
        binding.quizLayout.show()
        viewModel.setLoading(false)
    }

    private fun stateLifeline(view: View, enable: Boolean) {
        view.isEnabled = enable
        view.isClickable = enable
        if(enable) {
            (view as ImageButton).setColorFilter(
                ResourcesCompat.getColor(resources, R.color.black, null),
                PorterDuff.Mode.SRC_ATOP
            )
        } else {
            (view as ImageButton).setColorFilter(
                ResourcesCompat.getColor(resources, R.color.disable, null),
                PorterDuff.Mode.SRC_ATOP
            )
        }
    }

    private fun displayLifeline(type: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_lifeline, null)
        dialogBuilder.setView(dialogView)

        val ttle: TextView = dialogView.findViewById(R.id.title)
        val alertDialog = dialogBuilder.create()

        if (type.equals("fifty_fifty", true)) {
            stateLifeline(binding.btnFiftyFifty, false)
            stateLifeline(binding.btnDoubleDip, false)
            ttle.text = "Fifty Fifty"
        }

        if (type.equals("double_dip", true)) {
            stateLifeline(binding.btnDoubleDip, false)
            stateLifeline(binding.btnFiftyFifty, false)
            ttle.text = "Double Dip"
        }

        if (type.equals("flip", true)) {
            stateLifeline(binding.btnFlip, false)
            ttle.text = "Flip the question"
        }

        if (type.equals("quit", true)) {
            stateLifeline(binding.btnQuit, false)
            ttle.text = "Quit"
        }

        //In Android, AlertDialog insert into another container, to avoid that, we need to make back ground transparent
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.setCancelable(false)

        if (alertDialog.window != null)
            alertDialog.window!!.attributes.windowAnimations = R.style.SlidingDialogAnimation

        alertDialog.show()
        viewModel.isPopupOpen = true
        lifecycleScope.launch {
            delay(viewModel.TIME_POPUP)
            if(alertDialog.isShowing) {
                alertDialog.dismiss()
                viewModel.isPopupOpen = false
                viewModel.setSuccess(type, "performLifeline")
            }
        }
    }

    private fun setQuestion(currentQ: Question) {
        tag("$TAG setQuestion ${currentQ.qno} ${viewModel.index}")
        viewModel.lock(true)

        viewModel.questionTimerCancel()

        binding.textTimer.text = currentQ.qTime.toString()
        binding.progressTimer.max = currentQ.qTime * 1000
        binding.progressTimer.progress = 1

        binding.progressMultiplier.max = 10 * 1000
        binding.progressMultiplier.progress = 10 * 1000

        binding.qno.text = "Q.No.: ${currentQ.qno}"

        binding.layoutOption.clearAnimation()
        binding.layoutOption.visibility = View.INVISIBLE

        switchLanguage(currentQ)

        // reset attachment view
        resetAttachmantBackground()
    }

    private fun switchLanguage(currentQ: Question) {
        if(viewModel.lang.equals("eng", true)) {
            binding.question.text = currentQ.qdesc
        } else {
            if (currentQ.qdesc_hindi.isEmpty()
                || currentQ.qdesc_hindi.equals("null", true)
                || currentQ.option[0].adesc_hindi.isEmpty()
                || currentQ.option[0].adesc_hindi.equals("null", true)
            ) {
                viewModel.lang = "eng"
                binding.question.text = currentQ.qdesc
            } else {
                binding.question.text = currentQ.qdesc_hindi
            }
        }

        when {
            currentQ.qformat.equals("true_false", true) -> {
                setTrueFalse(currentQ)
            }
            currentQ.qformat.equals("spellings", true) -> {
                setSpellings(currentQ)
            }
            else -> {
                setSingleChoice(currentQ)
            }
        }
    }

    private fun setSingleChoice(currentQ: Question) {
        stateLifeline(binding.btnFiftyFifty,viewModel.lifeline["fifty_fifty"]!!)
        stateLifeline(binding.btnDoubleDip,viewModel.lifeline["double_dip"]!!)
        if(viewModel.fset.size > 2) {
            stateLifeline(binding.btnFlip,viewModel.lifeline["flip"]!!)
        }

        binding.layoutOption.removeAllViews()
        bindingSingleChoice = DataBindingUtil.inflate(
            layoutInflater, R.layout.option_linear, binding.layoutOption, false
        )

        for(o in currentQ.option.indices) {
            if(o == 0) {
                if (viewModel.lang.equals("eng", true)) {
                    bindingSingleChoice.optionA.text = currentQ.option[o].adesc
                } else {
                    bindingSingleChoice.optionA.text = currentQ.option[o].adesc_hindi
                }
                bindingSingleChoice.optionA.tag = currentQ.option[o].acorrect
                bindingSingleChoice.optionA.setOnClickListener {
                    clickOption(it)
                }
            }
            if(o == 1) {
                if (viewModel.lang.equals("eng", true)) {
                    bindingSingleChoice.optionB.text = currentQ.option[o].adesc
                } else {
                    bindingSingleChoice.optionB.text = currentQ.option[o].adesc_hindi
                }
                bindingSingleChoice.optionB.tag = currentQ.option[o].acorrect
                bindingSingleChoice.optionB.setOnClickListener {
                    clickOption(it)
                }
            }
            if(o == 2) {
                if (viewModel.lang.equals("eng", true)) {
                    bindingSingleChoice.optionC.text = currentQ.option[o].adesc
                } else {
                    bindingSingleChoice.optionC.text = currentQ.option[o].adesc_hindi
                }
                bindingSingleChoice.optionC.tag = currentQ.option[o].acorrect
                bindingSingleChoice.optionC.setOnClickListener {
                    clickOption(it)
                }
            }
            if(o == 3) {
                if (viewModel.lang.equals("eng", true)) {
                    bindingSingleChoice.optionD.text = currentQ.option[o].adesc
                } else {
                    bindingSingleChoice.optionD.text = currentQ.option[o].adesc_hindi
                }
                bindingSingleChoice.optionD.tag = currentQ.option[o].acorrect
                bindingSingleChoice.optionD.setOnClickListener {
                    clickOption(it)
                }
            }
        }

        binding.layoutOption.addView(bindingSingleChoice.root)
    }

    private fun clickOption(view: View) {
        viewModel.stopTimer()

        view.background = ResourcesCompat.getDrawable(resources, R.drawable.bg_btn_blue, null)
        (view as TextView).setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))

        lifecycleScope.launch {
            delay(viewModel.TIME_DELAY)
            if(view.tag == 1) {
                viewModel.isDoubleDip = false
                view.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_btn_green,
                    null
                )

                val duration = viewModel.currentq.qTime.minus(
                    binding.textTimer.text.toString().toInt()
                )

                viewModel.calculateScore(duration)
            } else {
                if(viewModel.isDoubleDip) {
                    viewModel.isDoubleDip = false
                    onPause()
                    view.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.bg_btn_gray,
                        null
                    )
                    view.setTextColor(ResourcesCompat.getColor(resources, R.color.disable, null))
                    onResume()
                } else {
                    view.background = ResourcesCompat.getDrawable(
                        resources,
                        R.drawable.bg_btn_red,
                        null
                    )
                    viewModel.finishQuiz()
                }
            }
            view.startAnimation(animBlink)
        }
    }

    private fun setTrueFalse(currentQ: Question) {
        stateLifeline(binding.btnFiftyFifty,false)
        stateLifeline(binding.btnDoubleDip,false)
        if(viewModel.fset.size > 2) {
            stateLifeline(binding.btnFlip,viewModel.lifeline["flip"]!!)
        }

        binding.layoutOption.removeAllViews()
        bindingTrueFalse = DataBindingUtil.inflate(
            layoutInflater, R.layout.option_true_false, binding.layoutOption, false
        )

        for(o in currentQ.option.indices) {
            if(o == 0) {
                if (viewModel.lang.equals("eng", true)) {
                    bindingTrueFalse.optionA.text = currentQ.option[o].adesc
                } else {
                    bindingTrueFalse.optionA.text = currentQ.option[o].adesc_hindi
                }
                bindingTrueFalse.optionA.tag = currentQ.option[o].acorrect
                bindingTrueFalse.optionA.setOnClickListener {
                    clickOption(it)
                }
            }
            if(o == 1) {
                if (viewModel.lang.equals("eng", true)) {
                    bindingTrueFalse.optionB.text = currentQ.option[o].adesc
                } else {
                    bindingTrueFalse.optionB.text = currentQ.option[o].adesc_hindi
                }
                bindingTrueFalse.optionB.tag = currentQ.option[o].acorrect
                bindingTrueFalse.optionB.setOnClickListener {
                    clickOption(it)
                }
            }
        }
        binding.layoutOption.addView(bindingTrueFalse.root)
    }

    private fun setSpellings(currentQ: Question) {
        stateLifeline(binding.btnFiftyFifty, false)
        stateLifeline(binding.btnDoubleDip, false)
        if(viewModel.fset.size > 2) {
            stateLifeline(binding.btnFlip,viewModel.lifeline["flip"]!!)
        }

        binding.layoutOption.removeAllViews()
        val bindingSpelling: OptionSpellingBinding = DataBindingUtil.inflate(
            layoutInflater, R.layout.option_spelling, binding.layoutOption, false
        )

        val shuffled = shuffleString(currentQ.option[0].adesc)
        for (element in shuffled) {
            val textView2 = TextView(this)
            val params2 = LinearLayout.LayoutParams(100, 100)
            textView2.layoutParams = params2
            textView2.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            textView2.setTypeface(textView2.typeface, Typeface.BOLD)
            textView2.gravity = Gravity.CENTER
            textView2.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            textView2.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.bg_circle_grey,
                null
            )
            textView2.text = null
            textView2.isAllCaps = true
            textView2.tag = false
            bindingSpelling.spelLayout.addView(textView2)
            textView2.setOnClickListener { v ->
                if (v.tag as Boolean) {
                    val txt = (v as TextView).text.toString().trim { it <= ' ' }
                    for (j in 0 until bindingSpelling.charLayout.childCount) {
                        if (txt.equals(
                                (bindingSpelling.charLayout.getChildAt(j) as TextView).text.toString()
                                    .trim { it <= ' ' }, ignoreCase = true
                            ) && !(bindingSpelling.charLayout.getChildAt(j).tag as Boolean)
                        ) {
                            bindingSpelling.charLayout.getChildAt(j).visibility = View.VISIBLE
                            bindingSpelling.charLayout.getChildAt(j).tag = true
                            v.text = null
                            v.background = ResourcesCompat.getDrawable(
                                resources,
                                R.drawable.bg_circle_grey,
                                null
                            )
                            v.setTag(false)
                            break
                        }
                    }
                }
            }
            val textView = TextView(this)
            val params = LinearLayout.LayoutParams(90, 90)
            textView.layoutParams = params
            textView.text = element.toString()
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            textView.setTypeface(textView.typeface, Typeface.BOLD)
            textView.gravity = Gravity.CENTER
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            textView.background = ResourcesCompat.getDrawable(
                resources,
                R.drawable.bg_circle_black,
                null
            )
            textView.tag = true
            textView.isAllCaps = true
            bindingSpelling.charLayout.addView(textView)
            textView.setOnClickListener { v ->
                var spelling = ""
                if (v.tag as Boolean) {
                    val txt = (v as TextView).text.toString().trim { it <= ' ' }
                    for (j in 0 until bindingSpelling.spelLayout.childCount) {
                        if (!(bindingSpelling.spelLayout.getChildAt(j).tag as Boolean)) {
                            bindingSpelling.spelLayout.getChildAt(j).tag = true
                            (bindingSpelling.spelLayout.getChildAt(j) as TextView).text = txt
                            bindingSpelling.spelLayout.getChildAt(j).background =
                                ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.bg_circle_blue,
                                    null
                                )
                            v.setTag(false)
                            v.setVisibility(View.INVISIBLE)
                            break
                        }
                    }
                }
                for (k in 0 until bindingSpelling.spelLayout.childCount) {
                    spelling += (bindingSpelling.spelLayout.getChildAt(k) as TextView).text.toString()
                        .trim { it <= ' ' }
                }
                tag("spelling: $spelling length: ${spelling.length}, ${shuffled.length} :: ${spelling.length == shuffled.length}")
                // check answer
                if(spelling.length == shuffled.length) {
                    // lock
                    viewModel.lock(true)
                    //stop progessbar
                    viewModel.questionTimerCancel()
                    viewModel.multiplierTimerCancel()

                    lifecycleScope.launch {
                        delay(viewModel.TIME_DELAY)
                        if(spelling.equals(currentQ.option[0].adesc,true)) {
                            for (k in 0 until bindingSpelling.spelLayout.childCount) {
                                (bindingSpelling.spelLayout.getChildAt(k) as TextView).background = ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.bg_circle_green,
                                    null
                                )
                            }

                            val duration = viewModel.currentq.qTime.minus(
                                binding.textTimer.text.toString().toInt()
                            )

                            viewModel.calculateScore(duration)
                        } else {
                            for (k in 0 until bindingSpelling.spelLayout.childCount) {
                                (bindingSpelling.spelLayout.getChildAt(k) as TextView).background = ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.bg_circle_red,
                                    null
                                )
                            }
                            viewModel.finishQuiz()
                        }
                        bindingSpelling.spelLayout.startAnimation(animBlink)
                    }
                }
            }
        }
        binding.layoutOption.addView(bindingSpelling.root)
    }


    private fun displayQuestion(currentQ: Question) {
        tag("displayQuestion: ${currentQ.qno}")
        viewModel.displayAttachment()
    }

    private fun displayOption(currentQ: Question) {
        tag("displayOption: ${currentQ.qno}")
        binding.layoutOption.startAnimation(animFadeIn)
        viewModel.startTimer()
    }

    private fun displayFinish() {
        alertDialog("", "Thank you for the participation!") {
            finish()
        }
    }

    private fun displayAttachment(currentQ: Question) {
        tag("displayAttachment: ${currentQ.qno}")

        val image = binding.layoutAttachment.findViewById<ImageView>(R.id.image)
        val video = binding.layoutAttachment.findViewById<VideoView>(R.id.video)
        val close = binding.layoutAttachment.findViewById<ImageButton>(R.id.btnClose)
        close.setOnClickListener {
            attachmentComplete(currentQ.qtype)
        }

        image.show(false)
        video.show(false)

        var url = currentQ.qattach.trim()
        mp = MediaPlayer()

        val localFile = File(getExternalFilesDir("quiz"), currentQ.qid.toString())
        if(localFile.exists()) {
            url = localFile.path
        }

        if(url.isNotEmpty() && !url.equals("null", true)) {
            tag("$TAG url: $url")
            when(currentQ.qtype) {
                "audio" -> {
                    try {
                        // Set the media player audio stream type
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            mp.setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()
                            )
                        } else {
                            mp.setAudioStreamType(AudioManager.STREAM_MUSIC)
                        }
                        mp.setDataSource(url)
                        mp.prepare()
                        if(!mp.isPlaying) {
                            mp.start()
                        }
                        mp.setOnCompletionListener {
                            attachmentComplete("audio")
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    image.loadImage(
                        "${BuildConfig.BASE_URL}gsp-admin/uploads/audio-quiz.jpg", false, true
                    )
                    image.show()
                    close.show()
                }
                "video" -> {
                    video.setVideoPath(url)
                    video.show()
                    close.show()
                    video.start()
                    video.setOnCompletionListener {
                        attachmentComplete("video")
                    }
                }
                else -> {
                    image.loadImage(
                        url,false, true
                    )
                    image.show()
                    lifecycleScope.launch {
                        delay(viewModel.TIME_POPUP)
                        attachmentComplete("image")
                    }
                }
            }
        }
    }

    private fun attachmentComplete(attachmentType: String) {
        tag("attachmentComplete: $attachmentType")
        val video = binding.layoutAttachment.findViewById<VideoView>(R.id.video)

        when(attachmentType) {
            "audio" -> {
                if (mp.isPlaying) {
                    mp.stop()
                }
                mp.release()
                resetAttachmantBackground()
            }
            "video" -> {
                if(video.visibility == View.VISIBLE) {
                    video.stopPlayback()
                }
                resetAttachmantBackground()
            }
        }

        viewModel.displayOption()
    }

    private fun resetAttachmantBackground() {
        val image = binding.layoutAttachment.findViewById<ImageView>(R.id.image)
        val close = binding.layoutAttachment.findViewById<ImageButton>(R.id.btnClose)
        val video = binding.layoutAttachment.findViewById<VideoView>(R.id.video)

        close.show(false)
        video.show(false)
        image.show()

        if(image.visibility == View.VISIBLE) {
            image.setImageDrawable(ResourcesCompat.getDrawable(resources,R.drawable.bg_play_o,null))
        }
    }

    private fun downloadFile(sourceFileName: String, destinationFileName: String) {
        val localFile = File(getExternalFilesDir("quiz"), destinationFileName)
        if(!localFile.exists()) {
            viewModel.saveFile(localFile, sourceFileName)
        }
    }

}