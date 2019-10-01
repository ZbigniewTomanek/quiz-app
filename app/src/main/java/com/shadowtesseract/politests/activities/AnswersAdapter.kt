package com.shadowtesseract.politests.activities

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.Observer
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import com.shadowtesseract.politests.database.engine.StorageConnector
import com.shadowtesseract.politests.database.engine.Tools
import com.shadowtesseract.politests.database.model.QuestionRequest


class AnswersAdapter(val mCtx : Context, val layoutResId : Int, val answers : List<String>,
                     private val questionRequest: QuestionRequest, val show: (Context, String) -> (Unit))
    : ArrayAdapter<String>(mCtx, layoutResId, answers) {

    var clickTime = SystemClock.uptimeMillis()


    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(mCtx)
        val view : View = layoutInflater.inflate(layoutResId, null)
        val answer = view.findViewById<TextView>(R.id.answerText)

        val text = answers[position]
        val photoName = Tools.getPhotoNameFromAnswer(text)


        class PhotoObserver: Observer {
            override fun run(obj: Any) {
                val bitmap = StorageConnector.getPhoto(questionRequest, photoName, context)
                if (bitmap != null) {
                    val background = view.findViewById<View>(R.id.answer_background)
                    val scale = context.resources.displayMetrics.density
                    val pd = (10 * scale + 0.5f).toInt()
                    background.setPadding(pd, pd, pd, pd)

                    answer.text = ""
                    val drawable = BitmapDrawable(bitmap)
                    answer.background = drawable
                } else Toast.makeText(context, "Nie udało się, pomin to pytanie :c", Toast.LENGTH_SHORT).show()

            }
        }

        if (text != photoName) {
            val bitmap = StorageConnector.getPhoto(questionRequest, photoName, context)
            if (bitmap != null) {
                val background = view.findViewById<View>(R.id.answer_background)
                val scale = context.resources.displayMetrics.density
                val pd = (10 * scale + 0.5f).toInt() //padding w dp
                background.setPadding(pd, pd, pd, pd)

                answer.text = ""
                val drawable = BitmapDrawable(bitmap)
                answer.background = drawable
            } else {
                Toast.makeText(context, "Próbuję pobrać brakujące zdjęcie", Toast.LENGTH_SHORT).show()
                Thread {
                    while (!instance.isOnline()) {
                        Thread.sleep(500)
                    }

                    StorageConnector.downloadPhoto(questionRequest, photoName, context, PhotoObserver())
                }
                val name = "Zdjęcie ${(65 + position).toChar()}"
                answer.text = name
            }

        } else {
            answer.text = text
        }

        return view
    }
}