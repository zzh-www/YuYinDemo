package com.mobvoi.wenet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.anim.DefaultAnimator
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.utils.DisplayUtils


class TestFloat : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test_float)
        EasyFloat.with(this)
            .setLayout(R.layout.default_add_layout)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.RESULT_HORIZONTAL)
            .setTag("Test")
            .setDragEnable(true)
            .hasEditText(false)
            .setLocation(100,200)
            .setGravity(Gravity.END or Gravity.CENTER_VERTICAL, 0, 200)
            .setLayoutChangedGravity(Gravity.END)
            .setBorder()
            .setMatchParent(false, false)
            .setAnimator(DefaultAnimator())
            .setFilter(MainActivity::class.java)
            .setDisplayHeight{context -> DisplayUtils.rejectedNavHeight(context)}
            .registerCallback {
                createResult { isCreated, msg, view -> }
                show {  }
                hide {  }
                dismiss {  }
                touchEvent {view, motionEvent -> }
                drag { view, motionEvent ->  }
                dragEnd {  }

            }
            .show()
    }
}