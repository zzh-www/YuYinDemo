package com.yuyin.demo;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {
    private List<FileItem> data_list;

    public FileAdapter(List<FileItem> data_list) {
        this.data_list = data_list;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView file_text;

        private final AppCompatImageView file_bt;

        public ViewHolder(View view) {
            super(view);
            this.file_text = (TextView) view.findViewById(R.id.file_card_text);
            this.file_bt = view.findViewById(R.id.file_card_bt);
        }

        public TextView getFile_text() {
            return file_text;
        }

        public AppCompatImageView getFile_bt() {
            return file_bt;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item, parent, false);
        ViewHolder viewHolder =  new ViewHolder(view);

//        String dir_path = parent.getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        viewHolder.file_text.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onClick(View v) {
                File text_path = data_list.get(viewHolder.getBindingAdapterPosition()).getFile_path();
                // 打开txt文件
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                Uri uri = FileProvider.getUriForFile(parent.getContext(),"com.yuyin.demo.fileprovider",text_path);
                intent.setDataAndType(uri,"text/plain");
                /*
                    MIME 媒体类型，如 image/jpeg 或 audio/mpeg4-generic。子类型可以是星号通配符 (*)，以指示任何子类型都匹配。
                    Intent 过滤器经常会声明仅包含 android:mimeType 属性的 <data>。
                    intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, file_path);
                */
                parent.getContext().startActivity(intent);

            }
        });

        viewHolder.file_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File text_path = data_list.get(viewHolder.getBindingAdapterPosition()).getFile_path();
                Intent intent = new Intent(Intent.ACTION_SEND);
                Uri uri = FileProvider.getUriForFile(parent.getContext(),"com.yuyin.demo.fileprovider",text_path);
                intent.putExtra(Intent.EXTRA_STREAM,uri);
                intent.setType("text/plain");
                parent.getContext().startActivity(intent);
            }
        });

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FileItem fileItem = data_list.get(position);
        holder.getFile_text().setText(fileItem.getFile_name());
    }



    @Override
    public int getItemCount() {
        return data_list.size();
    }


}
