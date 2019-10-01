package com.shadowtesseract.politests.database.model


data class QuestionBase (var listOfQuestions : List<Question>, var version: Int = -1, var name : String= "")
{
    @Override
    override fun toString(): String {
        return "Name: $name, Version: $version, Size: ${listOfQuestions.size}"
    }
}

