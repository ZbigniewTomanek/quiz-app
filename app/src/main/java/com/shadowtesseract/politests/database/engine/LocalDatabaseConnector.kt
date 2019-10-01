package com.shadowtesseract.politests.database.engine

import android.content.Context
import com.google.gson.Gson
import com.shadowtesseract.politests.database.model.QuestionBase
import com.shadowtesseract.politests.database.model.QuestionBasesLocalData
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.database.model.Session
import com.shadowtesseract.politests.logger.Logger.logMsg
import java.io.File
import java.io.PrintWriter


/**
 * This class will be singleton
 * Main function of this class will be sending requests of other classes to local database and passing results back
 */

const val APPLICATION_NAME = "Testownik"
const val QUESTION_BASE_LIST_PATH = "/$APPLICATION_NAME/questionbasesList.txt"

private const val TAG = "LDC"

class LocalDatabaseConnector : Observable
{
    var listOfObservers = mutableListOf<Observer>()
    var questionBasesLocalData = QuestionBasesLocalData()

    fun initQuestionBaseList(context: Context)
    {
        logMsg(TAG, "Tworzenie LDC")
        val file = File(context.filesDir, QUESTION_BASE_LIST_PATH)
        APPLICATION_NAME
        if(file.exists()) {
            logMsg(TAG, "Ładowanie pliku bazy")
            val bufferedReader = file.bufferedReader()
            val text: String = bufferedReader.readLines().get(0)

            val gson = Gson()
            questionBasesLocalData = gson.fromJson<QuestionBasesLocalData>(text, QuestionBasesLocalData::class.java) ?: QuestionBasesLocalData()
            logMsg(TAG, "Baza danych: " + questionBasesLocalData.listOfDatabases.toString())
        }
    }

    fun deleteQuestionDatabase(questionRequest: QuestionRequest, context: Context) : Boolean
    {
        val file = File(context.filesDir, "/"+APPLICATION_NAME+questionRequest.getFile()+".txt")
        if (!file.exists()) return false
        deleteSession(questionRequest, context)
        return file.delete()
    }

    fun saveDatabaseLocaly(questionBase: QuestionBase, questionRequest: QuestionRequest, context: Context)
    {
        val gson = Gson()
        val jsonString = gson.toJson(questionBase)

        val directory = File(context.filesDir, "/"+APPLICATION_NAME+questionRequest.getAsPath())
        if(!directory.exists())
            logMsg(TAG, "Tworzenie ścieżki dla pliku: "+directory.toString()+" -> "+directory.mkdirs())

        val file = File(context.filesDir, "/"+APPLICATION_NAME+questionRequest.getFile()+".txt")
        logMsg(TAG, "Zapisywanie danych do pliku: "+file.toString())
        val pw = PrintWriter(file)
        pw.print(jsonString)
        pw.close()
        logMsg(TAG, "dodawany qr: $questionRequest")
        questionBasesLocalData.listOfDatabases.add(questionRequest)
        saveQuestionBaseList(context)
        logMsg(TAG, "Zserializowano..")
    }

    fun loadDatabaseLocally(questionRequest: QuestionRequest, context: Context) : QuestionBase
    {
        val file = File(context.filesDir, "/"+APPLICATION_NAME+questionRequest.getFile()+".txt")
        val bufferedReader = file.bufferedReader()
        val text : String = bufferedReader.readLines()[0]
        logMsg(TAG, "R: "+text)

        val gson = Gson()
        return gson.fromJson<QuestionBase>(text, QuestionBase::class.java) ?: QuestionBase(arrayListOf())
    }

    private fun sessionPath(questionRequest: QuestionRequest) = "/$APPLICATION_NAME/sessions/${questionRequest.getFile()}.txt"

    fun loadSession(questionRequest: QuestionRequest, context: Context): Session {
        logMsg("LDC", "Ładowanie sesji: $questionRequest")
        val file = File(context.filesDir, sessionPath(questionRequest))

        val session: Session =
            if (!file.exists()) {
//                logMsg(TAG, "Obiekt sesji ${questionRequest.version} nie istnieje, zwracam domyślny")
                Session(loadDatabaseLocally(questionRequest, context), questionRequest)
            } else {
                logMsg(TAG, "Czytanie obiektu sesji ")
                val bufferedReader = file.bufferedReader()
                val jsonString = bufferedReader.readLines()[0]
                    logMsg(TAG, jsonString)
                Gson().fromJson<Session>(jsonString, Session::class.java) ?: Session(loadDatabaseLocally(questionRequest, context), questionRequest)
            }
        logMsg("LDC", "Zwrócono sesje")
        session.init()
        return session
    }

    fun saveSession(session: Session, questionRequest: QuestionRequest, context: Context) {
        val directory = File(context.filesDir, "/$APPLICATION_NAME/sessions/${questionRequest.getFile()}")
        if(!directory.exists())
            logMsg(TAG, "Tworzenie ścieżki dla folderu sesji: "+directory.toString()+" -> "+directory.mkdirs())


        val jsonString = Gson().toJson(session)
        val file = File(context.filesDir, sessionPath(questionRequest))
        val pw = PrintWriter(file)
        pw.print(jsonString)
        pw.close()
    }

    fun deleteSession(questionRequest: QuestionRequest, context: Context) {
        val file = File(context.filesDir, sessionPath(questionRequest))
        if (!file.exists())
            return

        file.delete()
    }

    fun saveQuestionBaseList(context: Context)
    {
        logMsg(TAG, "Zapisywanie aktualnej listy baz danych")
        val gson = Gson()
        val jsonString = gson.toJson(questionBasesLocalData)
        logMsg(TAG, "Zserializowany json bazy: "+jsonString)

        val file = File(context.filesDir, QUESTION_BASE_LIST_PATH)
        if(!file.exists()) file.createNewFile()
        logMsg(TAG, "Zapisywanie danych do pliku: "+file.toString())
        val pw = PrintWriter(file)
        pw.print(jsonString)
        pw.close()
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
}