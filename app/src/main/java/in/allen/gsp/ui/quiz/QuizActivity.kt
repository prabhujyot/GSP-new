package `in`.allen.gsp.ui.quiz

import `in`.allen.gsp.BuildConfig
import `in`.allen.gsp.R
import `in`.allen.gsp.data.entities.Question
import `in`.allen.gsp.data.repositories.UserRepository
import `in`.allen.gsp.data.services.LifeService
import `in`.allen.gsp.databinding.ActivityQuizBinding
import `in`.allen.gsp.databinding.OptionLinearBinding
import `in`.allen.gsp.databinding.OptionSpellingBinding
import `in`.allen.gsp.databinding.OptionTrueFalseBinding
import `in`.allen.gsp.utils.*
import android.animation.ObjectAnimator
import android.content.Intent
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
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.icon_life.view.*
import kotlinx.android.synthetic.main.toolbar.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kodein.di.KodeinAware
import org.kodein.di.android.kodein
import org.kodein.di.generic.instance
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


class QuizActivity : AppCompatActivity(), KodeinAware {

    private val TAG = QuizActivity::class.java.name
    private lateinit var binding: ActivityQuizBinding
    private lateinit var bindingSingleChoice: OptionLinearBinding
    private lateinit var bindingTrueFalse: OptionTrueFalseBinding
    private lateinit var viewModel: QuizViewModel

    override val kodein by kodein()
    private val factory:QuizViewModelFactory by instance()

    // bottomsheets
    private lateinit var categorySheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var attachmentSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var finishSheetBehavior: BottomSheetBehavior<FrameLayout>
    private lateinit var offersSheetBehavior: BottomSheetBehavior<FrameLayout>

    // animations
//    private lateinit var animZoomIn: Animation
    private lateinit var animFadeIn: Animation
//    private lateinit var animFadeOut: Animation
    private lateinit var animBlink: Animation
//    private lateinit var animMoveLeft: Animation
//    private lateinit var animMoveRight: Animation

    // Question Table
    private val questionTable = ArrayList<View>()

    private lateinit var mp: MediaPlayer

    private val userRepository: UserRepository by instance()
    private val preferences: AppPreferences by instance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_quiz)
        viewModel = ViewModelProvider(this, factory).get(QuizViewModel::class.java)

        setSupportActionBar(myToolbar)
        btnBack.setOnClickListener {
            onBackPressed()
        }

        val bg = ResourcesCompat.getDrawable(resources, R.drawable.bg_play_o, null)
        if (bg != null) {
            bg.alpha = 20
            binding.bgLayout.setImageDrawable(bg)
        }

        initBottomSheets()
        initAnimations()

        observeLoading()
        observeError()
        observeSuccess()
        viewModel.userData()
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val lifeMenu = menu?.findItem(R.id.menu_life)
        val itemRoot = lifeMenu?.actionView
        val lp = ConstraintLayout.LayoutParams(50,50)
        itemRoot?.layoutParams = lp

        itemRoot?.life?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        itemRoot?.life?.setTypeface(itemRoot.life?.typeface, Typeface.BOLD)

        userRepository.userLife.observe(this, {
            if(it != null) {
                viewModel.user?.life = it["life"]!!.toInt()
                itemRoot?.life?.text = "${it["life"]}"
                binding.layoutOffers.layoutLife.life.text = "${it["life"]}"

                if(System.currentTimeMillis() < preferences.timestampLife) {
                    itemRoot?.life?.text = getString(R.string.infinity)
                    binding.layoutOffers.layoutLife.life.text = getString(R.string.infinity)
                    it["life"] = 0
                }

                if (it["life"]!!.toInt() == 5) {
                    binding.layoutOffers.lifeTimer.text = "Full"
                } else {
                    it["remaining"]?.let { it1 ->
                        val m =
                            TimeUnit.MILLISECONDS.toMinutes(it1) - TimeUnit.HOURS.toMinutes(
                                TimeUnit.MILLISECONDS.toHours(it1)
                            )
                        val s =
                            TimeUnit.MILLISECONDS.toSeconds(it1) - TimeUnit.MINUTES.toSeconds(
                                TimeUnit.MILLISECONDS.toMinutes(it1)
                            )
                        val hms = String.format("%02d:%02d", m, s)
                        binding.layoutOffers.lifeTimer.text = "$hms"
                    }
                }
            }
        })

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.play, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if(id == R.id.menu_translate) {
            if(viewModel.lang.equals("eng", true)) {
                viewModel.lang = "hindi"
            } else {
                viewModel.lang = "eng"
            }

            switchLanguage(viewModel.currentq)
        }

        return super.onOptionsItemSelected(item)
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
        hideSystemUI()
        viewModel.onResume()
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
                            if(it1.startsWith("quizdata:")) {
                                action = "finish"
                                str = it1.removePrefix("quizdata:")
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
                            binding.layoutAttachment.progressTimer.progress = it.data.toInt()
                        }
                    }

                    "shuffleWild" -> {
                        tag("shuffleWild")
                        binding.layoutCategory.cat1Text.text = viewModel.wset[0].qcat
                        binding.layoutCategory.cat1.tag = viewModel.wset[0]

                        binding.layoutCategory.cat2Text.text = viewModel.wset[1].qcat
                        binding.layoutCategory.cat2.tag = viewModel.wset[1]

                        binding.layoutCategory.cat3Text.text = viewModel.wset[2].qcat
                        binding.layoutCategory.cat3.tag = viewModel.wset[2]

                        if(viewModel.attachmentList.size > 0) {
                            for (file in viewModel.attachmentList) {
                                downloadFile(file.filename, file.qid.toString())
                            }
                        }
                    }

                    "offerPurchase" -> {
                        if(it.data is String) {
                            when {
                                it.data.equals("1h",true) -> {
                                    val minutes = 60
                                    val timestamp = System.currentTimeMillis().plus(minutes.times(60).times(1000))
                                    preferences.timestampLife = timestamp
                                }
                            }
                            startService()

                            lifecycleScope.launch {
                                delay(viewModel.TIME_DELAY)
                                viewModel.quizData()
                            }
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
                            if(it.data.equals("displayWild",true)) {
                                displayWild()
                            }
                            if (it.data.equals("closeAttachment", true)) {
                                binding.layoutAttachment.btnClose.performClick()
                            }
                            if (it.data.equals("displayOption", true)) {
                                displayOption(viewModel.currentq)
                            }
                            if (it.data.equals("displayFinish", true)) {
                                displayFinish()
                            }
                            if(it.data.equals("showOffers",true)) {
                                displayOffers()
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

    fun btnActionPlay(view: View) {
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
        }
    }

    private fun initBottomSheets() {
        categorySheetBehavior = BottomSheetBehavior.from(binding.layoutCategory.bottomSheetCategory)
        attachmentSheetBehavior = BottomSheetBehavior.from(binding.layoutAttachment.bottomSheetAttachment)
        finishSheetBehavior = BottomSheetBehavior.from(binding.layoutFinish.bottomSheetFinish)
        offersSheetBehavior = BottomSheetBehavior.from(binding.layoutOffers.bottomSheetOffers)
        categorySheetBehavior.isDraggable = false
        attachmentSheetBehavior.isDraggable = false
        finishSheetBehavior.isDraggable = false
        offersSheetBehavior.isDraggable = false

        binding.quizLayout.show(false)
    }

    private fun initAnimations() {
//        animZoomIn = AnimationUtils.loadAnimation(applicationContext, R.anim.zoomin)
        animFadeIn = AnimationUtils.loadAnimation(applicationContext, R.anim.fadein)
//        animFadeOut = AnimationUtils.loadAnimation(applicationContext, R.anim.fadeout)
        animBlink = AnimationUtils.loadAnimation(applicationContext, R.anim.blink)
//        animMoveLeft = AnimationUtils.loadAnimation(applicationContext, R.anim.move_left)
//        animMoveRight = AnimationUtils.loadAnimation(applicationContext, R.anim.move_right)
    }

    private fun initQuiz() {
        tag("$TAG initQuiz")
        binding.quizLayout.show(false)
        initQusetionTable()
        initLifelines()
        binding.textScore.text = "0"
        binding.layoutFinish.progressScore.max = 0
        binding.layoutFinish.progressScore.clearAnimation()

        viewModel.qset.reverse()
        viewModel.currentq = viewModel.qset[viewModel.index]
        viewModel.setSuccess("setQuestion", "quizStatus")
    }

    private fun startService() {
        Intent(applicationContext, LifeService::class.java).apply {
            val bundle = Bundle()
            bundle.putParcelable("user",viewModel.user)
            bundle.putLong("timestampLife",preferences.timestampLife)
            putExtra("bundle",bundle)
            startService(this)
        }
    }

    private fun initQusetionTable() {
        // question table
        binding.level1.show(false)
        binding.level2.show(false)
        binding.level3.show(false)

        questionTable.clear()
        binding.layoutLevel1.removeAllViews()
        binding.layoutLevel2.removeAllViews()
        binding.layoutLevel3.removeAllViews()

        viewModel.qset.reverse()
        for(i in viewModel.qset) {
            tag("$TAG qno: ${i.qno}")
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            val textView = TextView(this)
            textView.layoutParams = params
            textView.setPadding(16, 2, 16, 2)
            if (Build.VERSION.SDK_INT < 23) {
                textView.setTextAppearance(this, R.style.TextAppearance_AppCompat_Small)
            } else {
                textView.setTextAppearance(R.style.TextAppearance_AppCompat_Small)
            }
            textView.typeface = Typeface.DEFAULT_BOLD
            textView.text = "Q.: ${i.qno}"
            textView.setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
            textView.tag = i.qno

            if (i.qno in 1..5) {
                binding.layoutLevel1.addView(textView)
                binding.level1.show()
            }

            if (i.qno in 6..10) {
                binding.layoutLevel2.addView(textView)
                binding.level2.show()
            }

            if (i.qno in 11..15) {
                binding.layoutLevel3.addView(textView)
                binding.level3.show()
            }

            // create view array
            questionTable.add(textView)
        }

        binding.quizLayout.show()
        viewModel.setLoading(false)
    }

    private fun initLifelines() {
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
                view.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_btn_orange,
                    null
                )

                val duration = viewModel.currentq.qTime.minus(
                    binding.textTimer.text.toString().toInt()
                )

                if(viewModel.isWild) {
                    viewModel.moveToNext()
                } else {
                    viewModel.calculateScore(duration)
                }
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
                        R.drawable.bg_btn_black,
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
                                    R.drawable.bg_circle_orange,
                                    null
                                )
                            }

                            val duration = viewModel.currentq.qTime.minus(
                                binding.textTimer.text.toString().toInt()
                            )

                            if(viewModel.isWild) {
                                viewModel.moveToNext()
                            } else {
                                viewModel.calculateScore(duration)
                            }
                        } else {
                            for (k in 0 until bindingSpelling.spelLayout.childCount) {
                                (bindingSpelling.spelLayout.getChildAt(k) as TextView).background = ResourcesCompat.getDrawable(
                                    resources,
                                    R.drawable.bg_circle_black,
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
        // if wild question then disable lifelines
        if(viewModel.isWild) {
            stateLifeline(binding.btnFiftyFifty, false)
            stateLifeline(binding.btnDoubleDip, false)
            stateLifeline(binding.btnFlip, false)
        }

//        binding.question.startAnimation(animFadeIn)

        // highlight table
        for(v in questionTable) {
            v.background = null
            (v as TextView).setTextColor(ResourcesCompat.getColor(resources, R.color.white, null))
        }

        for(v in questionTable) {
            if(v.tag == currentQ.qno) {
                v.background = ResourcesCompat.getDrawable(
                    resources,
                    R.drawable.bg_btn_yellow,
                    null
                )
                (v as TextView).setTextColor(
                    ResourcesCompat.getColor(
                        resources,
                        R.color.black,
                        null
                    )
                )
                v.setPadding(16, 2, 16, 2)
                break
            }
        }

        viewModel.displayAttachment()
    }

    private fun displayOption(currentQ: Question) {
        tag("displayOption: ${currentQ.qno}")
        binding.layoutOption.startAnimation(animFadeIn)
        viewModel.startTimer()
    }


    private fun displayAttachment(currentQ: Question) {
        tag("displayAttachment: ${currentQ.qno}")
        binding.layoutAttachment.progressTimer.show(false)

        val image = binding.layoutAttachment.layoutAttachment.findViewById<ImageView>(R.id.image)
        val video = binding.layoutAttachment.layoutAttachment.findViewById<VideoView>(R.id.video)

        image.show(false)
        video.show(false)

        var url = currentQ.qattach.trim()
        mp = MediaPlayer()
        binding.layoutAttachment.btnClose.setOnClickListener {
            when(currentQ.qtype) {
                "audio" -> {
                    if (mp.isPlaying) {
                        mp.stop()
                    }
                    mp.release()
                }
                "video" -> {
                    video.stopPlayback()
                }
            }

            if(video.visibility == View.VISIBLE) {
                video.stopPlayback()
            }
            if (attachmentSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                attachmentSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            viewModel.attachmentTimerCancel()
            viewModel.displayOption()
        }

        binding.layoutAttachment.question.text = currentQ.qdesc
        if(viewModel.lang.equals("hindi",true)) {
            binding.layoutAttachment.question.text = currentQ.qdesc_hindi
        }

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
                            binding.layoutAttachment.btnClose.performClick()
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }

                    image.loadImage(
                        "${BuildConfig.BASE_URL}gsp-admin/uploads/audio-quiz.jpg", false, true
                    )
                    image.show()
                }
                "video" -> {
                    video.setVideoPath(url)
                    video.show()
                    video.start()
                    video.setOnCompletionListener {
                        binding.layoutAttachment.btnClose.performClick()
                    }
                }
                else -> {
                    image.loadImage(
                        url,false, true
                    )
                    image.show()

                    binding.layoutAttachment.progressTimer.show()
                    binding.layoutAttachment.progressTimer.max = viewModel.TIME_ATTACHMENT.toInt()
                    binding.layoutAttachment.progressTimer.progress = viewModel.TIME_ATTACHMENT.toInt()
                    viewModel.attachmentTimerStart(viewModel.TIME_ATTACHMENT)
                }
            }

            if (attachmentSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                attachmentSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                hideSystemUI()
            }
        }
    }

    private fun displayWild() {
        binding.layoutCategory.shuffleCost.text = "Shuffle at ${viewModel.shuffleCoins} coins."
        binding.layoutCategory.cat1Text.text = viewModel.wset[0].qcat
        binding.layoutCategory.cat1.tag = viewModel.wset[0]

        binding.layoutCategory.cat2Text.text = viewModel.wset[1].qcat
        binding.layoutCategory.cat2.tag = viewModel.wset[1]

        binding.layoutCategory.cat3Text.text = viewModel.wset[2].qcat
        binding.layoutCategory.cat3.tag = viewModel.wset[2]

        binding.layoutCategory.cat1.setOnClickListener {
            viewModel.setWildQuestion(it.tag as Question)
            if (categorySheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                categorySheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.layoutCategory.cat2.setOnClickListener {
            viewModel.setWildQuestion(it.tag as Question)
            if (categorySheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                categorySheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.layoutCategory.cat3.setOnClickListener {
            viewModel.setWildQuestion(it.tag as Question)
            if (categorySheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                categorySheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        binding.layoutCategory.btnShuffle.setOnClickListener {
            shuffleWild(viewModel.shuffleCoins)
        }

        binding.layoutCategory.btnQuit.setOnClickListener {
            if (categorySheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                categorySheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }

            lifecycleScope.launch {
                delay(viewModel.TIME_DELAY)
                displayLifeline("quit")
            }
        }

        if (categorySheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            categorySheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            hideSystemUI()
        }
    }

    private fun shuffleWild(coins: Int) {
        confirmDialog(
            "Coins Redeem",
            "$coins coins will be redeem",
            { viewModel.shuffleWild(coins) },
            {}
        )
    }

    private fun displayFinish() {
        startService()

        binding.layoutFinish.btnPlay.setOnClickListener {
            if (finishSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                finishSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

                lifecycleScope.launch {
                    delay(viewModel.TIME_DELAY)
                    viewModel.quizData()
                }
            }
        }

        binding.layoutFinish.btnQuit.setOnClickListener {
            viewModel.setSuccess("exit", "quizStatus")
        }

        binding.layoutFinish.progressScore.max = viewModel.qset.size * 1000
        ObjectAnimator.ofInt(binding.layoutFinish.progressScore,"progress", viewModel.statsData.size * 1000)
            .setDuration(viewModel.TIME_DELAY)
            .start()
        binding.layoutFinish.progressText.text = "Achieved No. of Q. ${viewModel.statsData.size}"


        binding.layoutFinish.msg.text = "Congratulations!"
        if(binding.textScore.text.toString().equals("0", true)) {
            binding.layoutFinish.msg.text = "Oops!"
        }
        binding.layoutFinish.msgScore.text = "Your score is ${binding.textScore.text}"
        binding.layoutFinish.xp.text = "Earned XP: ${viewModel.xp.values.sum()}"

        if (finishSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            finishSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            hideSystemUI()
        }
    }

    private fun displayOffers() {
        viewModel.setLoading(false)
        binding.layoutOffers.offer1.setOnClickListener {
            offerPurchase("1",(it.tag as String).toInt())
        }

        binding.layoutOffers.offer2.setOnClickListener {
            offerPurchase("5",(it.tag as String).toInt())
        }

        binding.layoutOffers.offer3.setOnClickListener {
            offerPurchase("1h",(it.tag as String).toInt())
        }

        binding.layoutOffers.btnClose.setOnClickListener {
            viewModel.setSuccess("exit", "quizStatus")
        }

        if (offersSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
            offersSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            hideSystemUI()
        }
    }

    private fun offerPurchase(offer: String,coins: Int) {
        confirmDialog(
            "Coins Redeem",
            "$coins coins will be redeem",
            {
                if (offersSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
                    offersSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                viewModel.offerPurchase(offer,coins)
            },
            {}
        )
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

        hideSystemUI()
    }

    private fun downloadFile(sourceFileName: String, destinationFileName: String) {
        val localFile = File(getExternalFilesDir("quiz"), destinationFileName)
        if(!localFile.exists()) {
            viewModel.saveFile(localFile, sourceFileName)
        }
    }
}