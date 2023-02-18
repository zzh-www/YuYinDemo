package com.yuyin.demo

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.Navigation.findNavController
import com.yuyin.demo.YuYinUtil.checkRequestPermissions
import com.yuyin.demo.databinding.FragmentMainBinding
import com.yuyin.demo.models.YuyinViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.concurrent.thread

class Main : Fragment() {
    private var binding: FragmentMainBinding? = null

    private val yuyinViewModel: YuyinViewModel by activityViewModels()

    private val texttest = "哈喽一二三androidx.navigation.Navigation.findNavControllerandroidx.navigati"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding =
            FragmentMainBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding!!.recordAsrBt.setOnClickListener { v: View? ->
            if (checkRequestPermissions(requireActivity(), requireContext())) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_main_dest_to_runingRecord_dest)
            }
        }
        binding!!.captureAsrBt.setOnClickListener { v: View? ->
            if (checkRequestPermissions(requireActivity(), requireContext()) && yuyinViewModel.recorder != null) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                ).navigate(R.id.action_main_dest_to_runingCapture_dest)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}