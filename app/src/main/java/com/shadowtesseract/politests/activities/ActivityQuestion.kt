package com.shadowtesseract.politests.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.*
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.QuestionBaseManager
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import com.shadowtesseract.politests.database.engine.StorageConnector
import com.shadowtesseract.politests.database.engine.Tools
import com.shadowtesseract.politests.database.model.Question
import com.shadowtesseract.politests.database.model.Session
import com.shadowtesseract.politests.logger.Level
import com.shadowtesseract.politests.logger.Logger
import com.shadowtesseract.politests.logger.Logger.logErr
import com.shadowtesseract.politests.logger.Logger.logMsg
import com.shadowtesseract.politests.logger.Logger.logStats
import com.shadowtesseract.politests.logger.StatsType
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig
import kotlin.math.abs

private const val TAG = "AQ"
private const val GOOD_ANSWERS_STAT = "good_answers"
private const val SESSION_TIME_STAT = "session_time"
private const val WRONG_ANSWERS_STAT = "wrong_answers"
private const val TOTAL_WRONG_STAT = "total_wrong_answers"
private const val TOTAL_CORRECT_STAT = "total_correct_answers"
private const val TEST_NAME_STAT = "test_name"
const val MIN_SHIFT = 200

const val PUT_QUESTION_REQUEST = "qr"
const val PUT_QUESTION_NAME = "qn"
const val PUT_ANSWER_PHOTO_NAME = "apn"

private const val DELTA_TIME = 1000
private const val CLICKS_TO_SHOW_HELP = 5
private const val SHOWCASE_ID = "cy65xy"

private const val RC_SHOW_PHOTO = 420

private const val DOUBLE_TAP_TIME = 300

class ActivityQuestion : AppCompatActivity() {
    lateinit var question : TextView
    private lateinit var answersListView : ListView
    private lateinit var session: Session
    private var currentQuestion: Question? = null
    private var listOfCorrectAnswers : MutableList<Int> = mutableListOf()
    private var listOfChosenAnswers : MutableMap<TextView, Int> = mutableMapOf()

    private val stats = mutableMapOf<String, String>()

    // vibrations flag
    private var isVibrationsOn : Boolean = false

    // swype helper
    private var clickCounter = 0

    // save session flag
    private var savedSession : Boolean = false

    // send answer flag
    private var answered : Boolean = false

    // swype
    private var x1: Float = 0f
    private var x2: Float = 0f

    // exit
    private var time = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question)
        logMsg(TAG, "startuję")
        isVibrationsOn = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(getString(R.string.IS_VIBRATION_ON), false)
        val extras = intent.extras
        if (extras != null) {
            val serializedSession = extras.getString("serializedSession")
            session = Gson().fromJson<Session>(serializedSession, Session::class.java)
        } else {
            logErr(TAG, "zjebało się", Level.HIGH)
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            finish()
        }

        session.currentCorrect = 0
        session.currentWrong = 0

        question = findViewById(R.id.question)
        answersListView = findViewById(R.id.answers)

        answersListView.setOnTouchListener { view, event ->
            touchEvent(event)
            view.performClick()
            false}

        answersListView.performClick()

        stats[TIME_STAT] = (System.currentTimeMillis() / 1000).toString()
        stats[TEST_NAME_STAT] = session.questionRequest.getName()

        loadQuestion()
        updateCounters()
        startIntroduction()


        logStats(TAG, "Otworzono test", stats, StatsType.OPENED_TEST)
    }

    private fun startIntroduction() {
        val showcaseConfig = ShowcaseConfig()
        showcaseConfig.delay = 500
        showcaseConfig.maskColor = R.color.primaryDark
        val materialShowcaseSequence = MaterialShowcaseSequence(this, SHOWCASE_ID)
        materialShowcaseSequence.setConfig(showcaseConfig)
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(question)
                .setContentText(getString(R.string.question_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.answeredCounter))
                .setContentText(getString(R.string.answered_counter_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.repsCounter))
                .setContentText(getString(R.string.reps_counter_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(answersListView)
                .setContentText(getString(R.string.answers_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(answersListView)
                .setContentText(getString(R.string.image_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(answersListView)
                .setContentText(getString(R.string.check_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(answersListView)
                .setContentText(getString(R.string.next_question_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.reportError))
                .setContentText(getString(R.string.report_error_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.shareOnMessenger))
                .setContentText(getString(R.string.share_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.start()

    }

    private fun updateCounters() {
        val left = getString(R.string.answered_counter, session.getNumberOFQuestionsToLearn())
        findViewById<TextView>(R.id.answeredCounter).text = left
        val reps = getString(R.string.reps_counter, session.getRemainingNumberOfRepetitions())
        findViewById<TextView>(R.id.repsCounter).text = reps
    }


    override fun onBackPressed() {
        if (System.currentTimeMillis() - time < DELTA_TIME) {
            exit()
        }
        else {
            Toast.makeText(this, R.string.click_back_to_exit, Toast.LENGTH_SHORT).show()
            time = System.currentTimeMillis()
        }
    }

    private fun showSkipDialog() {
        val builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
        else
            AlertDialog.Builder(this)


        builder.setTitle("")
                .setMessage(R.string.skip_question)
                .setPositiveButton(R.string.yes) { _, _-> session.feedbackSkip()
                    loadQuestion()}
                .setNegativeButton(R.string.no) { _, _ -> logMsg(TAG, "No nie chce") }
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()

    }

    private fun showFinishedTestDialog() {
        val builder = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
        else
            AlertDialog.Builder(this)

        stats[TIME_STAT] = ((System.currentTimeMillis() / 1000) - stats[TIME_STAT]!!.toInt()).toString()
        stats[GOOD_ANSWERS_STAT] = session.currentCorrect.toString()
        stats[WRONG_ANSWERS_STAT] = session.currentWrong.toString()
        stats[TOTAL_WRONG_STAT] = session.getWrongAnswers().toString()
        stats[TOTAL_CORRECT_STAT] = session.getCorrectAnswers().toString()
        Logger.logStats(TAG, "Użytkownik rozwiązał cały test", stats, StatsType.SESSION_TIME)


        builder.setTitle("")
                .setMessage(R.string.congrats)
                .setPositiveButton(R.string.exit_test) { _, _-> exit()}
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()

    }


    private fun showImage(context: Context, answerPhotoName: String = "") {
        val intent = Intent(context, ImageDialog::class.java)
        val g = Gson()

        val qrString = g.toJson(session.questionRequest)
        intent.putExtra(PUT_QUESTION_REQUEST, qrString)
        intent.putExtra(PUT_QUESTION_NAME, currentQuestion!!.id)
        intent.putExtra(PUT_ANSWER_PHOTO_NAME, answerPhotoName)

        startActivityForResult(intent, RC_SHOW_PHOTO)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SHOW_PHOTO) {
            if (resultCode != Activity.RESULT_OK) {
                if (instance.isOnline()) {
                    Toast.makeText(this, "Pobieram ponownie zdjęcia pytania", Toast.LENGTH_SHORT).show()
                    StorageConnector.downloadQuestionPhotos(session.questionRequest, currentQuestion!!, this)
                }

            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        touchEvent(event)
        return super.onTouchEvent(event)
    }

    private fun touchEvent(event : MotionEvent?) {
        when(event!!.action) {
            MotionEvent.ACTION_DOWN -> x1 = event.x

            MotionEvent.ACTION_UP -> {
                x2 = event.x
                val deltaX = x2 - x1

                if(abs(deltaX) > MIN_SHIFT){
                    if (deltaX < 0 && !answered) { // swype right2left
                        logMsg(TAG, "prawo")
                        checkIfCorrect()
                    } else if (deltaX < 0 && answered) {
                        loadQuestion()
                        updateCounters()
                    } else if (deltaX >= 0) {
                        logMsg(TAG, "lewo")
                        if (currentQuestion!!.has_photo == "true")
                            showImage(this)

                        if (Tools.doesLinkPhoto(currentQuestion!!.question))
                            showImage(this, Tools.getPhotoNameFromAnswer(currentQuestion!!.question))

                        else if (currentQuestion!!.has_photo == "false")
                            Toast.makeText(this, R.string.no_image_for_question, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        stats[SESSION_TIME_STAT] = ((System.currentTimeMillis() / 1000) - stats[TIME_STAT]!!.toInt()).toString()
        stats[GOOD_ANSWERS_STAT] = session.currentCorrect.toString()
        stats[WRONG_ANSWERS_STAT] = session.currentWrong.toString()
        Logger.logStats(TAG, "Użytkownik opuścił sesję testu", stats, StatsType.SESSION_TIME)

        saveSession()
        super.onDestroy()
    }

    private fun saveSession() {
        if (!savedSession) {
            logMsg(TAG, "Zapisuje sesję")
            QuestionBaseManager.instance.saveSession(session, session.questionRequest, this)
            QuestionBaseManager.instance.addStatistics(session.currentCorrect, session.currentWrong)
            QuestionBaseManager.instance.saveUsersStats(this)
            savedSession = true
        }
    }

    private fun exit() {
        finish()
        saveSession()
    }

    private fun loadQuestion() {

        currentQuestion = session.getNextQuestion()

        listOfChosenAnswers.clear()
        listOfCorrectAnswers.clear()

        if (currentQuestion == null) {
            showFinishedTestDialog()
        } else {
            answered = false
            var question = currentQuestion?.question


            var hasSet = false
            if (Tools.doesLinkPhoto(question!!)) {
                val bitmap = StorageConnector.getPhoto(session.questionRequest, currentQuestion!!.id, this)
                if (bitmap != null) {
                    logMsg(TAG, "Wyświetlam")
                    question = ""
                    val drawable = BitmapDrawable(bitmap)
                    this.question.background = drawable
                    hasSet = true
                }
            } else
                this.question.setBackgroundResource(R.color.fui_transparent)


            if (currentQuestion?.has_photo == "true" && !hasSet) {
                question += " [zdjęcie]"
            }

            this.question.text = question
            setCorrectAnswers()
            setAnswers(currentQuestion!!.answers.toMutableList())
        }
    }

    private fun setCorrectAnswers() {
        val listOfCorrectAnswers = currentQuestion!!.right_answers
        for ((index, value) in listOfCorrectAnswers.iterator().withIndex()) {
            if (value == "true") {
                this.listOfCorrectAnswers.add(index)
            }
        }
    }

    private var lastClicked = -1
    private var timeClicked = SystemClock.uptimeMillis()
    private fun setAnswers(answers : MutableList<String>) {
        val adapter = AnswersAdapter(applicationContext, R.layout.answers, answers, session.questionRequest, ::showImage)
        this.answersListView.adapter = adapter

        this.answersListView.onItemClickListener = AdapterView.OnItemClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->

            val view = this.answersListView.getChildAt(position - this.answersListView.firstVisiblePosition)
            val clicked = view.findViewById<TextView>(R.id.answerText)
            val frame = view.findViewById<View>(R.id.answer_background)

            // sprawedza, czy powiększyć zdjęcie podwójnym kliknięciem
            logMsg(TAG, "Czas: ${SystemClock.uptimeMillis() - timeClicked}")
            if (SystemClock.uptimeMillis() - timeClicked < DOUBLE_TAP_TIME && currentQuestion!!.answerHasPhoto[position])
            {
                if (position == lastClicked) {
                    frame.setBackgroundResource(R.drawable.button) // odznaczamy odpowiedź
                    listOfChosenAnswers.remove(clicked)

                    val photoName = Tools.getPhotoNameFromAnswer(currentQuestion!!.answers[position])
                    showImage(this, photoName)
                    lastClicked = position
                    timeClicked = SystemClock.uptimeMillis()
                    clickCounter = 0
                }

            } else {
                lastClicked = position
                timeClicked = SystemClock.uptimeMillis()

                if (!answered) {
                    clickCounter++
                    if (clickCounter > CLICKS_TO_SHOW_HELP) {
                        if (currentQuestion!!.has_photo == "true")
                            Toast.makeText(this, "By zobaczyć zdjęcie przeuń w prawo", Toast.LENGTH_SHORT).show()

                        Toast.makeText(this, "By sprawdzić odpowiedzi przesuń w lewo", Toast.LENGTH_SHORT).show()

                        clickCounter = 0
                    }


                    if (listOfChosenAnswers.values.contains(position)) { //odznaczenie
                        if (currentQuestion!!.answerHasPhoto[position]) {
                            frame.setBackgroundResource(R.drawable.button)
                        } else {
                            clicked.setBackgroundResource(R.drawable.button)
                            clicked.setTextColor(ContextCompat.getColor(applicationContext, R.color.primaryDark))
                        }

                        listOfChosenAnswers.remove(clicked)
                    } else if (listOfCorrectAnswers.size == 1 && listOfChosenAnswers.size == 1 && clicked != listOfChosenAnswers.keys.toMutableList()[0]) { //zmiana zaznaczenia przy jednokrotnej odpowiedzi
                        if (currentQuestion!!.answerHasPhoto[position]) {
                            frame.setBackgroundResource(R.drawable.selected_answer)
                        } else {
                            clicked.setBackgroundResource(R.drawable.selected_answer)
                            clicked.setTextColor(ContextCompat.getColor(applicationContext, R.color.primaryDark))
                        }

                        val indexOfLastChosen = listOfChosenAnswers.values.toMutableList()[0]
                        if (currentQuestion!!.answerHasPhoto[indexOfLastChosen]) {
                            val view = this.answersListView.getChildAt(indexOfLastChosen - this.answersListView.firstVisiblePosition)
                            if (view != null) {
                                val frame = view.findViewById<View>(R.id.answer_background)
                                frame.setBackgroundResource(R.drawable.button)
                            }
                        } else {
                            val view = listOfChosenAnswers.keys.toMutableList()[0]
                            view.setBackgroundResource(R.drawable.button)
                            view.setTextColor(ContextCompat.getColor(applicationContext, R.color.primaryDark))
                        }

                        listOfChosenAnswers.clear()
                        listOfChosenAnswers[clicked] = position
                    }
                    // check if answered or there are more than 1 possible answer
                    else {
                        if (currentQuestion!!.answerHasPhoto[position]) {
                            frame.setBackgroundResource(R.drawable.selected_answer)
                        } else {
                            clicked.setBackgroundResource(R.drawable.selected_answer)
                            clicked.setTextColor(ContextCompat.getColor(applicationContext, R.color.primaryDark))
                        }
                        listOfChosenAnswers[clicked] = position
                    }
                }
            }
        }
    }

    private fun runVibrate(correct : Boolean) {
        if (isVibrationsOn) {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val vibratePattern: LongArray
            if (correct) {
                vibratePattern = longArrayOf(0, 400, 200, 400)
            } else {
                vibratePattern = longArrayOf(0, 200, 200, 200)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(vibratePattern, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            }
            // API <= 23
            else {
                vibrator.vibrate(vibratePattern, -1)
            }
        }
    }

    private fun checkIfCorrect() {
        clickCounter = 0

        when (listOfChosenAnswers.size) {
            0 -> showSkipDialog()
            else -> {
                answered = true
                if (listOfChosenAnswers.values.containsAll(listOfCorrectAnswers) && listOfChosenAnswers.size == listOfCorrectAnswers.size) {
                    session.feedbackCorrectAnswer()
                    runVibrate(true)
                    Toast.makeText(this, R.string.correct_answer, Toast.LENGTH_SHORT).show()
                }
                else {
                    session.feedbackWrongAnswer()
                    runVibrate(false)
                    Toast.makeText(this, R.string.wrong_answer, Toast.LENGTH_SHORT).show()
                }

                for ((key, value) in listOfChosenAnswers) {
                    if (listOfCorrectAnswers.contains(value)) {
                        if (currentQuestion!!.answerHasPhoto[value]) {
                            val view = this.answersListView.getChildAt(value - this.answersListView.firstVisiblePosition) as View
                            val frame = view.findViewById<View>(R.id.answer_background)
                            frame.setBackgroundResource(R.drawable.correct_answer)
                        } else {
                            key.setBackgroundResource(R.drawable.correct_answer)
                            key.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
                        }
                    }

                    else {
                        if (currentQuestion!!.answerHasPhoto[value]) {
                            val view = this.answersListView.getChildAt(value - this.answersListView.firstVisiblePosition) as View
                            val frame = view.findViewById<View>(R.id.answer_background)
                            frame.setBackgroundResource(R.drawable.wrong_answer)
                        } else {
                            key.setBackgroundResource(R.drawable.wrong_answer)
                            key.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
                        }
                    }
                }

                for (index in listOfCorrectAnswers) {
                    if (!listOfChosenAnswers.containsValue(index)) {
                        val layout = this.answersListView.getChildAt(index - this.answersListView.firstVisiblePosition) ?: continue
                        val view = layout.findViewById<TextView>(R.id.answerText)
                        val frame = layout.findViewById<View>(R.id.answer_background)

                        if (currentQuestion!!.answerHasPhoto[index]) {
                            frame.setBackgroundResource(R.drawable.not_selected_correct_answer)
                        } else {
                            view?.setBackgroundResource(R.drawable.not_selected_correct_answer)
                            view?.setTextColor(ContextCompat.getColor(applicationContext, R.color.white))
                        }
                    }
                }
            }
        }
    }

    private fun showSnackbar(message: String, action: () -> Unit) {
        val view = findViewById<View>(R.id.question_activity)
        Snackbar
                .make(view, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("Ponów") { action() }
                .setActionTextColor(Color.RED)
                .show()
    }

    fun sendReport(view : View) {
        val subject = "[TESTOWNIK ERROR] ${session.questionBase.name} v${session.questionBase.version} - ${currentQuestion?.id}"
        val message = "Tutaj opisz swoje uwagi. Dane pytania zawarte są już w temacie wiadomości."
        val mail = Intent(Intent.ACTION_SEND_MULTIPLE)
        mail.putExtra(Intent.EXTRA_EMAIL, arrayOf("shadow.tesseract.studio@gmail.com"))
        mail.putExtra(Intent.EXTRA_SUBJECT, subject)
        mail.putExtra(Intent.EXTRA_TEXT, message)
        mail.type = "message/rfc822"
        startActivity(mail)
    }

    fun shareOnMessenger(view : View) {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Hej, używam aplikacji Testownik. Chciałbyś mi pomóc z pytaniem: ${question.text}? Dostępne odpowiedzi: ")
        for ((index, answer) in currentQuestion!!.answers.iterator().withIndex()) {
            stringBuilder.append("\n${index + 1}) $answer")
        }
        val messenger = Intent(Intent.ACTION_SEND)
        messenger.putExtra(Intent.EXTRA_TEXT, stringBuilder.toString())
        messenger.type = "text/plain"
        messenger.`package` = "com.facebook.orca"
        try {
            startActivity(messenger)
        }
        catch (ex : android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Nie odnaleziono aplikacji Facebook Messenger", Toast.LENGTH_SHORT).show()
        }
    }
}
