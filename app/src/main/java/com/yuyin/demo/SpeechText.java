package com.yuyin.demo;

public class SpeechText {
    private String speech_Text="";
    public SpeechText(String text) {
        this.speech_Text = text;
    }

    public String getText() {
        return this.speech_Text;
    }

    public void setText(String text) {
        this.speech_Text = text;
    }
}

