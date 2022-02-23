package com.yuyin.demo;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.yuyin.demo.databinding.FragmentRuningCaptureBinding;


public class RuningCapture extends Fragment {

    private FragmentRuningCaptureBinding binding;




    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        binding = FragmentRuningCaptureBinding.inflate(inflater, container,false);
        View view = binding.getRoot();
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.stopBtRunCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO stopASR
            }
        });

        binding.saveBtRunCap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO saveResult
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}