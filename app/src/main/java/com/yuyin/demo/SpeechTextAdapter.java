package com.yuyin.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import java.util.List;

public class SpeechTextAdapter extends RecyclerView.Adapter<SpeechTextAdapter.ViewHolder> {
    private List<SpeechText> dataList;

    public SpeechTextAdapter(List<SpeechText> data) {
        dataList = data;
    }

    @NonNull
    @Override
    public SpeechTextAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_item, parent, false);
        return new SpeechTextAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SpeechTextAdapter.ViewHolder holder, int position) {
        SpeechText speechText = dataList.get(position);
        holder.getSpeech_view().setText(speechText.getText());
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView speech_view;
        public ViewHolder(View view) {
            super(view);
            this.speech_view = (TextView) view.findViewById(R.id.speechText);
        }
        public TextView getSpeech_view() {
            return speech_view;
        }
    }



}
