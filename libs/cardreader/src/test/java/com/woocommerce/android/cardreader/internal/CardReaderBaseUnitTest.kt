package com.woocommerce.android.cardreader.internal

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
abstract class CardReaderBaseUnitTest(testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    @ExperimentalCoroutinesApi
    @Rule @JvmField
    val coroutinesTestRule = CardReaderCoroutineTestRule(testDispatcher)

    @ExperimentalCoroutinesApi
    protected fun testBlocking(block: suspend TestScope.() -> Unit) =
        runTest(coroutinesTestRule.testDispatcher) {
            block()
        }
}
