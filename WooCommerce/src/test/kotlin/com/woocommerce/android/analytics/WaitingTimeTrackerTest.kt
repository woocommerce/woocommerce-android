package com.woocommerce.android.analytics

import com.woocommerce.android.analytics.WaitingTimeTracker.State.Idle
import com.woocommerce.android.analytics.WaitingTimeTracker.State.Waiting
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WaitingTimeTrackerTest : BaseUnitTest() {
    lateinit var sut: WaitingTimeTracker

    @Before
    fun setUp() {
        createSut(
            customTimeInMillis = { 100 },
            customWaitingTimeout = 10L
        )
    }

    @Test
    fun `When starting and ending the waiting process, then handle all states correctly`() =
        testBlocking {
            assertTrue(sut.currentState is Idle)
            sut.onWaitingStarted(mock())
            assertTrue(sut.currentState is Waiting)
            sut.onWaitingEnded()
            assertTrue(sut.currentState is Idle)
        }

    @Test
    fun `When only starting the waiting process, then handle return to idle after the timeout`() =
        testBlocking {
            assertTrue(sut.currentState is Idle)
            sut.onWaitingStarted(mock())
            assertTrue(sut.currentState is Waiting)
            delay(100)
            assertTrue(sut.currentState is Idle)
        }

    @Test
    fun `When ending the waiting without starting it, then do nothing`() = testBlocking {
        assertTrue(sut.currentState is Idle)
        sut.onWaitingEnded()
        assertTrue(sut.currentState is Idle)
    }

    private fun createSut(
        customTimeInMillis: () -> Long,
        customWaitingTimeout: Long
    ) {
        sut = WaitingTimeTracker(
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers,
            currentTimeInMillis = customTimeInMillis,
            waitingTimeout = customWaitingTimeout
        )
    }
}
