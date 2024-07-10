package com.woocommerce.android.ui.woopos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Rule
import org.junit.rules.RuleChain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
abstract class WooPosBaseUnitTest(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {

    @get:Rule
    val rule: RuleChain = RuleChain
        .outerRule(InstantTaskExecutorRule())
        .around(CoroutineTestRule(testDispatcher))

    protected fun testBlocking(block: suspend TestScope.() -> Unit) = runTest(testDispatcher) { block() }

    class CoroutineTestRule(
        private val testDispatcher: TestDispatcher
    ) : TestWatcher() {
        override fun starting(description: Description) {
            super.starting(description)
            kotlinx.coroutines.Dispatchers.setMain(testDispatcher)
        }

        override fun finished(description: Description) {
            super.finished(description)
            kotlinx.coroutines.Dispatchers.resetMain()
            testDispatcher.cancelChildren()
        }
    }
}
