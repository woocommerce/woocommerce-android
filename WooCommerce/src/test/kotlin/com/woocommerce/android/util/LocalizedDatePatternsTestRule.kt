package com.woocommerce.android.util

import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.text.format.DateFormat
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stubs the ICU date APIs with the values CLDR returns for en-US, for tests that format dates only as a side
 * effect of what they actually cover. The interval stub joins both full dates instead of replicating ICU's elision.
 */
class LocalizedDatePatternsTestRule : TestWatcher() {
    private var dateFormat: MockedStatic<DateFormat>? = null
    private var intervalFormat: MockedStatic<DateIntervalFormat>? = null
    private var dateInterval: MockedConstruction<DateInterval>? = null

    override fun starting(description: Description?) {
        val lenient = Mockito.withSettings().strictness(Strictness.LENIENT)

        dateFormat = Mockito.mockStatic(DateFormat::class.java, lenient)
        whenever(DateFormat.getBestDateTimePattern(any(), eq("MMMd"))).thenReturn("MMM d")
        whenever(DateFormat.getBestDateTimePattern(any(), eq("yMMMd"))).thenReturn("MMM d, y")
        whenever(DateFormat.getBestDateTimePattern(any(), eq("EEEEMMMd"))).thenReturn("EEEE, MMM d")

        dateInterval = Mockito.mockConstruction(DateInterval::class.java, lenient) { mock, context ->
            whenever(mock.fromDate).thenReturn(context.arguments()[0] as Long)
            whenever(mock.toDate).thenReturn(context.arguments()[1] as Long)
        }

        val enUsIntervalFormat = Mockito.mock(DateIntervalFormat::class.java, lenient)
        whenever(enUsIntervalFormat.format(any<DateInterval>())).thenAnswer { invocation ->
            val interval = invocation.getArgument<DateInterval>(0)
            val format = SimpleDateFormat("MMM d, yyyy", Locale.US)
            "${format.format(Date(interval.fromDate))} – ${format.format(Date(interval.toDate))}"
        }
        intervalFormat = Mockito.mockStatic(DateIntervalFormat::class.java, lenient)
        whenever(DateIntervalFormat.getInstance(eq("yMMMd"), any<Locale>())).thenReturn(enUsIntervalFormat)
    }

    override fun finished(description: Description?) {
        dateInterval?.close()
        dateInterval = null
        intervalFormat?.close()
        intervalFormat = null
        dateFormat?.close()
        dateFormat = null
    }
}
