package com.woocommerce.android.util

import android.text.format.DateFormat
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

/**
 * Stubs [DateFormat.getBestDateTimePattern] with the patterns CLDR returns for a month-first locale, for tests
 * that format dates only as a side effect of what they actually cover.
 */
class LocalizedDatePatternsTestRule : TestWatcher() {
    private var dateFormat: MockedStatic<DateFormat>? = null

    override fun starting(description: Description?) {
        dateFormat = Mockito.mockStatic(DateFormat::class.java)
        whenever(DateFormat.getBestDateTimePattern(any(), eq("MMMd"))).thenReturn("MMM d")
        whenever(DateFormat.getBestDateTimePattern(any(), eq("yMMMd"))).thenReturn("MMM d, y")
        whenever(DateFormat.getBestDateTimePattern(any(), eq("EEEEMMMd"))).thenReturn("EEEE, MMM d")
    }

    override fun finished(description: Description?) {
        dateFormat?.close()
        dateFormat = null
    }
}
