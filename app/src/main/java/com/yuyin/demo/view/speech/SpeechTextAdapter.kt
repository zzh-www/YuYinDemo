package com.yuyin.demo.view.speech

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.yuyin.demo.R
import com.yuyin.demo.models.SpeechResult
import com.yuyin.demo.viewmodel.RunningAsrViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class SpeechTextAdapter(
    private val dataList: List<SpeechResult>,
    private val viewModel: RunningAsrViewModel
) :
    RecyclerView.Adapter<SpeechTextAdapter.ViewHolder>() {
    val TAG = "SpeechTextAdapter"
    var mRecyclerView: RecyclerView? = null
    var isFocus = MutableStateFlow(false)

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
        if (viewModel.needToShowTime) {
            holder.timeTextView.text = speechText.timeInfo
        } else {
            holder.timeTextView.visibility = View.GONE
        }
        holder.speechView.setOnFocusChangeListener { _, hasFocus ->
            holder.onFocus = hasFocus
            if (hasFocus) {
                Log.i(tag, "on focus")
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    isFocus.emit(true)
                }
            } else {
                Log.i(tag, "not on focus")
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    isFocus.emit(false)
                }
            }
        }

        holder.speechView.doAfterTextChanged {
            if (holder.onFocus) {
                Log.i(TAG, "text change")
                Log.i(TAG,"change from ${speechText._text} to $it on $position  and holderText = ${holder.speechView.text}")
                speechText._text = it.toString()
            } else {
                Log.i(TAG,"change from ${speechText._text} to $it on $position  and holderText = ${holder.speechView.text} positonText = ${dataList[position].text}")
                Log.i(TAG, "auto change")
            }
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speechView = view.findViewById<View>(R.id.speechText) as TextInputEditText
        val timeTextView = view.findViewById<View>(R.id.timeInfo_asr) as TextView
        var onFocus = false

        init {
            speechView.setOnClickListener {
                Log.i(tag, "on click")
            }
            speechView.onFocusChangeListener = View.OnFocusChangeListener { _, _ ->
                Log.i(tag, "on focus $")
            }
            speechView.doBeforeTextChanged { _, _, _, _ ->
                Log.i(tag, "doBeforeTextChanged")
            }
            speechView.doAfterTextChanged {
                Log.i(tag, "doAfterTextChanged")
            }
        }
    }

    companion object {
        val tag = "SpeechTextAdapter"
    }
}