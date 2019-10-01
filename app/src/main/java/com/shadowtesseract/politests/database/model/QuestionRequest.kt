package com.shadowtesseract.politests.database.model

/**
 * This class is form needed to request concrete base from QuestionBaseManager
 */

data class QuestionRequest(
        val path : MutableList<String> = mutableListOf(),
        val names : MutableList<String> = mutableListOf())
{

    fun getQuesionRequestForTestIndex() : QuestionRequest
    {
        return QuestionRequest(ArrayList(path.subList(0,path.size-1)), ArrayList(names.subList(0,names.size-1)))
    }

    @Override
    override fun toString(): String {
        return names.joinToString(prefix = "data/", separator = "/").replace(" ", "_")
    }

    fun toStringReversed(): String {
        return names.asReversed().joinToString( separator = "/").replace(" ", "_")
    }

    fun getAsPath() : String
    {
        return path.joinToString(prefix = "data/", separator = "/").replace(" ", "_")
    }

    fun getAsIndex() : String
    {
        return path.joinToString(prefix = "data/", separator = "/").replace(" ", "_").plus("/index")
    }

    fun getAsTestIndex() : String
    {
        return path.joinToString(prefix = "data/", separator = "/").replace(" ", "_").plus("/test_index")
    }

    fun getFile() : String
    {
        return path.joinToString(prefix = "data/", separator = "/").replace(" ", "_")
    }

    fun getName() : String
    {
        return "${names[names.size - 2]} - ${names[names.size - 1]}"
    }
}