package com.yuyin.demo.view.edit

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.yuyin.demo.R
import com.yuyin.demo.models.AudioPlay
import com.yuyin.demo.models.ResultItem
import com.yuyin.demo.viewmodel.EditViewModel
import kotlinx.coroutines.launch
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class ResultAdapter(
    val results: MutableList<ResultItem>,
    private val viewModel: EditViewModel,
    val startIcon:Drawable,
    val endIcon:Drawable
) :
    RecyclerView.Adapter<ResultAdapter.ViewHolder>() {
    val TAG = "ResultAdapter"
    private lateinit var mRecyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.text_with_audio, parent, false)
        val viewHolder = ViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text = results[position].speechText
        val start = results[position].start
        val end = results[position].end
        holder.textView.text = text
        holder.timeInfo.text = "${start/1000f} ~ ${end/1000f}"
        holder.audioBt.setOnClickListener {
            viewModel.viewModelScope.launch {
                viewModel.audioConfig.emit(
                    AudioPlay.AudioConfig(
                        start,
                        end,
                        results[position].state,
                        position
                    )
                )
            }
        }
        holder.textView.doAfterTextChanged {
            Log.i(TAG, "string: $it")
            Log.i(TAG, "position: $position")
            results[position].speechText = it.toString()
            viewModel.localResult.speechText[position] = it.toString()
        }
    }

    override fun getItemCount(): Int {
        return results.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView
        val timeInfo: TextView
        val audioBt: MaterialButton

        init {
            textView = view.findViewById(R.id.speechText)
            timeInfo = view.findViewById(R.id.timeInfo)
            audioBt = view.findViewById(R.id.audio_play)

        }
    }
}