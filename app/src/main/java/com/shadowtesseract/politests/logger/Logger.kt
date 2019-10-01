package com.shadowtesseract.politests.logger

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log

enum class Level(value: Int) {
    HIGH(2),
    LOW(1),
    DEBUG(0)
}

enum class StatsType{
    SESSION_TIME,
    DOWNLOADING_TIME,
    DOWNLOADED_TESTS,
    DELETED_TESTS,
    COFFEE,
    OPENED_TEST
}

object Logger {
    private val listOfOutputs = mutableListOf<LogOutput>()
    private const val TAG = "LG"
    private val fo = FirebaseOutput

    @SuppressLint("HardwareIds")
    fun initID(context: Context) {
        Log.d(TAG, "Wczytywanie unikalnego ID")

        fo.init()
        listOfOutputs.add(fo)

        Log.d(TAG, "logger dla firebase jest aktywny")
    }

    fun logMsg(tag: String, message: String, level: Level = Level.DEBUG) {
        Log.d(tag, message)

        if (level > Level.DEBUG)
            for (output in listOfOutputs)
                output.logMsg(tag, message, level)
    }

    fun logErr(tag: String, message: String, level: Level = Level.DEBUG) {
        Log.e(tag, message)

        //if (level > Level.DEBUG)
            for (output in listOfOutputs)
                output.logErr(tag, message, level)
    }

    /**
    Jeśli chcecie zapisać jakieś statystyki, to musicie wywołać tą funkcję podając jej słownik z danymi, któe chcecie przechować.
    Jeśli typu akcji nie ma w StatsType, to dodajcie ją tam
     */
    fun logStats(TAG: String, message: String, info: MutableMap<String, String>, type: StatsType) {
        for (output in listOfOutputs) {
            Log.d("STATS", "Wysyłam staystyki: $info")
            output.logStats(TAG, message, info, type)
        }

    }
}
