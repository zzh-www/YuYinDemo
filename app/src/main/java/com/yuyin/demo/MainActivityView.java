package com.yuyin.demo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.yuyin.demo.databinding.ActivityMainViewBinding;

public class MainActivityView extends AppCompatActivity {

    private ActivityMainViewBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainViewBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
    }
}