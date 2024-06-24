package com.woocommerce.android

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@ExperimentalCoroutinesApi
@Suppress("UnnecessaryAbstractClass")
@RunWith(MockitoJUnitRunner::class)
abstract class BaseUnitTest(testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    init {

        /**
         * This is a temporary workaround  to fix existing tests that were broken by
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

    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Rule
    @JvmField
    val coroutinesTestRule = CoroutineTestRule(testDispatcher)

    protected fun testBlocking(block: suspend TestScope.() -> Unit) =
        runTest(coroutinesTestRule.testDispatcher) {
            block()
        }
}
