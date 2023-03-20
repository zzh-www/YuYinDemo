package com.mobvoi.wenet

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.mobvoi.wenet.Recognize.javaStringToJniArray
import com.mobvoi.wenet.Recognize.jniArrayToJavaString
import com.yuyin.demo.view.MainActivityView
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
class JNIInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivityView::class.java)
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val javaString = "是爱烧烤"
        val jniArray = javaStringToJniArray(javaString)
        assertEquals(javaString, jniArrayToJavaString(jniArray))
    }
}