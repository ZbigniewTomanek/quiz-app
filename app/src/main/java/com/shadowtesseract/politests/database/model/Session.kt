package com.shadowtesseract.politests.database.model

import android.util.Log
import com.google.gson.Gson
import java.util.*

/**
 * This class keeps all data connected to running question session
 * @property questionBase -> this is object od question base of this session
 * @property numberOfRepeats -> number of preferred by user repeats of question before its labeled as "learned"
 * @property numberOfExtraRepeats -> this is number of extra added repeats if user gets question wrong
 * @property maxNumberOfRepetitions -> number of maximal amount of question repeats after numerous mistaken answersListView
 */

const val DEFAULT_NUMBER_OF_EXTRA_REPEATS = 2
const val DEFAULT_NUMBER_OF_REPEATS = 3
const val DEFAULT_MAX_NUMBER_OF_REPEATS = 5
data class Session (val questionBase: QuestionBase, val questionRequest: QuestionRequest,
                    var numberOfRepeats: Int = DEFAULT_NUMBER_OF_REPEATS, var numberOfExtraRepeats: Int = DEFAULT_NUMBER_OF_EXTRA_REPEATS,
                        private val maxNumberOfRepetitions: Int = DEFAULT_MAX_NUMBER_OF_REPEATS, private var correctAnswers: Int = 0, private var wrongAnswers: Int = 0,
                            private var actualQuestionIndex: Int = 0, private var listOfQuestionsToLearn:MutableList<Int> = mutableListOf(),
                                private var indexInToLearnList: Int = 0, private var listOfRepetition:MutableList<Int> = mutableListOf(),
                                    private var receivedFeedback: Boolean = true, private var numOfQuestions:Int = 0)
{

    var currentCorrect = 0
    var currentWrong = 0


    fun init() {
        Log.d("AQ", "probuje inicjowac $numOfQuestions")
        if (numOfQuestions == 0) {
            Log.d("AQ", "inicjuje")
            numOfQuestions = questionBase.listOfQuestions.size
            listOfQuestionsToLearn = MutableList(numOfQuestions) { index -> index }
            listOfRepetition = MutableList(numOfQuestions) { 0 }
        }
    }

    /**
     * Returns randomly selected question from remaining list of unlearned questions
     * Returns null when all questions were learned
     */
    fun getNextQuestion() : Question?
    {
        Log.d("AQ", questionBase.listOfQuestions.toString())
        Log.d("AQ", listOfQuestionsToLearn.size.toString())
        if(!receivedFeedback) throw Exception("User's answer must be returned to session as one of feedback options, before next question can be pulled")
        if(listOfQuestionsToLearn.size==0) return null
        val r = Random()
        indexInToLearnList = r.nextInt(listOfQuestionsToLearn.size)
        actualQuestionIndex = listOfQuestionsToLearn[indexInToLearnList]
        return shuffleAnswers(questionBase.listOfQuestions[actualQuestionIndex])
    }

    private fun shuffleAnswers(question: Question): Question {
        val size = question.answers.size
        val list = MutableList(size, init = {i -> i})
        list.shuffle()

        val newAnswers = mutableListOf<String>()
        val newRightAnswers = mutableListOf<String>()

        for (i in list) {
            newAnswers.add(question.answers[i])
            newRightAnswers.add(question.right_answers[i])
        }

        val q = Question()
        q.number = question.number
        q.has_photo = question.has_photo
        q.id = question.id
        q.question = question.question

        q.answers = newAnswers
        q.right_answers = newRightAnswers

        return q
    }

    /**
     * This function returns serialised Session as a sting
     */
    fun serialiseSession(session: Session) : String
    {
        val gson = Gson()
        return gson.toJson(session)
    }

    /**
     * Should be called if user answer is correct
     */
    fun feedbackCorrectAnswer()
    {
        Log.d("LDC", "zwiększam $correctAnswers")
        receivedFeedback = true
        correctAnswers += 1
        currentCorrect += 1
        listOfRepetition[actualQuestionIndex]++
        if(listOfRepetition[actualQuestionIndex]>=numberOfRepeats)
            listOfQuestionsToLearn.removeAt(indexInToLearnList)
    }
    /**
     * Should be called if user answer is wrong
     */
    fun feedbackWrongAnswer()
    {
        receivedFeedback = true
        wrongAnswers += 1
        currentWrong += 1
        listOfRepetition[actualQuestionIndex]-=numberOfExtraRepeats
        if(getRemainingNumberOfRepetitions()>=maxNumberOfRepetitions)
            listOfRepetition[actualQuestionIndex] = numberOfRepeats - maxNumberOfRepetitions
    }
    /**
     * Should be called if user skipped answer
     */
    fun feedbackSkip()
    {
        receivedFeedback = true
    }

    /**
     * Returns remaining number of repetitions of actual question
     */
    fun getRemainingNumberOfRepetitions():Int = numberOfRepeats-listOfRepetition[actualQuestionIndex]

    /**
     * Returnes index of actual question
     */
    fun getActualQuestonIndex():Int = actualQuestionIndex

    /**
     * Returns number of unlearned questions
     */
    fun getNumberOFQuestionsToLearn():Int = listOfQuestionsToLearn.size

    fun getCorrectAnswers(): Int = correctAnswers
    fun getWrongAnswers(): Int = wrongAnswers
}