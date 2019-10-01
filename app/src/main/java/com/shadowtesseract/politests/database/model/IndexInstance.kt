package com.shadowtesseract.politests.database.model

data class IndexInstance (
        var name: String = "",
        var hash: String = "",
        var number_of_questions:Int = 0,
        var version: Int = -1,
        var authors : MutableList<String> = mutableListOf())
{
    @Override
    override fun toString(): String {
        return name
    }

    fun toList(): MutableList<String>
    {
        var list : MutableList<String> = arrayListOf()
        list.add("name: "+name)
        list.add("hash: "+hash)
        list.add("number_of_questions: "+number_of_questions)
        list.add("version: "+version)
        list.add("authors:")
        list.addAll(authors)
        return list
    }
}
