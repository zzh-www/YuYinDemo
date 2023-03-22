package com.yuyin.demo.view.file

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuyin.demo.R
import com.yuyin.demo.models.LocalResult
import com.yuyin.demo.utils.YuYinUtil.compressFiles
import com.yuyin.demo.utils.YuYinUtil.moshi
import com.yuyin.demo.viewmodel.FilesManagerViewModel
import com.yuyin.demo.viewmodel.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class FileAdapter(
    private val data_list: ArrayList<FileItem>,
    private val viewModel: FilesManagerViewModel,
    private val yuyinViewModel: YuyinViewModel
) :
    RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    val tag = "FileAdapter"
    private lateinit var mRecyclerView: RecyclerView

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = recyclerView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.file_text.setOnClickListener {
            val jsonPath = data_list[viewHolder.bindingAdapterPosition].jsonFile
            with(viewModel) {
                viewModelScope.launch {
                    openFile.emit(jsonPath)
                }
            }
        }
        viewHolder.file_bt.setOnClickListener {
            val jsonPath = data_list[viewHolder.bindingAdapterPosition].jsonFile
            val textPath = data_list[viewHolder.bindingAdapterPosition].textFile
            val zipFile = File(yuyinViewModel.yuYinTmpDir, jsonPath.nameWithoutExtension + ".zip")
            yuyinViewModel.viewModelScope.launch(Dispatchers.IO) {
                val audioPath =
                    File(jsonPath.parent, LocalResult.fromJson(moshi, jsonPath).audioFile)
                compressFiles(listOf(jsonPath, audioPath, textPath), zipFile)
                withContext(Dispatchers.Main) {
                    val intent = Intent(Intent.ACTION_SEND)
                    val uri = FileProvider.getUriForFile(
                        parent.context,
                        "com.yuyin.demo.fileprovider",
                        zipFile
                    )
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.type = "application/zip"
                    parent.context.startActivity(intent)
                }
            }
        }
        viewHolder.delete_bt.setOnClickListener {
            val position = viewHolder.bindingAdapterPosition
            if (position == RecyclerView.NO_POSITION) {
                Log.e(tag, "NO_POSITION")
                return@setOnClickListener
            }
            viewModel.viewModelScope.launch(Dispatchers.IO) {
                val jsonPath = data_list[viewHolder.bindingAdapterPosition].jsonFile
                val textPath = data_list[viewHolder.bindingAdapterPosition].textFile
                val audioPath =
                    File(jsonPath.parent, LocalResult.fromJson(moshi, jsonPath).audioFile)
                withContext(Dispatchers.Main) {
                    val dialog =
                        MaterialAlertDialogBuilder(parent.context)
                            .setIcon(R.drawable.delete__icon)
                            .setTitle(R.string.delete_dialog_title)
                            .setMessage(jsonPath.nameWithoutExtension)
                            .setNegativeButton(R.string.cancel) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .setPositiveButton(R.string.confirm) { _, _ ->
                                jsonPath.delete()
                                textPath.delete()
                                audioPath.delete()
                                data_list.removeAt(viewHolder.bindingAdapterPosition)
                                this@FileAdapter.notifyItemRemoved(position)
                            }.create()
                    dialog.show()
                }
            }
        }
        return viewHolder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileItem = data_list[position]
        holder.file_text.text = fileItem.file_name
    }

    override fun getItemCount(): Int {
        return data_list.size
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val file_text: TextView
        val file_bt: AppCompatImageView
        val delete_bt: AppCompatImageView

        init {
            file_text = view.findViewById<View>(R.id.file_card_text) as TextView
            file_bt = view.findViewById(R.id.file_card_bt)
            delete_bt = view.findViewById(R.id.file_delete_bt)
        }
    }
}