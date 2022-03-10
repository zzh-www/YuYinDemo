package com.yuyin.demo;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Environment;
import android.provider.DocumentsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuyin.demo.databinding.FragmentFilesManagerBinding;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.PhantomReference;
import java.util.ArrayList;


public class FilesManager extends Fragment {

    private FragmentFilesManagerBinding binding;
    private final String LOG_tag = "YUYIN_FILEMANAGER";

    private ArrayList<FileItem> fileItemArrayList = new ArrayList<>();
    private FileAdapter adapter;
    private RecyclerView recyclerView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_files_manager, container, false);
        binding = FragmentFilesManagerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        recyclerView = binding.recyclerFile;
        recyclerView.setLayoutManager(linearLayoutManager);
        File dir_path = getContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File file = null;
        try {
            file = new File(dir_path.getCanonicalPath()+File.separator+"YuYin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        File[] files = file.listFiles((dir, name) -> name.endsWith(".txt"));
        // 返回null
        if (files != null) {
            for (File item : files) {
                fileItemArrayList.add(new FileItem(item));
            }
        }
        adapter = new FileAdapter(fileItemArrayList);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}