package com.yuyin.demo.view.speech

import android.text.Editable
import android.text.TextWatcher
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

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)

        val position = holder.absoluteAdapterPosition
        holder.speechView.setOnFocusChangeListener { _, hasFocus ->
            val textWatcher = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable?) {
                    if (holder.onFocus) {
                        Log.i(TAG, "text change on $position")
                    } else {
                        Log.d(TAG, "auto change $position")
                    }
                }
            }
            holder.onFocus = hasFocus
            if (hasFocus) {
                Log.i(TAG, "position: $position addTextChangedListener")
                Log.i(TAG, "on focus")
                holder.speechView.addTextChangedListener(textWatcher)
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    isFocus.emit(true)
                }
            } else {
                Log.i(TAG, "position: $position removeTextChangedListener")
                Log.i(TAG, "not on focus")
                holder.speechView.removeTextChangedListener(textWatcher)
                viewModel.viewModelScope.launch(Dispatchers.Main) {
                    isFocus.emit(false)
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.speechView.setOnFocusChangeListener { _, hasFocus ->
            Log.i(TAG, "focus $hasFocus")
        }
        holder.onFocus = false
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val speechText = dataList[holder.absoluteAdapterPosition]
        holder.speechView.setText(speechText.text)
        if (viewModel.needToShowTime) {
            holder.timeTextView.text = speechText.timeInfo
        } else {
            holder.timeTextView.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val speechView = view.findViewById<View>(R.id.speechText) as TextInputEditText
        val timeTextView = view.findViewById<View>(R.id.timeInfo_asr) as TextView
        var onFocus = false
    }
}