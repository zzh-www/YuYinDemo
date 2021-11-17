package com.mobvoi.wenet;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * @ProjectName: wenet
 * @Package: com.mobvoi.wenet
 * @ClassName: SpeechTextAdapter
 * @Description: 描述
 * @Author: ZZH
 * @CreateDate: 2021/11/17 11 12:52
 * @UpdateUser: 86180：
 * @UpdateDate: 2021/11/17 11 12:52
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
public class SpeechTextAdapter extends RecyclerView.Adapter<SpeechTextAdapter.ViewHolder> {
    private List<SpeechText> dataList;

    public SpeechTextAdapter(List<SpeechText> data) {
        dataList = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.text_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
