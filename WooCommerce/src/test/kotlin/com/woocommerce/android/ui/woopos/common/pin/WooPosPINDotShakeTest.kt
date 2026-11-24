package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.runtime.BroadcastFrameClock
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Offset.offset
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosPINDotShakeTest {
    @get:Rule
    val coroutineTestRule = WooPosCoroutineTestRule()

    @Test
    fun `when the dots shake, then they move in both directions and return to center`() = runTest {
        // GIVEN
        val shake = WooPosPINDotShake()
        val frameClock = BroadcastFrameClock()

        // WHEN
        val animation = launch(frameClock) { shake.play() }
        runCurrent()
        frameClock.sendFrame(0L)
        runCurrent()
        frameClock.sendFrame(66_666_667L)
        runCurrent()
        val firstPeak = shake.offset.value
        frameClock.sendFrame(200_000_000L)
        runCurrent()
        val secondPeak = shake.offset.value
        frameClock.sendFrame(400_000_000L)
        runCurrent()

        // THEN
        assertThat(firstPeak).isCloseTo(8f, offset(0.01f))
        assertThat(secondPeak).isCloseTo(-8f, offset(0.01f))
        assertThat(shake.offset.value).isCloseTo(0f, offset(0.01f))
        assertThat(animation.isCompleted).isTrue()
    }
}
