package com.woocommerce.android.analytics

import com.woocommerce.android.analytics.WaitingTimeTracker.State.Idle
import com.woocommerce.android.analytics.WaitingTimeTracker.State.Waiting
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WaitingTimeTrackerTest: BaseUnitTest() {
    lateinit var sut: WaitingTimeTracker

    @Before
    fun setUp() {
        sut = WaitingTimeTracker(
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher),
            dispatchers = coroutinesTestRule.testDispatchers,
            currentTimeInMillis = { 100 },
            waitingTimeout = 10L
        )
    }

    @Test
    fun `When starting and ending the waiting process, then handle all states correctly`() = runBlocking {
        assertTrue(sut.currentState is Idle)
        sut.onWaitingStarted()
        assertTrue(sut.currentState is Waiting)
        sut.onWaitingEnded()
        assertTrue(sut.currentState is Idle)
    }

    @Test
    fun `When ending the waiting without starting it, then do nothing`() = runBlocking {
        assertTrue(sut.currentState is Idle)
        sut.onWaitingEnded()
        assertTrue(sut.currentState is Idle)
    }
}
