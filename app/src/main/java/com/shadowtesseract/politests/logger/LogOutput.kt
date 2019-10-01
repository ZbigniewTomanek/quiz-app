package com.shadowtesseract.politests.logger

interface LogOutput {
    fun logErr(TAG: String, message: String, level: Level)
    fun logMsg(TAG: String, message: String, level: Level)
    fun logStats(TAG: String, message: String, info: MutableMap<String, String>, statsType: StatsType)
    fun init()
}