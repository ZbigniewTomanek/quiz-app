package com.shadowtesseract.politests.logger

import android.os.Build
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shadowtesseract.politests.activities.LogInActivity
import com.shadowtesseract.politests.database.engine.QuestionBaseManager.QuestionBaseManager.instance
import java.sql.Timestamp

private const val LOG_PATH = "log"
private const val TAG = "FO"

object FirebaseOutput : LogOutput {
    private val database = FirebaseDatabase.getInstance()

    private var received = false
    private var exist = true

    private val infoListener = object : ValueEventListener {
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.value == null)
                exist = false
            received = true
        }

        override fun onCancelled(p0: DatabaseError) {
            received = true
            exist = false
        }
    }

    override fun init() {
        Log.d(TAG, "inicjuję logger")
        if (instance.isOnline()) {
            Log.d(TAG, "online")
            val path = "users/${LogInActivity.userID()}/info"
            val reference = database.reference.child(path)
            reference.addListenerForSingleValueEvent(infoListener)

            Thread {
                while (!received)
                    Thread.sleep(5)

                Log.d(TAG, "Odebrano informację o metadanych")

                if (!exist) {
                    Log.d(TAG, "Tworzenie pliku metadanych")
                    val data = mutableMapOf<String, String>()
                    data["model"] = Build.MODEL
                    data["version"] = Build.VERSION.RELEASE
                    data["device"] = Build.DEVICE

                    reference.setValue(data)
                }
            }.start()
        }
    }

    override fun logErr(TAG: String, message: String, level: Level) {
        val time = System.currentTimeMillis()
        val stamp = Timestamp(time)

        val data = mutableMapOf<String, String>()
        data["tag"] = TAG
        data["message"] = message
        data["time"] = stamp.toString()
        data["user"] = LogInActivity.userID()

        val ref = database.reference.child("$LOG_PATH/error/$level/$time")
        ref.setValue(data)
    }

    override fun logMsg(TAG: String, message: String, level: Level) {
        val time = System.currentTimeMillis()
        val stamp = Timestamp(time)

        val data = mutableMapOf<String, String>()
        data["tag"] = TAG
        data["message"] = message
        data["time"] = stamp.toString()
        data["user"] = LogInActivity.userID()

        val ref = database.reference.child("$LOG_PATH/logs/$level/$time")
        ref.setValue(data)
    }

    override fun logStats(TAG: String, message: String, info: MutableMap<String, String>, statsType: StatsType) {
        Log.d("Stats", "wysyłam do fireabse $message")
        val time = System.currentTimeMillis()
        val stamp = Timestamp(time)

        info["tag"] = TAG
        info["message"] = message
        info["time"] = stamp.toString()

        val ref = database.reference.child("users/${LogInActivity.userID()}/stats/$statsType/$time")
        ref.setValue(info)
    }
}