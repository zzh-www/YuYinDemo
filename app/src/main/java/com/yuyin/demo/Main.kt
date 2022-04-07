package com.yuyin.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import com.yuyin.demo.YuYinUtil.checkRequestPermissions
import com.yuyin.demo.databinding.FragmentMainBinding

class Main : Fragment() {
    private var binding: FragmentMainBinding? = null

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
                )
                    .navigate(R.id.action_main_dest_to_runingRecord_dest)
            }
        }
        binding!!.captureAsrBt.setOnClickListener { v: View? ->
            if (checkRequestPermissions(requireActivity(), requireContext())) {
                findNavController(
                    requireActivity(),
                    R.id.yuyin_nav_host_container_fragment
                )
                    .navigate(R.id.action_main_dest_to_runingCapture_dest)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}