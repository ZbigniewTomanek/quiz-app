package com.shadowtesseract.politests.activities

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.Observer
import com.shadowtesseract.politests.database.engine.QuestionBaseManager
import com.shadowtesseract.politests.database.engine.StorageConnector
import com.shadowtesseract.politests.database.model.QuestionBase
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.logger.Logger.logMsg
import com.shadowtesseract.politests.logger.Logger.logStats
import com.shadowtesseract.politests.logger.StatsType
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig

private const val SHOWCASE_ID = "pykoo6"
private const val TAG = "DTMD"

/**
 * This activity displays passed in bundle test metadata and contains download button
 */
class DisplayTestMetadataActivity : AppCompatActivity(), Observer {
    private var name = ""
    private var numberOfQuestions = 0
    private var version = 0
    private var authors = arrayListOf<String>()
    private var path = arrayListOf<String>()
    private var names = arrayListOf<String>()

    lateinit var downloadButton: Button
    lateinit var nameView: TextView
    lateinit var numberOFQuestionsView: TextView
    lateinit var versionView: TextView
    lateinit var authorsView: TextView

    private fun displayData() {
        nameView.text = name
        numberOFQuestionsView.text = numberOfQuestions.toString()
        versionView.text = version.toString()
        var auth = ""
        for (a in authors)
            auth += "$a "
        authorsView.text = auth.substring(0, auth.length - 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_test_metadata)
        logMsg("DTMA", "Tworzenie wyswietlania metadanych")
        name = intent.getStringExtra(NAME_PROPERTY)
        numberOfQuestions = intent.getIntExtra(NUMBER_OF_QUESTION_PROPERTY, -1)
        version = intent.getIntExtra(VERSION_PROPERTY, -1)
        authors = intent.getStringArrayListExtra(AUTHORS_PROPERTY)
        path = intent.getStringArrayListExtra(PATH_PROPERTY)
        names = intent.getStringArrayListExtra(NAMES_PROPERTY)

        nameView = findViewById(R.id.test_name_textView)
        numberOFQuestionsView = findViewById(R.id.number_of_question_textView)
        versionView = findViewById(R.id.version_textView)
        authorsView = findViewById(R.id.authors_textView)

        downloadButton = findViewById(R.id.test_download_button)

        QuestionBaseManager.instance.add(this)
        displayData()

        startIntroduction()
    }

    private fun startIntroduction() {
        val showcaseConfig = ShowcaseConfig()
        showcaseConfig.delay = 500
        showcaseConfig.maskColor = R.color.primaryDark
        val materialShowcaseSequence = MaterialShowcaseSequence(this, SHOWCASE_ID)
        materialShowcaseSequence.setConfig(showcaseConfig)
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.test_download_button))
                .setContentText(getString(R.string.download_intro))
                .setDismissOnTouch(true)
                .build()
        )

        materialShowcaseSequence.start()
    }

    fun downloadTest(view: View)
    {
        logMsg("DTMA", "Pobieranie testu")

        QuestionBaseManager.instance.downloadFromActivity = true

        findViewById<RelativeLayout>(R.id.loadingPanel).visibility = View.VISIBLE
        val request = QuestionRequest(path, names)
        val downloadedDatabases = QuestionBaseManager.instance.getListOFDownloadedQuestionBases()

        if (downloadedDatabases.listOfDatabases.contains(request)) {
            val info = mutableMapOf<String, String>()
            info["test_name"] = request.getName()
            info["downloaded"] = "false"
            logStats(TAG, "Próbowano ponowanie pobrać test", info, StatsType.DOWNLOADED_TESTS)

            Toast.makeText(this, "Ten test już jest pobrany :)", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            val info = mutableMapOf<String, String>()
            info["test_name"] = request.toString()
            info["downloaded"] = "true"
            logStats(TAG, "Pobrano test", info, StatsType.DOWNLOADED_TESTS)
            QuestionBaseManager.instance.getDatabaseFromFirebase(request)
            StorageConnector.add(this)
            QuestionBaseManager.instance.downloadTestPhotos(request, this)
            Toast.makeText(this, "Jeśli test posiada wiele zdjęć, pobieranie może chwilę trwać", Toast.LENGTH_SHORT).show()
        }
    }

    override fun run(obj: Any)
    {
        if(obj is QuestionBase)
        {
            logMsg(TAG, version.toString())
            logMsg("DTMA", "Pobrano bazę pytań.. ")
            logMsg("DTMA", obj.toString())
            downloadButton.isEnabled = false
            obj.version = version
            obj.name = name
            logMsg("DTMA", "Obj1: "+obj.toString())
            QuestionBaseManager.instance.saveQuestionBaseLocaly(obj, QuestionRequest(path, names), applicationContext)
            Toast.makeText(this, "Pobrano pytania testu", Toast.LENGTH_SHORT).show()
        }

        if (obj is Boolean)
        {
            StorageConnector.remove(this)
            finish()
        }

        logMsg(TAG, "Observer z argumentem: $obj")

    }

    override fun onBackPressed() {
        val info = mutableMapOf<String, String>()
        info["test_name"] = names.joinToString(separator = "/")
        info["downloaded"] = "false"
        logStats(TAG, "Nie pobrano testu", info, StatsType.DOWNLOADED_TESTS)
        super.onBackPressed()
    }

    @Override
    override fun onDestroy() {
        QuestionBaseManager.instance.remove(this)
        super.onDestroy()
    }
}
