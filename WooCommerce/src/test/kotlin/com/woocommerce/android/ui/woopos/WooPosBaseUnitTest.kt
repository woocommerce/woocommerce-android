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

    init {

        /**
         * This is copied from BaseUnitTest to fix existing tests that were broken by
         * the change on kotlinx.coroutines 1.7.0 that causes tests that
         * throw exceptions to fail. Previously test methods that threw exceptions would not prevent
         * tests from passing, which was a bug in kotlinx.coroutines that has now been fixed. However,
         * significant number of our tests are currently failing because of this change.
         *
         * See the following issue for more details: https://github.com/Kotlin/kotlinx.coroutines/issues/1205.
         * The workaround below is taken from the related PR: https://github.com/Kotlin/kotlinx.coroutines/pull/3736
         * and is a solution suggested by JetBrains to disable the new behavior using non-public API
         * until we fix our tests. This should not be considered a long-term solution, rather a temporary hack.
         */

        Class.forName("kotlinx.coroutines.test.TestScopeKt")
            .getDeclaredMethod("setCatchNonTestRelatedExceptions", Boolean::class.java)
            .invoke(null, false)
    }

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
