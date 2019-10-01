package com.shadowtesseract.politests.database.model

import com.shadowtesseract.politests.database.engine.Tools

data class Question (
        var number: String = "",
        var has_photo : String = "",
        var question : String = "",
        var answers : List<String> = mutableListOf(),
        var id : String = "",
        var right_answers : List<String> = mutableListOf()) {

    private var temp = listOf<Boolean>()
    var answerHasPhoto: List<Boolean> = listOf()
            get() = if (temp.isEmpty()) {
                temp = List(answers.size) { it -> Tools.doesLinkPhoto(answers[it])}
                temp
            } else temp





    override fun toString(): String {
        return "'$number $id $question'"
    }

}