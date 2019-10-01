package com.shadowtesseract.politests.activities

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.database.engine.QuestionBaseManager
import com.shadowtesseract.politests.logger.Logger
import kotlinx.android.synthetic.main.activity_introduction.*

class Introduction : AppCompatActivity() {
    private val RES_CODE = 69

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_introduction)
    }

    fun start(view : View) {
        val intent = Intent(this, LogInActivity::class.java)
        intent.putExtra(LOG_EXTRAS, LOG_IN)
        startActivity(intent)
        finish()
    }

}
