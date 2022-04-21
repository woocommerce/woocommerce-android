package com.woocommerce.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.woocommerce.android.util.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@Suppress("UnnecessaryAbstractClass")
@RunWith(MockitoJUnitRunner::class)
abstract class BaseUnitTest(testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    @Rule @JvmField
    val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @Rule @JvmField
    val coroutinesTestRule = CoroutineTestRule(testDispatcher)

    @ExperimentalCoroutinesApi
    protected fun testBlocking(block: suspend TestScope.() -> Unit) =
        runTest(coroutinesTestRule.testDispatcher) {
            block()
        }
}
