package com.shadowtesseract.politests.database.engine

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.model.*
import com.shadowtesseract.politests.logger.Level
import com.shadowtesseract.politests.logger.Logger.logErr
import com.shadowtesseract.politests.logger.Logger.logMsg
import java.io.File
import java.io.IOException
import java.io.PrintWriter


/**
 * This class main can be used to:
 * ->Download question bases from firebase
 * ->Update question bases
 * ->Create local copy of question bases
 */


private const val USER_STATS_PATH = "stats"
private const val TAG = "QBM"
//data class UserStatiscs(var numberOfTests: Int = 0, var correctAnswers: Int = 0, var wrongAnswers: Int = 0)
var isIndexReady = false
var isDataReady = false
var errorOccurred = false

class QuestionBaseManager : Observer, Observable
{
    val fc = FirebaseConnector
    val sc = StorageConnector
    var ldb : LocalDatabaseConnector = LocalDatabaseConnector()
    var listOfObservers = mutableListOf<Observer>()
    var userStatistics: UserStatistics = UserStatistics()
    lateinit var currentBaseRequest: QuestionRequest

    var downloadFromActivity = false

    private constructor(){
        logMsg("QBM", "Tworzenie singletonu")

        fc.add(this)
        ldb.add(this)
        fc.add(sc)
    }

    private object Holder {val INSTANCE = QuestionBaseManager()}

    companion object QuestionBaseManager
    {
        val instance: com.shadowtesseract.politests.database.engine.QuestionBaseManager by lazy { Holder.INSTANCE }
    }

    fun getListOFDownloadedQuestionBases() : QuestionBasesLocalData = ldb.questionBasesLocalData

    fun initContext(context: Context)
    {
        logMsg("QBM", "Tworzenie obiektu LDC")
        ldb.initQuestionBaseList(context)
        logMsg(TAG, "Pobrane testy: ${getListOFDownloadedQuestionBases().listOfDatabases}")
    }

    fun isOnline(): Boolean {
        val runtime = Runtime.getRuntime()
        try {
            val ipProcess = runtime.exec("/system/bin/ping -c 1 8.8.8.8")
            val exitValue = ipProcess.waitFor()
            return exitValue == 0
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        return false
    }

    var questionBase : QuestionBase = QuestionBase(mutableListOf())
        get() = if (isDataReady && !errorOccurred) field else QuestionBase(mutableListOf())
        set(value) {field = value
            logMsg("QBM", "Ustawianie bazy")
            logMsg("QBM", "Wartosc bazy:"+field.listOfQuestions.toString())
        }

    var index : MutableList<IndexInstance> = mutableListOf()
        get() {return if (isIndexReady && !errorOccurred) field else mutableListOf<IndexInstance>()}
        set(value) {field = value
            logMsg("QBM", "Ustawianie indexu:")
            logMsg("QBM", "Wartosc indexu:"+field.toString())
        }



    override fun run(obj: Any) {
        if(obj is MutableList<*>)
        {
            logMsg(TAG, "odebrałem nowy indeks")
            index = obj as MutableList<IndexInstance>
            isIndexReady = true
        }
        else if(obj is PathIndex)
        {

        }
        else
        {
            questionBase = obj as QuestionBase
            isDataReady = true
        }
        notify(obj)
    }

    fun getPathIndex(request:QuestionRequest)
    {
        fc.downloadPathIndexOfQuestionBase(request)
    }

    fun getTestIndex(request:QuestionRequest)
    {
        fc.downloadTextIndexOfQuestionBase(request)
    }

    fun getDatabaseFromFirebase(request:QuestionRequest)
    {
        fc.downloadQuestionBase(request)
        currentBaseRequest = request
    }

    fun findTestIndexByRequest(request: QuestionRequest): IndexInstance {
        val key = request.path[request.path.size - 1]
        val newIndex = index.filter { it ->  it.hash == key}

        logMsg(TAG, "request dla indexu: $request")
        logMsg(TAG, "odnaleziony indeks: $newIndex")
        logMsg(TAG, "obecny indeks: ${index.map { it -> it.toList() }}")
        logMsg(TAG, "odnaleziony klucz: $key")

        if (!newIndex.isEmpty())
            return newIndex[0]
        else
            logErr(TAG, "Błąd pobierania indeksu", Level.HIGH)
            throw Exception("Jest załadowany zły testIndex!")
    }

    fun actualizeAllTests(context: Context) {
        val actualized = mutableListOf<QuestionRequest>()
        if (getListOFDownloadedQuestionBases().listOfDatabases.isEmpty()) return

        var isReady = 0
        var hasFailed = false

        val newTestIndexesList: MutableList<IndexInstance> = mutableListOf()

            Thread {
                for (request in getListOFDownloadedQuestionBases().listOfDatabases) {
                    fc.downloadTextIndexOfQuestionBase(request.getQuesionRequestForTestIndex())
                    while (!isIndexReady)
                        Thread.sleep(10)

                    logMsg(TAG, "Załadowano kolejny index")

                    val newIndex = findTestIndexByRequest(request)
                    logMsg(TAG, "pobrany index $")

                    if (!index.isEmpty())
                        newTestIndexesList.add(newIndex)
                    else {
                        hasFailed = true
                        Log.e(TAG, "Indeks dla żądania $request nie został pobrany!")
                    }

                    isIndexReady = false
                    isReady++
                }
            }.start()

        val numberOfTests = getListOFDownloadedQuestionBases().listOfDatabases.size
        Thread {
            while (isReady < numberOfTests) {
                logMsg(TAG, "Czekam z aktualizacją")
                Thread.sleep(10)
            }

            logMsg(TAG, "Pobrane indeksy $newTestIndexesList")

            if (!hasFailed) {
                logMsg(TAG, "Przystępuje do sprawdzania wersji")
                for (i in 0 until numberOfTests) {
                    val request = getListOFDownloadedQuestionBases().listOfDatabases[i]
                    val oldDatabase: QuestionBase = loadQuestionBaseLocaly(request, context)

                    logMsg(TAG, "Nowa wersja testu: ${newTestIndexesList[i].version}")
                    logMsg(TAG, "Stara wersja testu: ${oldDatabase.version}")


                    if (oldDatabase.version < newTestIndexesList[i].version) {
                        logMsg(TAG, "Aktualizowanie bazy ${request.getFile()}")
                        getListOFDownloadedQuestionBases().listOfDatabases.remove(request)

                        deleteQuestionBase(request, context)
                        deleteTestPhotos(request, context)

                        saveQuestionBaseLocaly(questionBase, request, context)
                        downloadTestPhotos(request, context)
                        actualized.add(request)
                    }
                }
            }
        }.start()

        val handler = Handler()
        handler.postDelayed({
            if (actualized.isEmpty())
                Toast.makeText(context, "Wszystkie testy są aktualne", Toast.LENGTH_SHORT).show()
            else
                for (qr in actualized)
                    Toast.makeText(context, "Zaktualizowano ${qr.toStringReversed()}", Toast.LENGTH_SHORT).show()

            actualized.clear()
        }, 3000)
    }

    fun deleteQuestionBase(questionRequest: QuestionRequest, context: Context)
    {
        ldb.questionBasesLocalData.listOfDatabases.remove(questionRequest)
        ldb.saveQuestionBaseList(context)
        val hasDeleted = ldb.deleteQuestionDatabase(questionRequest, context)
        logMsg("QBM", "Usunięto: "+questionRequest.toString()+" -> "+hasDeleted)
    }

    fun saveQuestionBaseLocaly(questionBase: QuestionBase, questionRequest: QuestionRequest, context: Context)
    {
        logMsg("QBM", "Zapisywanie bazy danych lokalnie")
        ldb.saveDatabaseLocaly(questionBase, questionRequest, context)
    }

    fun loadQuestionBaseLocaly(questionRequest: QuestionRequest, context: Context) : QuestionBase
    {
        return ldb.loadDatabaseLocally(questionRequest, context)
    }

    fun loadSession(questionRequest: QuestionRequest, context: Context): Session {
        return ldb.loadSession(questionRequest, context)
    }

    fun saveSession(session: Session, questionRequest: QuestionRequest, context: Context) {
        ldb.saveSession(session, questionRequest, context)
    }

    fun deleteSession(questionRequest: QuestionRequest, context: Context) {
        ldb.deleteSession(questionRequest, context)
    }
    override fun notify(obj: Any) {
        for(o : Observer in listOfObservers)
            o.run(obj)
    }

    override fun remove(observer: Observer) {
        listOfObservers.remove(observer)
    }

    override fun add(observer: Observer) {
        listOfObservers.add(observer)
    }


    fun loadUsersStats(context: Context) {
        val directory = File(context.filesDir, "/$APPLICATION_NAME/$USER_STATS_PATH")
        if(!directory.exists()) {
            logMsg(TAG, "Tworzenie folderu z plikiem statystyk użykownika")
            directory.mkdirs()
        }

        val file = File(context.filesDir, "/$APPLICATION_NAME/$USER_STATS_PATH.txt")

        if (!file.exists())
            saveUsersStats(context)

        val bufferedReader = file.bufferedReader()
        // TODO zmienić statystyki na takie trzymane w chmurze
        try {
            val text : String = bufferedReader.readLines()[0]
            userStatistics = userStatistics.deserialize(text)
        } catch (e: IndexOutOfBoundsException) {
            logErr(TAG, "Plik statystyk jest niepoprawny", Level.HIGH)
            Toast.makeText(context, "Nie udało się załadować statystyk :c", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveUsersStats(context: Context) {
        val file = File(context.filesDir, "/$APPLICATION_NAME/$USER_STATS_PATH.txt")
        logMsg(TAG, "Zapisywanie statysty użytkownika")
        val jsonString = userStatistics.serialize()
        val pw = PrintWriter(file)
        pw.print(jsonString)
        pw.close()
    }

    fun deleteTestPhotos(questionRequest: QuestionRequest, context: Context): Boolean {
        return sc.deleteTestPhotos(questionRequest, context)
    }

    /**
     * Żeby dostać zdjęcie w postaci bitmapy trzeba podać funkcji
     * questionRequest trzymane w obiekcie sesji, nazwę pytania i kontekst
     * Może zwrócić nulla w razie błędu lub wywołąniu dla złych danych
     */
    fun getPhoto(questionRequest: QuestionRequest, questionName: String, context: Context): Bitmap? {
        return sc.getPhoto(questionRequest, questionName, context)
    }

    fun downloadTestPhotos(questionRequest: QuestionRequest, context: Context) {
        sc.downloadTestPhotos(questionRequest, context)
    }

    fun addStatistics(correctAnswers: Int, wrongAnswers: Int) {
        logMsg(TAG, "Zwiększam statystyki ${userStatistics.correctAnswers} $correctAnswers")

        userStatistics.addAnsweredQuestions(correctAnswers, wrongAnswers)
    }

    fun showDialog(context: Context, info: String) {
        val dialog = Dialog(context)
        dialog.setContentView(R.layout.popup_view)

        val textView = dialog.findViewById<TextView>(R.id.dialog_textView)
        textView.text = info
        dialog.show()
    }


}