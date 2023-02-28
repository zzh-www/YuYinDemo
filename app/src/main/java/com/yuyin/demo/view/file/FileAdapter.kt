package com.yuyin.demo.view.file

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.yuyin.demo.R

class FileAdapter(private val data_list: List<FileItem>) :
    RecyclerView.Adapter<FileAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_item, parent, false)
        val viewHolder = ViewHolder(view)
        viewHolder.file_text.setOnClickListener { v: View? ->
            val txt_path = data_list[viewHolder.bindingAdapterPosition].file_path
            // 打开txt文件
            val intent = Intent(Intent.ACTION_VIEW)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val uri = FileProvider.getUriForFile(
                parent.context,
                "com.yuyin.demo.fileprovider",
                txt_path
            )
            intent.flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.setDataAndType(uri, "text/plain")
            /*
                MIME 媒体类型，如 image/jpeg 或 audio/mpeg4-generic。子类型可以是星号通配符 (*)，以指示任何子类型都匹配。
                Intent 过滤器经常会声明仅包含 android:mimeType 属性的 <data>。
                intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, file_path);
            */
            parent.context.startActivity(intent)
        }
        viewHolder.file_bt.setOnClickListener { v: View? ->
            val text_path = data_list[viewHolder.bindingAdapterPosition].file_path
            val intent = Intent(Intent.ACTION_SEND)
            val uri = FileProvider.getUriForFile(
                parent.context,
                "com.yuyin.demo.fileprovider",
                text_path
            )
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.type = "text/plain"
            parent.context.startActivity(intent)
        }
        viewHolder.delete_bt.setOnClickListener { v:View? ->
            val text_path = data_list[viewHolder.bindingAdapterPosition].file_path
            text_path.delete()
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