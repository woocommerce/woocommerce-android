package com.woocommerce.android.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

@ExperimentalCoroutinesApi
fun TestScope.advanceTimeAndRun(duration: Long) {
    advanceTimeBy(duration)
    runCurrent()
}
