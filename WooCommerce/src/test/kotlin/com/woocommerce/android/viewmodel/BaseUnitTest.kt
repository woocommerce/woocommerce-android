package com.woocommerce.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.woocommerce.android.util.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@Suppress("UnnecessaryAbstractClass")
@RunWith(MockitoJUnitRunner::class)
abstract class BaseUnitTest {
    @Rule @JvmField
    val rule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @Rule @JvmField
    val coroutinesTestRule = CoroutineTestRule()

    @ExperimentalCoroutinesApi
    protected fun testBlocking(block: suspend TestCoroutineScope.() -> Unit) =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            block()
        }
}
