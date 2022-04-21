package com.woocommerce.android.util

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

fun TestScope.advanceTimeAndRun(duration: Long) {
    advanceTimeBy(duration)
    runCurrent()
}
