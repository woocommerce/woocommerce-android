package com.woocommerce.android.analytics

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            waitingTimeout = 100000L
        )
    }

    @Test
    fun `WaitingTimeTracker should handle states correctly`() = runBlocking {
        assertTrue(sut.state.value is WaitingTimeTracker.State.Idle)
        sut.onWaitingStarted()
        assertTrue(sut.state.value is WaitingTimeTracker.State.Waiting)
        sut.onWaitingEnded()
        assertTrue(sut.state.value is WaitingTimeTracker.State.Idle)
    }
}
