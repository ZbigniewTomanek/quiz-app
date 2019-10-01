package com.shadowtesseract.politests.database.model

data class PathIndex(val names : MutableList<String> = mutableListOf(),
                     val hashes : MutableList<String> = mutableListOf())
{
    @Override
    override fun toString(): String {
        return names.toString()+" -> "+hashes.toString()
    }
}