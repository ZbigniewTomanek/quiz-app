package com.shadowtesseract.politests.activities

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.shadowtesseract.politests.R

class TestNameAdapter(val mCtx : Context, val layoutResId : Int, val namesList : List<String>)
    : ArrayAdapter<String>(mCtx, layoutResId, namesList) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val layoutInflater : LayoutInflater = LayoutInflater.from(mCtx)
        val view : View = layoutInflater.inflate(layoutResId, null)

        val textViewName = view.findViewById<TextView>(R.id.textViewName)

        val name = namesList[position]

        textViewName.text = name

        textViewName.isSelected = true // for marquee

        textViewName.requestFocus() // for marquee

        return view
    }
}