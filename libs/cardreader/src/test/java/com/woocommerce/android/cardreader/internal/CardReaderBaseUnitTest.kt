package com.woocommerce.android.cardreader.internal

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
abstract class CardReaderBaseUnitTest(testDispatcher: TestDispatcher = UnconfinedTestDispatcher()) {
    @Rule @JvmField
    val coroutinesTestRule = CardReaderCoroutineTestRule(testDispatcher)

    protected fun testBlocking(block: suspend TestScope.() -> Unit) =
        runTest(coroutinesTestRule.testDispatcher) {
            block()
        }
}
