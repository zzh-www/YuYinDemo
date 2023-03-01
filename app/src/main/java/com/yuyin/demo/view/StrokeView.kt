package com.yuyin.demo.view

import android.R
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView

/*
实现字体描边效果，凸显字幕文字，本质是重新绘制一遍字体，一遍画边框，一遍画
 */
class StrokeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = R.attr.textViewStyle
) : MaterialTextView(context, attrs, defStyleAttr) {
    private val strokeWidth = 0.04f
    private val originStrokeWidth = paint.strokeWidth
    private val originStyle = paint.style
    private val originStrokeJoin = paint.strokeJoin
    private val originColor = this.currentTextColor

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas) // 画字体本体

        // 画边框
        paint.strokeWidth = paint.textSize *strokeWidth //设置描边宽度
        paint.style = Paint.Style.STROKE //设置画笔样式为描边
        paint.strokeJoin = Paint.Join.BEVEL //连接方式为圆角
        this.setTextColor(Color.BLACK)
        super.onDraw(canvas)

        // 恢复
        paint.strokeWidth = originStrokeWidth
        paint.style = originStyle
        paint.strokeJoin = originStrokeJoin
        this.setTextColor(originColor)
    }

    override fun setShadowLayer(radius: Float, dx: Float, dy: Float, color: Int) {
        // 换阴影为边框

    }

}