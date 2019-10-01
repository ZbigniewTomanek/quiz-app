package com.shadowtesseract.politests.database.model

data class QuestionBasesLocalData(var listOfDatabases: MutableList<QuestionRequest> = mutableListOf())
{
    fun getQuestionbasesAsNames(): MutableList<String> = listOfDatabases.map { it -> it.toStringReversed() }.toMutableList()

}