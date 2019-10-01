package com.shadowtesseract.politests.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.shadowtesseract.politests.R
import com.shadowtesseract.politests.logger.Level
import com.shadowtesseract.politests.logger.Logger.logErr
import com.shadowtesseract.politests.logger.Logger.logMsg

private const val TAG = "AUTH"
const val LOG_IN = "login"
const val LOG_OUT= "logout"
const val LOG_EXTRAS = "xtr"

class LogInActivity : AppCompatActivity() {
    companion object {
        private const val RC_SIGN_IN = 123
        private val auth = FirebaseAuth.getInstance()
        fun isLoggedIn() =  auth.currentUser != null
        fun userID(): String {
            if (auth.currentUser == null)
                return "null"
            return auth.currentUser!!.uid
        }
    }

    lateinit var mainView: View
    lateinit var sadfaceView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        //TODO dodać coś w sylu showcase, które przywita użytkownika i zapowie logowanie

        mainView = findViewById(R.id.login_view)
        sadfaceView = mainView.findViewById(R.id.messageTextView)

        val extras = intent.extras
        if (extras == null) {
            logMsg(TAG, "Nie podano stringa z akcją")
            finish()
        }

        val string = extras!!.getString(LOG_EXTRAS) ?: ""
        when(string) {
            LOG_IN -> logIn()
            LOG_OUT -> logOut()
            "" -> logMsg(TAG, "Podano do aktywności zły string")
        }
    }

    private fun logIn() {
        if (!isLoggedIn()) {
            logMsg(TAG, "Próbuję zalogować użytkownika")
            val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build())

            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setLogo(R.mipmap.ic_launcher)
                            .setTheme(R.style.LogTheme)
                            .build(),
                    RC_SIGN_IN)

            logMsg(TAG, "jestem tu")
        } else {
            logMsg(TAG, "Użytkownik już jest zalogowany")
            finish()
        }



    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        logMsg(TAG, "Użytkownik zakończył akcję")

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                logMsg(TAG, "Poprawnie zalogowano")
                finish()
            } else {

                if (response == null)
                    showSnackbar("Zalogowanie się jest konieczne", ::logIn)
                else if (response.error!!.errorCode == ErrorCodes.NO_NETWORK)
                    showSnackbar("Brak internetu", ::logIn)
                else {
                    showSnackbar("Nienznay błąd", ::logIn)
                    logErr(TAG, "Dziwny błąd logowania", Level.HIGH)
                }

            }
        }
    }

    private fun logOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener { logMsg(TAG, "Poprawnie wylogowano użytkownika")
                    Toast.makeText(this, "Poprawnie wylogowano", Toast.LENGTH_SHORT).show()
                    finish()}
                .addOnCanceledListener { showSnackbar("Nie udało się wylogować", ::logOut) }
    }

    private fun delete() {
        AuthUI.getInstance()
                .delete(this)
                .addOnCompleteListener { logMsg(TAG, "Poprawnie usunięto konto")}
    }

    private fun showSnackbar(message: String, action: () -> Unit) {
        sadfaceView.visibility = View.VISIBLE
        Snackbar
                .make(mainView, message, Snackbar.LENGTH_INDEFINITE)
                .setAction("Ponów") { action() }
                .setActionTextColor(Color.RED)
                .show()
    }
}