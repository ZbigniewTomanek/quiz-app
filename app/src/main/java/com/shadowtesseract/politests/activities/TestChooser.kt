package com.shadowtesseract.politests.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.QuestionBaseManager
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import com.shadowtesseract.politests.database.model.QuestionBasesLocalData
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.logger.Logger.logMsg
import com.shadowtesseract.politests.logger.Logger.logStats
import com.shadowtesseract.politests.logger.StatsType
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig

private const val SHOWCASE_ID = "cppdkj"
private const val TAG = "TC"

class TestChooser : AppCompatActivity() {
    lateinit var listView: ListView
    lateinit var qmld : QuestionBasesLocalData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_chooser)
        setSupportActionBar(findViewById(R.id.save_toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false) // hide title

        listView = findViewById(R.id.listView)
        qmld = QuestionBaseManager.instance.getListOFDownloadedQuestionBases()

        val testNamesList = qmld.getQuestionbasesAsNames()
        if (testNamesList.isEmpty())
        {
            val textView: TextView = findViewById(R.id.test_chooser_title)
            textView.text = getString(R.string.empty_chooser_label_text)
        }
        else
        {
            showList(testNamesList)
        }

        startIntroduction()
    }

    private fun startIntroduction() {
        val showcaseConfig = ShowcaseConfig()
        showcaseConfig.delay = 500
        showcaseConfig.maskColor = R.color.primaryDark
        val materialShowcaseSequence = MaterialShowcaseSequence(this, SHOWCASE_ID)
        materialShowcaseSequence.setConfig(showcaseConfig)
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.save_toolbar))
                .setContentText(getString(R.string.toolbar_download))
                .setDismissOnTouch(true)
                .build()
        )

        materialShowcaseSequence.start()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_saved, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item!!.itemId) {
        R.id.action_download -> {
            if (QuestionBaseManager.instance.isOnline()) {
                val intent = Intent(this, DownloadTestActivity::class.java)
                startActivity(intent)
                finish()
            } else Toast.makeText(this, "By móc pobierać - połącz się z internetem", Toast.LENGTH_SHORT).show()

                true
        }

        R.id.action_contact -> {
            val mail = Intent(Intent.ACTION_SEND_MULTIPLE)
            mail.putExtra(Intent.EXTRA_EMAIL, arrayOf("shadow.tesseract.studio@gmail.com"))
            mail.putExtra(Intent.EXTRA_SUBJECT, "[TESTOWNIK]")
            mail.type = "message/rfc822"
            startActivity(mail)
            finish()
            true
        }

        R.id.action_donate -> {
            if (instance.isOnline()) {
                logStats(TAG, "Kliknięto w kubek", mutableMapOf(), StatsType.COFFEE)
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.buymeacoffee.com/shadowTesseract"))
                startActivity(browserIntent)
                finish()
                true
            } else {
                Toast.makeText(this, "Aby nas wesprzeć - połącz się z internetem", Toast.LENGTH_SHORT).show()
                false
            }
        }

        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    private fun showList(namesList_: MutableList<String>) {
        val namesList = namesList_.map { it ->
            val list = it.replace("_", " ").split("/")
            if (list.size > 6)
                "${list[2]} -> ${list[0]}"
            else if (list.size == 6)
                "${list[1]} -> ${list[0]}"
            else
                list.joinToString(separator = "->")}

        val delete: (Int) -> Unit = {position ->
            val name:String = qmld.listOfDatabases[position].toString()
            logMsg("TC", "Usuwanie: $name")
            QuestionBaseManager.instance.deleteSession(qmld.listOfDatabases[position], applicationContext)
            QuestionBaseManager.instance.deleteQuestionBase(qmld.listOfDatabases[position], applicationContext)

            val info: MutableMap<String, String> = mutableMapOf()
            info["test_name"] = name
            logStats(TAG, "Usunięto test", info, StatsType.DELETED_TESTS)



            val testNamesList = qmld.getQuestionbasesAsNames()
            if (testNamesList.isEmpty())
            {
                val textView: TextView = findViewById(R.id.test_chooser_title)
                textView.text = getString(R.string.empty_chooser_label_text)
            }
            else
            {
                showList( testNamesList)
            }

            showList(qmld.getQuestionbasesAsNames())
            Toast.makeText(this, "Usunięto test", Toast.LENGTH_SHORT).show()
        }


        val adapter = TestNamesButtonAdapter(applicationContext, R.layout.downloaded_test_names, namesList, delete)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener {
            adapterView, view, position, id ->
            logMsg("TC", "Wybrano: "+qmld.listOfDatabases[position])
            showSessionData(qmld.listOfDatabases[position])
        }

    }

    private fun showSessionData(questionRequest: QuestionRequest) {
        val intent = Intent(this, ShowSessionData::class.java)
        val json = Gson().toJson(questionRequest)

        intent.putExtra(QUESTION_REQUEST, json)
        startActivity(intent)
        finish()
    }
}
