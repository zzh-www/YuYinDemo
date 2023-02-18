package com.yuyin.demo.view;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textview.MaterialTextView;

public class ScrollTextView extends MaterialTextView {

    public ScrollTextView(@NonNull Context context) {
        super(context);
    }

    public ScrollTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, android.R.attr.textViewStyle);
    }

    public ScrollTextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ScrollTextView(
            @NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean isFocused() {
        return true;
    }
}
