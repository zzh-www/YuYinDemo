package test.kt

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import com.lzf.easyfloat.R as easyR
import com.lzf.easyfloat.interfaces.OnTouchRangeListener
import com.lzf.easyfloat.utils.DisplayUtils
import com.lzf.easyfloat.widget.BaseSwitchView
import com.yuyin.demo.R

class TestView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : BaseSwitchView(context, attrs, defStyleAttr) {

    private var normalColor = Color.parseColor("#99000000")
    private var inRangeColor = Color.parseColor("#99FF0000")
    private var shapeType = 2

    private lateinit var paint: Paint
    private var width = 0f
    private var height = 0f
    private var region = Region()
    private var inRange = false
    private var zoomSize = DisplayUtils.dp2px(context, 4f).toFloat()
    private var listener: OnTouchRangeListener? = null
    private var myIcon: Bitmap? = ResourcesCompat.getDrawable(context.resources,R.drawable.ic_main,null)?.toBitmap()

    init {
        attrs?.apply { initAttrs(this) }
        initPaint()
        setWillNotDraw(false)
    }

    private fun initAttrs(attrs: AttributeSet) =
        context.theme.obtainStyledAttributes(attrs, easyR.styleable.DefaultCloseView, 0, 0).apply {
            normalColor = getColor(easyR.styleable.DefaultCloseView_normalColor, normalColor)
            inRangeColor = getColor(easyR.styleable.DefaultCloseView_inRangeColor, inRangeColor)
            shapeType = getInt(easyR.styleable.DefaultCloseView_shapeType, shapeType)
            zoomSize = getDimension(easyR.styleable.DefaultCloseView_zoomSize, zoomSize)
        }.recycle()


    private fun initPaint() {
        paint = Paint().apply {
            color = normalColor
            strokeWidth = 10f
            style = Paint.Style.FILL
            isAntiAlias = true
        }


    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        width = w.toFloat()
        height = h.toFloat()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
//        canvas?.apply {
//            // w: 36dp
//            val radius = height/2f
//            drawCircle(radius,radius,radius,paint)
//
//            // 24dp
//            myIcon?.let {
//                drawBitmap(it,radius-it.width/2f,radius-it.height/2f,null)
//            }
//        }
    }

    override fun setTouchRangeListener(event: MotionEvent, listener: OnTouchRangeListener?) {
        this.listener = listener
        initTouchRange(event)
    }

    private fun initTouchRange(event: MotionEvent): Boolean {
        val location = IntArray(2)
        // 获取在整个屏幕内的绝对坐标
        getLocationOnScreen(location)
        val currentInRange = region.contains(
            event.rawX.toInt() - location[0], event.rawY.toInt() - location[1]
        )
        if (currentInRange != inRange) {
            inRange = currentInRange
            invalidate()
        }
        listener?.touchInRange(currentInRange, this)
        if (event.action == MotionEvent.ACTION_UP && currentInRange) {
            listener?.touchUpInRange()
        }
        return currentInRange
    }
}
