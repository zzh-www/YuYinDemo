package com.yuyin.demo.view.file

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyin.demo.databinding.FragmentFilesManagerBinding
import java.io.File
import java.io.IOException

class FilesManager : Fragment() {
    private var binding: FragmentFilesManagerBinding? = null
    private val fileItemArrayList = ArrayList<FileItem>()
    private var adapter: FileAdapter? = null
    private var recyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_files_manager, container, false);
        binding = FragmentFilesManagerBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding!!.recyclerFile
        recyclerView!!.layoutManager = linearLayoutManager
        val dir_path = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        var file: File? = null
        try {
            file = File(dir_path!!.canonicalPath + File.separator + "YuYin")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val files = file!!.listFiles { dir: File?, name: String -> name.endsWith(".txt") }
        // 返回null
        if (files != null) {
            for (item in files) {
                fileItemArrayList.add(FileItem(item))
            }
        }
        adapter = FileAdapter(fileItemArrayList)
        recyclerView!!.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}