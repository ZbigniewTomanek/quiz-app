package com.shadowtesseract.politests.database.engine

interface Observable
{
    fun notify(obj: Any)
    fun remove(observer: Observer)
    fun add(observer: Observer)
}