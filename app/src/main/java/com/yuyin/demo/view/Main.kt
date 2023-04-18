package com.yuyin.demo.view

import android.content.Intent
import android.media.AudioRecord
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.yuyin.demo.R
import com.yuyin.demo.databinding.FragmentMainBinding
import com.yuyin.demo.utils.YuYinUtil
import com.yuyin.demo.viewmodel.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.io.path.exists
import com.yuyin.demo.utils.YuYinUtil.YuYinLog as Log

class Main : Fragment() {

    companion object {
        const val TAG = "Main"
    }

    private var binding: FragmentMainBinding? = null

    private val yuyinViewModel: YuyinViewModel by activityViewModels()

    private fun initShareLog() {
        binding!!.textWelcome.run {
            isLongClickable = true
            setOnLongClickListener {
                Log.i(TAG,"card on long click")
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.share_title)
                    .setMessage(R.string.share_log)
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(R.string.confirm) { dialog, _ ->
                        dialog.dismiss()
                        val zipFile = File(YuyinViewModel.yuYinTmpDir.absolutePath, "log.zip")
                        val compressJob = viewLifecycleOwner.lifecycleScope.async(Dispatchers.IO) {
                            if (!zipFile.exists()) {
                                YuyinViewModel.yuyinLogDir.listFiles()
                                    ?.let { it1 -> YuYinUtil.compressFiles(it1.toList(), zipFile) }
                            }
                            true
                        }
                        val createIntent = viewLifecycleOwner.lifecycleScope.async {
                            val intent = Intent(Intent.ACTION_SEND)
                            val uri = FileProvider.getUriForFile(
                                requireContext(),
                                "com.yuyin.demo.fileprovider",
                                zipFile
                            )
                            intent.putExtra(Intent.EXTRA_STREAM, uri)
                            intent.type = "application/zip"
                            intent
                        }
                        viewLifecycleOwner.lifecycleScope.launch {
                            if (compressJob.await()) {
                                requireContext().startActivity(createIntent.await())
                            } else {
                                Toast.makeText(requireContext(), "can not share log", Toast.LENGTH_SHORT).show()
                                Log.e(
                                    TAG,
                                    "compress log failed ${YuyinViewModel.yuyinLogDir.listFiles()?.joinToString(";")}"
                                )
                            }
                        }
                    }.create().show()
                true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            FragmentMainBinding.inflate(inflater, container, false)
        // 改为 MenuHost 控制toolbar菜单
        // Add menu items without overriding methods in the Activity
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menu.clear()
                menuInflater.inflate(R.menu.bar_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle the menu selection
                when (menuItem.itemId) {
                    // 跳转至设定界面
                    R.id.setting_option -> {
                        if (checkSettingFile()) {
                            findNavController(
                                requireActivity(),
                                R.id.yuyin_nav_host_container_fragment
                            ).navigate(R.id.setting_option_dest)
                        } else {
                            (requireActivity() as MainActivityView).initProfile()
                        }
                        return true
                    }
                }
                return false
            }
        })
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.recordAsrBt.setOnClickListener {
            if (check()) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_main_dest_to_runingRecord_dest)
            }
        }
        binding!!.captureAsrBt.setOnClickListener {
            if (check()) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_main_dest_to_runingCapture_dest)
            }
        }
        initShareLog()
    }

    private fun check(): Boolean {
        if (yuyinViewModel.recorder == null) {
            Log.e(TAG, "recorder is null")
            return false
        }
        if (yuyinViewModel.recorder?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "recorder is not init")
            return false
        }
        if (!(requireActivity() as MainActivityView).checkFloatView()) {
            Log.e(TAG, "checkFloatView false")
            return false
        }
        return true
    }

    fun checkSettingFile(): Boolean {
        var result = true
        if (!YuyinViewModel.settingProfilePath.exists()) {
            Log.e(TAG, "settingProfilePath is not exit")
            result = false
        }
        if (!YuyinViewModel.yuYinDirPath.exists()) {
            Log.e(TAG, "yuYinDirPath is not exit")
            result = false
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}