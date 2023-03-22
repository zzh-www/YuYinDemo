package com.yuyin.demo.view.file

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyin.demo.R
import com.yuyin.demo.databinding.FragmentFilesManagerBinding
import com.yuyin.demo.viewmodel.FilesManagerViewModel
import com.yuyin.demo.viewmodel.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log


class FilesManager : Fragment() {
    val TAG = "FilesManager"
    private var binding: FragmentFilesManagerBinding? = null
    private val fileItemArrayList = ArrayList<FileItem>()
    private var adapter: FileAdapter? = null
    private var recyclerView: RecyclerView? = null
    val yuyinViewModel: YuyinViewModel by activityViewModels()
    val filesManagerViewModel: FilesManagerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG,"onCreate")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.i(TAG,"onCreateView")
        binding = FragmentFilesManagerBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.i(TAG,"onViewCreated")
        super.onViewCreated(view, savedInstanceState)
        val linearLayoutManager = LinearLayoutManager(context)
        recyclerView = binding!!.recyclerFile
        recyclerView!!.layoutManager = linearLayoutManager
        val dir_path = yuyinViewModel.yuYinDataDir.toFile()
        val files = dir_path.listFiles { _: File?, name: String -> name.endsWith(".json") }
        // 返回null
        if (files != null) {
            for (item in files) {
                fileItemArrayList.add(FileItem(item))
            }
        }
        adapter = FileAdapter(fileItemArrayList,filesManagerViewModel,yuyinViewModel)
        recyclerView!!.adapter = adapter
        openFileListener()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.i(TAG,"onDestroyView")
        fileItemArrayList.clear()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG,"onDestroy")
    }

    private fun openFileListener()  {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            filesManagerViewModel.openFile.collect {
                Navigation.findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_filesManager_dest_to_edit_dest, Bundle().apply {
                    putString("file", it.absolutePath)
                })
            }
        }
    }
}