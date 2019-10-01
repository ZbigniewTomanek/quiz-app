package com.shadowtesseract.politests.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.FirebaseConnector
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import com.shadowtesseract.politests.logger.Logger
import com.shadowtesseract.politests.logger.Logger.logMsg
import org.achartengine.GraphicalView
import org.achartengine.chart.BarChart
import org.achartengine.model.XYMultipleSeriesDataset
import org.achartengine.model.XYSeries
import org.achartengine.renderer.XYMultipleSeriesRenderer
import org.achartengine.renderer.XYSeriesRenderer
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig

private const val TAG = "MAIN_ACTIVITY"

private const val SHOWCASE_ID = "bkbk87"

class MainActivity : AppCompatActivity() {
    lateinit var statsSummaryTextView: TextView
    lateinit var pieChartView : LinearLayout
    private var mChartView : GraphicalView? = null

    private val RES_CODE = 666


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!LogInActivity.isLoggedIn()) {
            val intent = Intent(this, Introduction::class.java)
            startActivityForResult(intent, RES_CODE)
        }

        setContentView(R.layout.activity_main)

        Logger.initID(this)

        logMsg(TAG, "Tworzenie")
        instance.initContext(applicationContext)
        instance.loadUsersStats(applicationContext)

        pieChartView = findViewById(R.id.pieChartView)
        statsSummaryTextView = findViewById(R.id.statsSummaryText)

        setPieChart()
        showStats()

        startIntroduction()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RES_CODE) {
            if (!LogInActivity.isLoggedIn()) {
                logMsg(TAG, "Użytkownika nie zalogował się poprawnie - zamykam")
                finish()
            } else if(instance.isOnline())
                instance.actualizeAllTests(this)
        }
    }

    private fun startIntroduction() {
        val showcaseConfig = ShowcaseConfig()
        showcaseConfig.delay = 500
        showcaseConfig.maskColor = R.color.primaryDark
        val materialShowcaseSequence = MaterialShowcaseSequence(this, SHOWCASE_ID)
        materialShowcaseSequence.setConfig(showcaseConfig)
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.goToTests))
                .setContentText(getString(R.string.general_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.showSettingsButton))
                .setContentText(getString(R.string.main_settings_intro))
                .setDismissOnTouch(true)
                .build()
        )
        materialShowcaseSequence.start()
    }

    private fun setPieChart()
    {
        // clear data
//        mSeries.clear()
//        mRenderer.removeAllRenderers()

        // x-axis labels
        val days = instance.userStatistics.getDays().toIntArray()

        // XYSeries object
        val xAxis = IntArray(days.size, {i -> i})
        val yAxis = instance.userStatistics.getNumberOfAnsweredQuestions().toIntArray()
        val answersSeries = XYSeries("Answers")
        for (i in 0 until days.size) {
            answersSeries.add(xAxis[i].toDouble(), yAxis[i].toDouble())
        }

        // Dataset
        val dataset = XYMultipleSeriesDataset()
        dataset.addSeries(answersSeries)

        // XYSeriesRenderer
        val answersRenderer = XYSeriesRenderer()
        answersRenderer.isGradientEnabled = true
        answersRenderer.setGradientStart(0.0, ContextCompat.getColor(applicationContext, R.color.primary))
        answersRenderer.setGradientStop(5.0, ContextCompat.getColor(applicationContext, R.color.accent))
        answersRenderer.isFillPoints = false
        answersRenderer.lineWidth = 2f
        answersRenderer.isDisplayChartValues = false

        // XYMultipleSeriesRenderer
        val multiRenderer = XYMultipleSeriesRenderer()
        multiRenderer.orientation = XYMultipleSeriesRenderer.Orientation.HORIZONTAL
        multiRenderer.xLabels = 0
        multiRenderer.labelsTextSize = 24f
        multiRenderer.isZoomButtonsVisible = false
        multiRenderer.setPanEnabled(false, false)
        multiRenderer.isClickEnabled = false
        multiRenderer.setZoomEnabled(false, false)
        multiRenderer.isShowGridX = false
        multiRenderer.isShowGridY = false
        multiRenderer.setShowGrid(false)
        multiRenderer.isZoomEnabled = false
        multiRenderer.isExternalZoomEnabled = false
        multiRenderer.isAntialiasing = true
        multiRenderer.isInScroll = false
        multiRenderer.isShowLegend = false
        multiRenderer.xAxisColor = Color.TRANSPARENT
        multiRenderer.yAxisColor = Color.TRANSPARENT
        multiRenderer.xLabelsAlign = Paint.Align.CENTER
        multiRenderer.yLabels = 0
        multiRenderer.xAxisMin = -0.5
        multiRenderer.barSpacing = 0.5
        multiRenderer.backgroundColor = ContextCompat.getColor(applicationContext, R.color.primary)
        multiRenderer.marginsColor = ContextCompat.getColor(applicationContext, R.color.primary)
        multiRenderer.isApplyBackgroundColor = true
        multiRenderer.margins = intArrayOf(0, 50, 0, 0)
        multiRenderer.xAxisMax = 7.0

        // X-axis labels
        for (i in 0 until days.size) {
            multiRenderer.addXTextLabel(i.toDouble(), days[i].toString())
        }

        multiRenderer.addSeriesRenderer(answersRenderer)

        pieChartView.removeAllViews()
        val barChart = BarChart(dataset, multiRenderer, BarChart.Type.DEFAULT)
        mChartView = GraphicalView(applicationContext, barChart)
        pieChartView.addView(mChartView)
    }

    override fun onStart() {
        super.onStart()
        showStats()
        setPieChart()
    }

    override fun onResume() {
        super.onResume()

        showStats()
        setPieChart()

        val view = findViewById<View>(R.id.main_activity_view)
        view.invalidate()
        view.requestLayout()
    }


    @SuppressLint("StringFormatMatches")
    private fun showStats() {
        val stats = instance.userStatistics

        val totalQuestions = stats.wrongAnswers + stats.correctAnswers

        val statsText = getString(R.string.statsContent, totalQuestions)
        statsSummaryTextView.text = statsText
    }

    override fun onDestroy() {
        instance.saveUsersStats(applicationContext)
        FirebaseConnector.uploadUserStatistics(instance.userStatistics)
        super.onDestroy()
    }



    fun startLearningChooserScreen(view: View)
    {
        val intent = Intent(this, TestChooser::class.java)
        startActivity(intent)
    }

    fun showSettings(view : View) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Ustawienia")
        val types = arrayOf("Wibracje weryfikujące odpowiedź", "Wyloguj")

        builder.setItems(types) { dialog, which ->
            dialog.dismiss()
            when (which) {
                0 -> setVibrations()
                1 -> {
                    val intent = Intent(this, LogInActivity::class.java)
                    intent.putExtra(LOG_EXTRAS, LOG_OUT)
                    startActivity(intent)
                    finish()}
                    }
        }
        builder.show()
    }

    private fun setVibrations() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Wibracje weryfikujące odpowiedź")
        val types = arrayOf("Tak", "Nie")

        builder.setItems(types) { dialog, which ->
            dialog.dismiss()
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean( getString(R.string.IS_VIBRATION_ON), (which == 0)).apply()
            Toast.makeText(this, "Zmieniono wartość", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }
}
