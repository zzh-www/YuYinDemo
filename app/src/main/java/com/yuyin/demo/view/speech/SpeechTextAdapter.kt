package com.yuyin.demo.view.speech

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.yuyin.demo.R
import com.yuyin.demo.viewmodel.RunningAsrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SpeechTextAdapter(private val dataList: List<SpeechText>, private val viewModel: RunningAsrViewModel) :
    RecyclerView.Adapter<SpeechTextAdapter.ViewHolder>() {

    var mRecyclerView: RecyclerView? = null
    var isEdit = MutableStateFlow(false)

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        mRecyclerView = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.text_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val speechText = dataList[position]
        holder.speechView.setText(speechText.text)
        holder.speechView.isInEditMode
        viewModel.viewModelScope.launch(Dispatchers.Main) {
            isEdit.emit(false)
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speechView = view.findViewById<View>(R.id.speechText) as TextInputEditText

    }
}