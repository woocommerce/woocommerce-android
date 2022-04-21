package com.woocommerce.android.helpers

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

fun TestScope.advanceTimeAndRun(duration: Long) {
    advanceTimeBy(duration)
    runCurrent()
}
