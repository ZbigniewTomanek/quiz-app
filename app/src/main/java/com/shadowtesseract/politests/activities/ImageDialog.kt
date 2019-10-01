package com.shadowtesseract.politests.activities

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import com.google.gson.Gson
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import com.shadowtesseract.politests.database.model.QuestionRequest
import com.shadowtesseract.politests.logger.Level
import com.shadowtesseract.politests.logger.Logger.logErr
import uk.co.senab.photoview.PhotoViewAttacher


class ImageDialog : Activity() {

    lateinit var view: ImageView
    private val TAG = "ImageDialog"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_dialog)

        view = findViewById(R.id.question_photo)
        view.isClickable = true
        view.setOnClickListener {
            finish()
        }

        val extras = intent.extras
        val gson = Gson()

        val qrString = extras!!.getString(PUT_QUESTION_REQUEST)

        val questionRequest = gson.fromJson<QuestionRequest>(qrString, QuestionRequest::class.java)
        val questionName = extras.getString(PUT_QUESTION_NAME) ?: ""
        val answerPhotoName = extras.getString(PUT_ANSWER_PHOTO_NAME)

        val photo: Bitmap? =
                if (answerPhotoName.isEmpty())
                    instance.getPhoto(questionRequest, questionName, this)
                else
                    instance.getPhoto(questionRequest, answerPhotoName, this)

        if (photo == null) {
            setResult(Activity.RESULT_CANCELED)
            logErr(TAG, "W bibliotece brakuje zdjęcia: $answerPhotoName w pytaniu $questionName w teście ${questionRequest.getName()}", Level.HIGH)
            Toast.makeText(this, "Brak zdjęcia - próbuję je pobrać", Toast.LENGTH_SHORT).show()
            finish()
        }
        else
        {
            setResult(Activity.RESULT_OK)
            view.setImageBitmap(photo)
            val attacher = PhotoViewAttacher(view)
        }
    }


}
