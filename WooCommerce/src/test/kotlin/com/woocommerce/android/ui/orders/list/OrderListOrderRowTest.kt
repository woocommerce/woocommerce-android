package com.woocommerce.android.ui.orders.list

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OrderListOrderRowTest {
    @Test
    fun `given a fast leftward flick, when released at halfway, then the order is not completed`() {
        // WHEN
        val shouldComplete = shouldCompleteOrderSwipe(
            offsetPx = -500f,
            widthPx = 1_000,
            velocityPxPerSecond = -125f,
            cancelVelocityThresholdPxPerSecond = 125f,
        )

        // THEN
        assertThat(shouldComplete).isFalse()
    }

    @Test
    fun `given a fast leftward swipe, when released past halfway, then the order is completed`() {
        // WHEN
        val shouldComplete = shouldCompleteOrderSwipe(
            offsetPx = -501f,
            widthPx = 1_000,
            velocityPxPerSecond = -125f,
            cancelVelocityThresholdPxPerSecond = 125f,
        )

        // THEN
        assertThat(shouldComplete).isTrue()
    }

    @Test
    fun `given a swipe past halfway, when released moving right at the cancel speed, then the order is not completed`() {
        // WHEN
        val shouldComplete = shouldCompleteOrderSwipe(
            offsetPx = -501f,
            widthPx = 1_000,
            velocityPxPerSecond = 125f,
            cancelVelocityThresholdPxPerSecond = 125f,
        )

        // THEN
        assertThat(shouldComplete).isFalse()
    }
}
