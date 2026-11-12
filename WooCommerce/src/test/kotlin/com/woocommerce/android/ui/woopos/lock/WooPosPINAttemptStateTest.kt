package com.woocommerce.android.ui.woopos.lock

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test

class WooPosPINAttemptStateTest {
    @Test
    fun `when wrong entries reach each threshold, then waits escalate and later groups remain hourly`() {
        // GIVEN
        var state = WooPosPINAttemptState()
        var time = WooPosPINTime(1, 1_000)
        val waits = mapOf(5 to 30_000L, 10 to 300_000L, 15 to 1_800_000L, 20 to 3_600_000L, 25 to 3_600_000L)

        (1..25).forEach { entry ->
            // WHEN
            state = state.at(time).failed(time)

            // THEN
            val expectedWait = waits[entry] ?: 0L
            assertThat(state.failures).isEqualTo(entry.toLong())
            assertThat(state.remainingMillis(time)).isEqualTo(expectedWait)
            time = time.copy(elapsedMillis = time.elapsedMillis + expectedWait + 1)
        }
    }

    @Test
    fun `given an active wait, when monotonic time advances, then only the remaining duration is required`() {
        // GIVEN
        val state = WooPosPINAttemptState(5, 1, 1_000, 30_000)

        // THEN
        listOf(1_000L to 30_000L, 11_000L to 20_000L, 30_999L to 1L, 31_000L to 0L, 31_001L to 0L)
            .forEach { (elapsed, remaining) ->
                val time = WooPosPINTime(1, elapsed)
                assertThat(state.at(time).remainingMillis(time)).isEqualTo(remaining)
                assertThat(state.at(time).failures).isEqualTo(5)
            }
    }

    @Test
    fun `given a quiet counter, when exactly 24 hours pass, then the next wrong entry starts a new budget`() {
        // GIVEN
        val state = WooPosPINAttemptState(14, 1, 1_000)
        val before = WooPosPINTime(1, 1_000 + DAY_MILLIS - 1)
        val boundary = before.copy(elapsedMillis = before.elapsedMillis + 1)

        // THEN
        assertThat(state.at(before).failures).isEqualTo(14)
        assertThat(state.at(boundary)).isEqualTo(WooPosPINAttemptState())
        assertThat(state.at(boundary).failed(boundary).failures).isEqualTo(1)
        assertThat(state.at(boundary).failed(boundary).remainingMillis(boundary)).isZero()
    }

    @Test
    fun `given a wrong entry before the quiet boundary, when another hour passes, then the counter is retained`() {
        // GIVEN
        val state = WooPosPINAttemptState(13, 1, 1_000)
        val entryTime = WooPosPINTime(1, 1_000 + DAY_MILLIS - HOUR_MILLIS)

        // WHEN
        val updated = state.at(entryTime).failed(entryTime)
        val originalBoundary = entryTime.copy(elapsedMillis = 1_000 + DAY_MILLIS)
        val newBoundary = entryTime.copy(elapsedMillis = entryTime.elapsedMillis + DAY_MILLIS)

        // THEN
        assertThat(updated.at(originalBoundary).failures).isEqualTo(14)
        assertThat(updated.at(newBoundary.copy(elapsedMillis = newBoundary.elapsedMillis - 1)).failures)
            .isEqualTo(14)
        assertThat(updated.at(newBoundary).failures).isZero()
    }

    @Test
    fun `given a different boot, when reading a saved penalty, then a full wait is conservatively restarted`() {
        // GIVEN
        val state = WooPosPINAttemptState(10, 1, 50_000, 300_000)
        val time = WooPosPINTime(2, DAY_MILLIS * 2)

        // WHEN
        val reanchored = state.at(time)

        // THEN
        assertThat(reanchored.failures).isEqualTo(10)
        assertThat(reanchored.bootCount).isEqualTo(2)
        assertThat(reanchored.lastFailureMillis).isEqualTo(time.elapsedMillis)
        assertThat(reanchored.remainingMillis(time)).isEqualTo(300_000)
        val later = time.copy(elapsedMillis = time.elapsedMillis + 60_000)
        assertThat(reanchored.at(later).remainingMillis(later)).isEqualTo(240_000)
        assertThat(reanchored.at(time.copy(elapsedMillis = time.elapsedMillis + DAY_MILLIS)).failures).isZero()
    }

    @Test
    fun `given decreased uptime in the same boot, when reading a saved penalty, then it cannot shorten the wait`() {
        // GIVEN
        val state = WooPosPINAttemptState(5, 1, 50_000, 30_000)
        val time = WooPosPINTime(1, 1_000)

        // WHEN
        val reanchored = state.at(time)

        // THEN
        assertThat(reanchored.failures).isEqualTo(5)
        assertThat(reanchored.lastFailureMillis).isEqualTo(1_000)
        assertThat(reanchored.remainingMillis(time)).isEqualTo(30_000)
        val later = time.copy(elapsedMillis = 31_000)
        assertThat(reanchored.at(later).remainingMillis(later)).isZero()
        assertThat(reanchored.at(later).failed(later).failures).isEqualTo(6)
    }

    @Test
    fun `given a counter above the integer range, when a wrong entry is recorded, then counting remains positive`() {
        // GIVEN
        val time = WooPosPINTime(1, 1_000)
        val state = WooPosPINAttemptState(Int.MAX_VALUE.toLong(), 1, 1_000)

        // WHEN
        val updated = state.at(time).failed(time)

        // THEN
        assertThat(updated.failures).isEqualTo(Int.MAX_VALUE.toLong() + 1)
    }

    @Test
    fun `given an exhausted long counter, when a wrong entry is recorded, then an hourly penalty survives overflow`() {
        // GIVEN
        val time = WooPosPINTime(1, 1_000)
        val state = WooPosPINAttemptState(Long.MAX_VALUE, 1, 1_000)

        // WHEN
        val updated = state.at(time).failed(time)

        // THEN
        assertThat(updated.failures).isPositive()
        assertThat(updated.remainingMillis(time)).isEqualTo(HOUR_MILLIS)
    }

    @Test
    fun `given invalid persisted values, when reconstructing a record, then no usable attempt state is returned`() {
        // THEN
        listOf<() -> Any>(
            { WooPosPINAttemptState(failures = -1) },
            { WooPosPINAttemptState(bootCount = -1) },
            { WooPosPINAttemptState(lastFailureMillis = -1) },
            { WooPosPINAttemptState(waitMillis = -1) },
            { WooPosPINAttemptState(waitMillis = HOUR_MILLIS + 1) },
            { WooPosPINTime(-1, 0) },
            { WooPosPINTime(0, -1) },
        ).forEach { create ->
            assertThatThrownBy { create() }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    companion object {
        private const val HOUR_MILLIS = 3_600_000L
        private const val DAY_MILLIS = 86_400_000L
    }
}
