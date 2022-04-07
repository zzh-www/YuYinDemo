package com.yuyin.demo

import com.yuyin.demo.SpeechText
import androidx.recyclerview.widget.RecyclerView
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.yuyin.demo.R
import android.widget.TextView

class SpeechTextAdapter(private val dataList: List<SpeechText>) :
    RecyclerView.Adapter<SpeechTextAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.text_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val speechText = dataList[position]
        holder.speech_view.text = speechText.text
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speech_view: TextView

        init {
            speech_view = view.findViewById<View>(R.id.speechText) as TextView
        }
    }
}