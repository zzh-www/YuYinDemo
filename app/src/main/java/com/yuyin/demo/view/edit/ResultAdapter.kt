package com.yuyin.demo.view.edit

import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.divider.MaterialDivider
import com.yuyin.demo.R
import com.yuyin.demo.models.AudioPlay
import com.yuyin.demo.models.ResultItem
import com.yuyin.demo.viewmodel.EditViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class ResultAdapter(
    val results: MutableList<ResultItem>,
    private val viewModel: EditViewModel,
    val startIcon: Drawable,
    val endIcon: Drawable,
    val lifecycleScope: LifecycleCoroutineScope,
    val playLabel: String,
    val stopLabel: String
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
        Log.i(TAG, "onDetachedFromRecyclerView")
        return viewHolder
    }


    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        Log.i(TAG, "onDetachedFromRecyclerView")
    }

    override fun onViewAttachedToWindow(holder: ViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.absoluteAdapterPosition
        val start = results[position].start
        val end = results[position].end
        Log.i(
            TAG,
            "onViewAttachedToWindow ${position}"
        )
        holder.textView.setOnFocusChangeListener { _, hasFocus ->
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
                        Log.i(TAG, "position: $position text change")
                        results[position].speechText = s.toString()
                        viewModel.localResult.speechText[position] = s.toString()
                    } else {
                        Log.d(TAG, "auto change")
                    }
                }
            }
            holder.onFocus = hasFocus
            if (hasFocus) {
                Log.i(TAG, "position: $position addTextChangedListener")
                holder.textView.addTextChangedListener(textWatcher)
            } else {
                Log.i(TAG, "position: $position removeTextChangedListener")
                holder.textView.removeTextChangedListener(textWatcher)
            }
            Log.i(TAG, "focus $hasFocus")
        }
        if (viewModel.localResult.resultType == 2) {
            holder.audioBt.setOnClickListener {
                lifecycleScope.launch {
                    viewModel.audioConfig.emit(
                        AudioPlay.AudioConfig(
                            start,
                            end,
                            results[position].state,
                            position
                        )
                    )
                    when (results[position].state) {
                        AudioPlay.AudioConfigState.PLAY -> {
                            holder.audioBt.icon = endIcon
                            holder.audioBt.text = stopLabel
                            results[position].state = AudioPlay.AudioConfigState.STOP
                            Log.i(TAG, "$position play->stop")
                            lifecycleScope.launch {
                                supervisorScope {
                                    viewModel.audioItemNotification.collect {
                                        Log.i(TAG, "item $position accept $it")
                                        if (it.id == position && !it.isPlaying) {
                                            // 自己收到通知 isPlaying false 暂停播放
                                            holder.audioBt.icon = startIcon
                                            holder.audioBt.text = playLabel
                                            results[position].state =
                                                AudioPlay.AudioConfigState.PLAY
                                            Log.i(TAG, "$position stop->play")
                                            this.cancel()
                                        } else if (it.id != position && it.isPlaying) {
                                            // 收到通知 其他item要播放
                                            holder.audioBt.icon = startIcon
                                            holder.audioBt.text = playLabel
                                            results[position].state =
                                                AudioPlay.AudioConfigState.PLAY
                                            Log.i(TAG, "$position stop->play")
                                            this.cancel()
                                        }
                                    }
                                }
                            }
                        }

                        AudioPlay.AudioConfigState.STOP -> {
                            // 只需停止即可
                            holder.audioBt.icon = startIcon
                            holder.audioBt.text = playLabel
                            results[position].state = AudioPlay.AudioConfigState.PLAY
                            Log.i(TAG, "$position stop->play")
                        }
                    }
                }
            }
        } else {
            holder.audioBt.isEnabled = false
            holder.audioBt.visibility = View.GONE
        }
    }

    override fun onViewDetachedFromWindow(holder: ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.absoluteAdapterPosition
        Log.i(
            TAG,
            "onViewDetachedFromWindow ${holder.absoluteAdapterPosition}"
        )
        // 恢复初始状态离开屏幕
        if (results[holder.absoluteAdapterPosition].state == AudioPlay.AudioConfigState.STOP) {
            viewModel.viewModelScope.launch {
                results[holder.absoluteAdapterPosition].state = AudioPlay.AudioConfigState.PLAY
                viewModel.audioConfig.emit(
                    AudioPlay.AudioConfig(
                        0,
                        0,
                        AudioPlay.AudioConfigState.STOP,
                        holder.absoluteAdapterPosition
                    )
                )
            }
            holder.audioBt.icon = startIcon
            holder.audioBt.text = playLabel
        }
        holder.textView.setOnFocusChangeListener { _, hasFocus ->
            Log.i(TAG, "focus $hasFocus")
        }
        holder.audioBt.setOnClickListener {

        }
        holder.onFocus = false
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        Log.i(TAG, "onViewRecycled ${holder.absoluteAdapterPosition}")
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentPosition = holder.absoluteAdapterPosition
        val text = results[currentPosition].speechText
        val start = results[currentPosition].start
        val end = results[currentPosition].end

        holder.textView.text = text

        if (viewModel.localResult.resultType > 0) {
            holder.timeInfo.text = "${start / 1000f}s ~ ${end / 1000f}s"
        } else {
            holder.timeInfo.visibility = View.GONE
            holder.divider.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return results.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var onFocus: Boolean = false
        val textView: TextView
        val timeInfo: TextView
        val audioBt: MaterialButton
        val divider: MaterialDivider

        init {
            textView = view.findViewById(R.id.speechText)
            timeInfo = view.findViewById(R.id.timeInfo)
            audioBt = view.findViewById(R.id.audio_play)
            divider = view.findViewById(R.id.divider)
        }
    }

    /***
     *
     */
    data class Notification(val id: Int, val isPlaying: Boolean)
}