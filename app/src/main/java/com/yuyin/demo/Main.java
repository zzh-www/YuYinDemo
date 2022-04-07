package com.yuyin.demo;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.yuyin.demo.databinding.FragmentMainBinding;


public class Main extends Fragment {

    private FragmentMainBinding binding;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        binding.recordAsrBt.setOnClickListener(v -> {
            if (YuYinUtil.checkRequestPermissions(requireActivity(), requireContext())) {
                Navigation.findNavController(
                        requireActivity(),
                        R.id.yuyin_nav_host_container_fragment)
                        .navigate(R.id.action_main_dest_to_runingRecord_dest);
            }
        });


        binding.captureAsrBt.setOnClickListener(v -> {
            if (YuYinUtil.checkRequestPermissions(requireActivity(), requireContext())) {
                Navigation.findNavController(
                        requireActivity(),
                        R.id.yuyin_nav_host_container_fragment)
                        .navigate(R.id.action_main_dest_to_runingCapture_dest);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}