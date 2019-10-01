package com.shadowtesseract.politests.database.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import com.shadowtesseract.politests.database.model.Question
import com.shadowtesseract.politests.database.model.QuestionBase
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.logger.Level
import com.shadowtesseract.politests.logger.Logger.logErr
import com.shadowtesseract.politests.logger.Logger.logMsg
import java.io.File


private const val TAG = "Storage Connector"
private const val TIMEOUT = 5 * 60 * 1000

object Tools {
    private const val PREFIX = "[img]"
    private const val SUFFIX = "[/img]"

    fun getPhotoNameFromAnswer(answer: String): String {
        var photoName = answer.removeSurrounding(PREFIX, SUFFIX)
        if (".png" in photoName)
            photoName = photoName.replace(".png", ".jpg")
        var i = 0
        while (photoName[i] == '0')
            i++
        photoName = photoName.substring(i)

        return photoName
    }

    fun doesLinkPhoto(text: String): Boolean {
        val res = getPhotoNameFromAnswer(text)
        return text != res
    }
}

class Counter {
    private var i = 0

    @Synchronized
    fun start() = i++

    @Synchronized
    fun end() = i--

    fun getI() = i
}

object StorageConnector : Observer, Observable {
    private var ready = false
    private val storage = FirebaseStorage.getInstance()
    private lateinit var lastQuestionBase: QuestionBase
    private val observers = mutableListOf<Observer>()
    private val counter = Counter()



    private fun downloadAnswersPhotos(questionRequest: QuestionRequest, question: Question, context: Context) {
        for (answer in question.answers) {
            if (answer[0] == '[' ) {

                val photoName = Tools.getPhotoNameFromAnswer(answer)
                //jeśli pytanie ma formę [img]nazwa.jpg[/img], to następuje próba pobrania
                if (answer != photoName) {
                    counter.start()
                    logMsg(TAG, "Scieżka zdjęcia do pobrania: /$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")
                    val file = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")
                    storage.reference.child("${questionRequest.getAsPath()}/$photoName").getFile(file).addOnSuccessListener {
                        logMsg(TAG, "Poprawnie pobrano zdjęcie: $photoName")
                        counter.end()
                    }.addOnFailureListener {it ->
                        logErr(TAG, "nie udało się pobrać zdjęcia odpowiedzi w pytaniu($question): $it", Level.HIGH)
                        counter.end()
                    }
                }
            }
        }
    }

    fun downloadPhoto(questionRequest: QuestionRequest, photoName: String, context: Context, observer: Observer) {
        logMsg(TAG, "Scieżka zdjęcia do pobrania: /$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")
        val file = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")
        storage.reference.child("${questionRequest.getAsPath()}/$photoName").getFile(file).addOnSuccessListener {
            logMsg(TAG, "Poprawnie pobrano zdjęcie: $photoName")
            observer.run(true)
        }.addOnFailureListener {it ->
            logErr(TAG, "nie udało się pobrać zdjęcia odpowiedzi: $it", Level.HIGH)
            observer.run(false)
        }
    }

    fun downloadQuestionPhotos(questionRequest: QuestionRequest, question: Question, context: Context) {
        if (question.has_photo == "true") {
            var photoName = question.id.replace("txt", "jpg")

            var i = 0
            while (photoName[i] == '0')
                i++
            photoName = photoName.substring(i)

            counter.start()

            logMsg(TAG, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")
            val file = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")
            storage.reference.child("${questionRequest.getAsPath()}/$photoName").getFile(file).addOnSuccessListener {
                logMsg(TAG, "Poprawnie pobrano zdjęcie: $photoName")
                counter.end()
            }.addOnFailureListener {it ->
                logErr(TAG, "nie udało się pobrać zdjęcia pytania: $it", Level.HIGH)
                counter.end()
            }
        }

        downloadAnswersPhotos(questionRequest, question, context)
    }

    fun downloadTestPhotos(questionRequest: QuestionRequest, context: Context) {
        logMsg(TAG, "czekam")
        Thread {

            while (!ready) {
                logMsg(TAG, "czekam")
                Thread.sleep(5)
            }

            logMsg(TAG, "Przekazano")
            val directory = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}")
            if(!directory.exists())
                logMsg(TAG, "Tworzenie folderu dla zdjęć testu : "+directory.toString()+" -> "+directory.mkdirs())

            logMsg(TAG, "Rozmiar listy pytań: ${lastQuestionBase.listOfQuestions.size}")

            for (question in lastQuestionBase.listOfQuestions)
                downloadQuestionPhotos(questionRequest, question, context)



            Thread{
                val time = System.currentTimeMillis()
                while (counter.getI() != 0) {
                    Thread.sleep(5)
                    if (System.currentTimeMillis() - time > TIMEOUT) {
                        Toast.makeText(context, "Nie udało się pobrać wszystkich zdjęć", Toast.LENGTH_SHORT).show()
                        notify(false)
                    }
                }

                logMsg(TAG+"thread", "udało się pobrać wszystkie zdjęcia")
                notify(true)
            }.start()

            ready = false
        }.start()
    }

    fun deleteTestPhotos(questionRequest: QuestionRequest, context: Context): Boolean {
        val directory = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}")
        return directory.delete()

    }

    fun getPhoto(questionRequest: QuestionRequest, questionName: String, context: Context): Bitmap? {
        val directory = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}")

        if (!directory.exists()) {
            Log.e(TAG, "Folder testu nie istnieje!")
            Toast.makeText(context, "Błąd wewnętrzny", Toast.LENGTH_SHORT).show()
            return null
        }

        var photoName = questionName.replace("txt", "jpg")

        var i = 0
        while (photoName[i] == '0')
            i++

        photoName = photoName.substring(i)

        logMsg(TAG, "pobieram zdjęcie  $APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")

        val file = File(context.filesDir, "/$APPLICATION_NAME/photos${questionRequest.getAsPath()}/$photoName")

        return BitmapFactory.decodeFile(file.absolutePath)
    }

    override fun run(obj: Any) {
        if (obj is QuestionBase) {
            lastQuestionBase = obj
            ready = true
        }
    }

    override fun notify(obj: Any) {
        observers.forEach { it -> it.run(obj) }
    }

    override fun remove(observer: Observer) {
        observers.remove(observer)
    }

    override fun add(observer: Observer) {
        observers.add(observer)
    }
}