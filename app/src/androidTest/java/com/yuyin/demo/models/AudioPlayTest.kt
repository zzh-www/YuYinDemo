package com.yuyin.demo.models

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.yuyin.demo.view.MainActivityView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class AudioPlayTest {
    @get:Rule
    val activityRule= ActivityScenarioRule(MainActivityView::class.java)
    @Test
    fun testAudioFormat() {
        println(AudioPlay.isPlay)
    }
}