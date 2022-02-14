package com.mobvoi.wenet;

/**
 * @ProjectName: com.demo.wenet
 * @Package: com.demo.wenet
 * @ClassName: SpeechText
 * @Description: 描述
 * @Author: ZZH
 * @CreateDate: 2021/11/17 11 12:54
 * @UpdateUser: 86180：
 * @UpdateDate: 2021/11/17 11 12:54
 * @UpdateRemark: 更新说明：
 * @Version: 1.0
 */
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
