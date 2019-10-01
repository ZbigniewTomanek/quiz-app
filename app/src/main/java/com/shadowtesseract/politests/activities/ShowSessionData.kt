package com.shadowtesseract.politests.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.QuestionBaseManager
import com.shadowtesseract.politests.database.model.DEFAULT_MAX_NUMBER_OF_REPEATS
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.database.model.Session
import com.shadowtesseract.politests.logger.Logger
import org.achartengine.GraphicalView
import org.achartengine.chart.PieChart
import org.achartengine.model.CategorySeries
import org.achartengine.renderer.DefaultRenderer
import org.achartengine.renderer.SimpleSeriesRenderer
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig

const val QUESTION_REQUEST = "qr"
private const val TAG = "SSD"
private const val SHOWCASE_ID = "ewdrui"

class ShowSessionData : AppCompatActivity() {
    lateinit var questionRequest: QuestionRequest
    lateinit var session: Session

    lateinit var titleTextView: TextView
    lateinit var answersTextView: TextView
    lateinit var remainingAnswersTextView: TextView
    lateinit var settingsButton: Button
    lateinit var startTestButton: Button
    lateinit var resetSessionButton: Button

    lateinit var pieChartView : LinearLayout
    private var mSeries = CategorySeries("")
    private val mRenderer = DefaultRenderer()
    private var mChartView : GraphicalView? = null


    private val COLORS = intArrayOf(Color.rgb(56, 128, 58), Color.rgb(174,0,0), Color.rgb(69, 90, 100))
    private val NAME_LIST = arrayOf("Poprawnie:", "Niepoprawnie:")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_session_data)

        val extras = intent.extras
        if (extras != null) {
            val json = extras.getString(QUESTION_REQUEST)
            questionRequest = Gson().fromJson(json, QuestionRequest::class.java)
        } else {
            Toast.makeText(this, R.string.error, Toast.LENGTH_SHORT).show()
            finish()
        }

        session = QuestionBaseManager.instance.loadSession(questionRequest, applicationContext)

        pieChartView = findViewById(R.id.pieChartView)
        titleTextView = findViewById(R.id.session_test_name_textView)
        answersTextView = findViewById(R.id.answers_textView)
        remainingAnswersTextView = findViewById(R.id.reamining_questions_textView)
        settingsButton = findViewById(R.id.settings_button)
        startTestButton = findViewById(R.id.start_test_button)
        resetSessionButton = findViewById(R.id.reset_session_button)

        setPieChart(session.getCorrectAnswers(), session.getWrongAnswers())
        loadView()

        resetSessionButton.setOnClickListener { QuestionBaseManager.instance.deleteSession(questionRequest, applicationContext)
                                                session = QuestionBaseManager.instance.loadSession(questionRequest, applicationContext)
                                                loadView()
                                                Toast.makeText(this, R.string.reset_session, Toast.LENGTH_SHORT).show()}

        settingsButton.setOnClickListener { showSettingsDialog() }

        startIntroduction()
    }


    private fun setPieChart(numOfCorrect:Int, numOfUncorrect:Int)
    {
        // clear data
        mSeries.clear()
        mRenderer.removeAllRenderers()


        Logger.logMsg(TAG, "Ustawianie wykresu")
        mSeries = CategorySeries("")
        mRenderer.isApplyBackgroundColor = false
        mRenderer.isShowLegend = false
        mRenderer.isShowLabels = false
        mRenderer.startAngle = 90.0f
        mRenderer.isClickEnabled = false
        mRenderer.isZoomEnabled = false
        mRenderer.isPanEnabled = false


        Logger.logMsg(TAG, "Dodawanie danych wykresu")

        if(numOfCorrect == 0 && numOfUncorrect == 0)
        {
            mRenderer.isShowLabels = false
            mSeries.add("", 1.toDouble())
            val rendererLack = SimpleSeriesRenderer()
            rendererLack.color = COLORS[2]
            mRenderer.addSeriesRenderer(rendererLack)
        }
        else
        {
            mSeries.add("${NAME_LIST[0]} $numOfCorrect", numOfCorrect.toDouble())
            val rendererCorrect = SimpleSeriesRenderer()
            rendererCorrect.color = COLORS[0]
            mRenderer.addSeriesRenderer(rendererCorrect)

            mSeries.add("${NAME_LIST[1]} $numOfUncorrect", numOfUncorrect.toDouble())
            val rendererWrong = SimpleSeriesRenderer()
            rendererWrong.color = COLORS[1]
            mRenderer.addSeriesRenderer(rendererWrong)
        }



        Logger.logMsg(TAG, "Odświeżanie wykresu")

        if (mChartView != null) {
            Logger.logMsg(TAG, "Ryswoanie: " + mChartView + " -> " + pieChartView)
            pieChartView.removeView(mChartView)
        }

        val pieChart = PieChart(mSeries, mRenderer)
        Logger.logMsg(TAG, " " + mSeries.title + " - " + mRenderer.toString() + " " + pieChart.toString())
        mChartView = GraphicalView(applicationContext, pieChart)
        pieChartView.addView(mChartView)

        Logger.logMsg(TAG, "Zakończono")
    }

    private fun startIntroduction() {
        val showcaseConfig = ShowcaseConfig()
        showcaseConfig.delay = 500
        showcaseConfig.maskColor = R.color.primaryDark
        val materialShowcaseSequence = MaterialShowcaseSequence(this, SHOWCASE_ID)
        materialShowcaseSequence.setConfig(showcaseConfig)
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.session_top))
                .setContentText(getString(R.string.session_stats_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.reset_session_button))
                .setContentText(getString(R.string.reset_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.settings_button))
                .setContentText(getString(R.string.settings_intro))
                .setDismissOnTouch(true)
                .build()
        )

        materialShowcaseSequence.start()
    }

    override fun onBackPressed() {
        val intent = Intent(this, TestChooser::class.java)
        startActivity(intent)
        finish()
    }

    private fun loadView() {
        val title = "${questionRequest.getName()}"
        titleTextView.text = title

        val totalAnswers = session.getCorrectAnswers() + session.getWrongAnswers()
        val answersText = if (totalAnswers == 0) getString(R.string.not_opened_test)
            else getString(R.string.session_stats, totalAnswers, ((session.getCorrectAnswers().toDouble()/totalAnswers) * 100).toInt())

        answersTextView.text = answersText

        remainingAnswersTextView.text = session.getNumberOFQuestionsToLearn().toString()
    }

    private fun showSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Właściwości")
        val types = arrayOf("Liczba powtórzeń każdego pytania", "Dodatkowe powtórzenia w razie pomyłki")

        builder.setItems(types) { dialog, which ->
            dialog.dismiss()
            when (which) {
                0 -> setNumberOfRepsDialog()
                1 -> setNumberOfExtraRepsDialog()
         }
        }
        builder.show()
    }

    private fun setNumberOfRepsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Liczba powtórzeń każdego pytania")
        val types = MutableList(DEFAULT_MAX_NUMBER_OF_REPEATS) { i -> (i+1).toString()}

        builder.setItems(types.toTypedArray()) { dialog, which ->
            dialog.dismiss()
            session.numberOfRepeats = (which + 1)
            QuestionBaseManager.instance.saveSession(session, session.questionRequest, this)
            Toast.makeText(this, "Zmieniono wartość", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun setNumberOfExtraRepsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Liczba dodaktowych powtórzeń")
        val types = MutableList(DEFAULT_MAX_NUMBER_OF_REPEATS) { i -> (i+1).toString()}

        builder.setItems(types.toTypedArray()) { dialog, which ->
            dialog.dismiss()
            session.numberOfExtraRepeats = (which + 1)
            QuestionBaseManager.instance.saveSession(session, session.questionRequest, this)
            Toast.makeText(this, "Zmieniono wartość", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    fun startTest(view : View) {
        val intent = Intent(this, ActivityQuestion::class.java)

        intent.putExtra("serializedSession", session.serialiseSession(session))
        startActivity(intent)
        finish()
    }

}
