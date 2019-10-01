package com.shadowtesseract.politests.activities

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.Observer
import com.shadowtesseract.politests.database.engine.QuestionBaseManager
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import com.shadowtesseract.politests.database.model.IndexInstance
import com.shadowtesseract.politests.database.model.PathIndex
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.logger.Logger.logMsg
import com.shadowtesseract.politests.logger.Logger.logStats
import com.shadowtesseract.politests.logger.StatsType
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseSequence
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView
import uk.co.deanwild.materialshowcaseview.ShowcaseConfig
import java.util.*


private const val TAG = "DTA"
private const val SHOWCASE_ID = "rd81et"

const val NAME_PROPERTY = "name"
const val VERSION_PROPERTY = "version"
const val NUMBER_OF_QUESTION_PROPERTY = "number"
const val AUTHORS_PROPERTY = "authors"
const val PATH_PROPERTY = "path"
const val NAMES_PROPERTY = "names"

const val TIME_STAT = "downloading_time"
const val IF_DOWNLOAD_STAT = "gone_to_download"

// after this number of nodes we have to download index file with all nodes test names
const val PROPER_PATH_LENGTH = 7


/**
 * This class provides ability to explore all paths leading to valid test uploaded to server
 * 
 * @property actualNode keeps the current node in tree path
 */

class DownloadTestActivity : AppCompatActivity(), Observer{
    lateinit var listView: ListView
    lateinit var pathView: TextView
    val request : QuestionRequest = QuestionRequest()
    val gbm = QuestionBaseManager.instance
    var index : MutableList<IndexInstance> = arrayListOf()
    var actualPathIndex: PathIndex = PathIndex()
    var hasPathIndex = true
    var ifResume = false
    var downloading = false

    private val stats: MutableMap<String, String> = mutableMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_download_test)
        setSupportActionBar(findViewById(R.id.download_toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(false)

        listView = this.findViewById(R.id.listView)
        pathView = findViewById(R.id.path_textView)

        Toast.makeText(this, "Ładuję listę testów", Toast.LENGTH_SHORT).show()

        gbm.add(this)
        gbm.getPathIndex(QuestionRequest())
        stats[TIME_STAT] = (System.currentTimeMillis() / 1000).toString()
        stats[IF_DOWNLOAD_STAT] = "false"

    }

    override fun onStart() {
        super.onStart()
        val rand = Random(SystemClock.uptimeMillis())
        if (rand.nextInt(5) == 0)
        {
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.popup_view)
            val textView = dialog.findViewById<TextView>(R.id.dialog_textView)
            textView.text = getString(R.string.add_database_alert)
            dialog.show()
        }
    }
    override fun onResume() {
        super.onResume()
        if (QuestionBaseManager.instance.downloadFromActivity) {
            QuestionBaseManager.instance.downloadFromActivity = false
            finish()
        }

        if (ifResume) {
            request.path.removeAt(request.path.size - 1)
            request.names.removeAt(request.names.size - 1)
            hasPathIndex = true
            printPath(request.names)
            gbm.getPathIndex(request)
            QuestionBaseManager.instance.getPathIndex(request)
            ifResume = false
        }
    }

    private fun startIntroduction() {
        val showcaseConfig = ShowcaseConfig()
        showcaseConfig.delay = 500
        showcaseConfig.maskColor = R.color.primaryDark
        val materialShowcaseSequence = MaterialShowcaseSequence(this, SHOWCASE_ID)
        materialShowcaseSequence.setConfig(showcaseConfig)
        materialShowcaseSequence.addSequenceItem(MaterialShowcaseView.Builder(this)
                .setTarget(findViewById(R.id.download_toolbar))
                .setContentText(getString(R.string.toolbar_downloaded))
                .setDismissOnTouch(true)
                .build()
        )

        materialShowcaseSequence.start()
    }

    @Suppress("UNCHECKED_CAST")
    override fun run(obj: Any) {

        if (obj is PathIndex)
        {
            logMsg(TAG, "Odebrano PathIndex")
            if(hasPathIndex)
            {
                actualPathIndex = obj
                showList(actualPathIndex.names)
                downloading = false
            }
            else
            {
                actualPathIndex = obj
            }
        }
        else if(obj is MutableList<*>)
        {
            index = obj as MutableList<IndexInstance>
            if(index.size>0)
            {
                hasPathIndex = false
                showList(index.map { it.toString() } as MutableList<String>)
                downloading = false
            }
            else
            {
                gbm.getPathIndex(request)
            }

        }


        logMsg(TAG, obj.toString())

    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_download, menu)
        startIntroduction()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item!!.itemId) {
        R.id.action_saved -> {
            val intent = Intent(this, TestChooser::class.java)
            startActivity(intent)
            finish()
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

    private fun showList(namesList: MutableList<String>) {
        val adapter = TestNameAdapter(applicationContext, R.layout.test_names, namesList)
        listView.adapter = adapter

        listView.onItemClickListener = AdapterView.OnItemClickListener {
            adapterView, view, position, id ->
            if(!downloading)
            {
                if(hasPathIndex)
                {
                    downloading = true
                    logMsg(TAG, actualPathIndex.toString())
                    request.path.add(actualPathIndex.hashes[position])
                    request.names.add(actualPathIndex.names[position])
                    logMsg(TAG, "Szukanie kolejnego pathIndex-u "+request.getAsIndex())
                    printPath(request.names)
                    gbm.getTestIndex(request)
                }
                else
                {
                    downloading = true
                    request.path.add(index[position].hash)
                    request.names.add(index[position].name)
                    printPath(request.names)
                    showTestMetadata(index[position].name, index[position].version, index[position].number_of_questions, index[position].authors)
                }
            }
        }
    }

    private fun printPath(path : MutableList<String>)
    {
        val sb = StringBuilder()
        for (loc in path)
            sb.append(loc+"/")
        if (!path.isEmpty())
            sb.deleteCharAt(sb.length-1)
        pathView.text = sb.toString()
    }

    override fun onBackPressed() {
        if (!request.path.isEmpty()) {
            downloading = true
            if(!hasPathIndex)
            {
                request.path.removeAt(request.path.size - 1)
                request.names.removeAt(request.names.size - 1)
                hasPathIndex = true
            }
            request.path.removeAt(request.path.size - 1)
            request.names.removeAt(request.names.size - 1)
            printPath(request.names)
            gbm.getPathIndex(request)

        } else {
            stats[TIME_STAT] = ((System.currentTimeMillis() / 1000) - stats[TIME_STAT]!!.toInt()).toString()
            logStats("Download Activity", "Nie pobrano testu", stats, StatsType.DOWNLOADING_TIME)
            finish()
            }
    }


    /**
     * starts screen displaying test metadata
     */
    private fun showTestMetadata(name: String, version: Int, numberOfQuestions: Int, authors: List<String>) {
        ifResume = true

        stats[TIME_STAT] = ((System.currentTimeMillis() / 1000) - stats[TIME_STAT]!!.toInt()).toString()
        stats[IF_DOWNLOAD_STAT] = "true"
        stats["test_name"] = pathView.text.toString()
        logStats("Download Activity", "Użytkownik przeszedł do ekranu testu", stats, StatsType.DOWNLOADING_TIME)

        logMsg(TAG, "przekazana wersja: $version")
        val intent = Intent(this, DisplayTestMetadataActivity::class.java)

        logMsg("DTA", "authors: "+authors)
        logMsg("DTA", "authors2: "+authors.toTypedArray())
        intent.putExtra(NAME_PROPERTY, name)
        intent.putExtra(VERSION_PROPERTY, version)
        intent.putExtra(NUMBER_OF_QUESTION_PROPERTY, numberOfQuestions)
        intent.putExtra(AUTHORS_PROPERTY, ArrayList<String>(authors))
        intent.putExtra(PATH_PROPERTY, ArrayList<String>(request.path))
        intent.putExtra(NAMES_PROPERTY, ArrayList<String>(request.names))
        startActivity(intent)
    }

    @Override
    override fun onDestroy() {
        QuestionBaseManager.instance.remove(this)
        super.onDestroy()
    }
}
