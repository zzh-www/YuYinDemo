package com.mobvoi.wenet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import com.mobvoi.wenet.Recognize.getByteResult
import com.mobvoi.wenet.Recognize.javaStringToJniArray
import com.mobvoi.wenet.Recognize.jniArrayToJavaString
import com.yuyin.demo.MainActivityView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
@SmallTest
class ExampleInstrumentedTest {

    @get:Rule
    val activityRule = ActivityTestRule(MainActivityView::class.java,false)
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val javaString = "是爱烧烤"
        val jniArray = javaStringToJniArray(javaString)
        assertEquals(javaString, jniArrayToJavaString(jniArray))
    }
}