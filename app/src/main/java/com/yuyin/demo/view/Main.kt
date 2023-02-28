package com.yuyin.demo.view

import android.content.Intent
import android.media.AudioRecord
import android.os.Bundle
import android.view.*
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation.findNavController
import com.yuyin.demo.R
import com.yuyin.demo.YuYinUtil.checkRequestPermissions
import com.yuyin.demo.YuYinUtil.YuYinLog as Log
import com.yuyin.demo.databinding.FragmentMainBinding
import com.yuyin.demo.viewmodel.YuyinViewModel

class Main : Fragment() {

    companion object {
        const val TAG = "Main"
    }

    private var binding: FragmentMainBinding? = null

    private val yuyinViewModel: YuyinViewModel by activityViewModels()

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
                        val intent = Intent(requireActivity(), SettingsActivity::class.java)
                        startActivity(intent)
                    }
                }
                return false
            }
        })
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.recordAsrBt.setOnClickListener { v: View? ->
            if (check()) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_main_dest_to_runingRecord_dest)
            }
        }
        binding!!.captureAsrBt.setOnClickListener { v: View? ->
            if (check()) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_main_dest_to_runingCapture_dest)
            }
        }
    }

    private fun check(): Boolean {
        if (!checkRequestPermissions(requireActivity(), requireContext())) {
            Log.e(TAG,"no permission for asr")
            return false
        }
        if (yuyinViewModel.recorder == null) {
            Log.e(TAG,"recorder is null")
            return false
        }
        if (yuyinViewModel.recorder?.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG,"recorder is not init")
            return false
        }
        if (!(requireActivity() as MainActivityView).checkFloatView()) {
            Log.e(TAG,"checkFloatView false")
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}