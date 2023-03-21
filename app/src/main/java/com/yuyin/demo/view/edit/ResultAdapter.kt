package com.yuyin.demo.view.edit

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val text = results[position].speechText
        val start = results[position].start
        val end = results[position].end
        holder.textView.text = text
        holder.timeInfo.text = "${start/1000f}s ~ ${end/1000f}s"
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
                when(results[position].state) {
                    AudioPlay.AudioConfigState.PLAY -> {
                        holder.audioBt.icon = endIcon
                        holder.audioBt.text = stopLabel
                        results[position].state = AudioPlay.AudioConfigState.STOP
                        Log.i(TAG, "$position play->stop")
                        lifecycleScope.launch {
                            supervisorScope {
                                viewModel.audioItemNotification.collect {
                                    Log.i(TAG,"item $position accept $it")
                                    if (it.id == position && !it.isPlaying) {
                                        // 自己收到通知 isPlaying false 暂停播放
                                        holder.audioBt.icon = startIcon
                                        holder.audioBt.text = playLabel
                                        results[position].state = AudioPlay.AudioConfigState.PLAY
                                        Log.i(TAG, "$position stop->play")
                                        this.cancel()
                                    } else if(it.id != position && it.isPlaying){
                                        // 收到通知 其他item要播放
                                            holder.audioBt.icon = startIcon
                                            holder.audioBt.text = playLabel
                                            results[position].state = AudioPlay.AudioConfigState.PLAY
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

    /***
     *
     */
    data class Notification(val id: Int, val isPlaying: Boolean)
}