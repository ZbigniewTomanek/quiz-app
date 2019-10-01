package com.shadowtesseract.politests.database.engine

import com.google.gson.Gson
import java.util.*

data class UserStatistics(val answeredQuestionsByDay: MutableMap<Int, Int> = mutableMapOf(),
                          var totalQuestions: Int = 0,
                          var correctAnswers: Int = 0,
                          var wrongAnswers: Int = 0) {

    private val DAYS = 7
    init {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        for (i in 0 until DAYS) {
            answeredQuestionsByDay[currentDay + i] = 0
        }
    }
    companion object {
        fun deserialize(data : String) : UserStatistics {
            return Gson().fromJson<UserStatistics>(data, UserStatistics::class.java)
        }
    }

    /**
     * Increments number of answered question at current day & number of answered questions & number of correct/wrong answers
     * If MutableMap contains too many days (more than DAYS), first day is removing and current day is added at the end of map
     */
    fun addAnsweredQuestion(correctAnswer : Boolean) {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        if (answeredQuestionsByDay.containsKey(currentDay)) {
            val currentNumberOfAnsweredQuestions = answeredQuestionsByDay[currentDay] ?: 0
            answeredQuestionsByDay[currentDay] = currentNumberOfAnsweredQuestions + 1
        }
        else {
            if (answeredQuestionsByDay.size >= DAYS) {
                answeredQuestionsByDay.remove(currentDay - DAYS)
            }
            answeredQuestionsByDay[currentDay] = 1
        }

        increaseTotalQuestions()
        increaseCorrectOrWrongAnswers(correctAnswer)
    }

    fun addAnsweredQuestions(correctAnswers: Int, wrongAnswers: Int) {
        val calendar = Calendar.getInstance()
        val totalAnswers = correctAnswers + wrongAnswers
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        if (answeredQuestionsByDay.containsKey(currentDay)) {
            val currentNumberOfAnsweredQuestions = answeredQuestionsByDay[currentDay] ?: 0
            answeredQuestionsByDay[currentDay] = currentNumberOfAnsweredQuestions + totalAnswers
        }
        else {
            if (answeredQuestionsByDay.size >= DAYS) {
                answeredQuestionsByDay.remove(currentDay - DAYS)
            }
            answeredQuestionsByDay[currentDay] = totalAnswers
        }

        increaseTotalQuestions(totalAnswers)
        increaseCorrectOrWrongAnswers(true, correctAnswers)
        increaseCorrectOrWrongAnswers(false, wrongAnswers)

    }

    fun getNumberOfAnsweredQuestions(day : Int) : Int {
        return answeredQuestionsByDay[day] ?: 0
    }

    fun getDays() : MutableSet<Int> {
        return answeredQuestionsByDay.keys
    }

    fun getNumberOfAnsweredQuestions() : MutableCollection<Int> {
        return answeredQuestionsByDay.values
    }

    private fun increaseTotalQuestions(amount : Int = 1) {
        totalQuestions += amount
    }


    private fun increaseCorrectOrWrongAnswers(correctAnswer : Boolean, amount: Int = 1) {
        if (correctAnswer) {
            correctAnswers += amount
        }
        else {
            wrongAnswers += amount
        }
    }


    fun serialize() : String {
        return Gson().toJson(this)
    }

    fun deserialize(data : String) : UserStatistics {
        return Gson().fromJson<UserStatistics>(data, UserStatistics::class.java)
    }

}