package com.woocommerce.android.util

import com.woocommerce.android.util.RollingLogEntries.LogEntry
import com.woocommerce.android.util.WooLog.LogLevel.d
import com.woocommerce.android.util.WooLog.T.UTILS
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.InvalidParameterException
import kotlin.test.assertEquals

class RollingLogEntriesTest {
    @Test
    fun `Given there are fewer entries than the limit, no entries are removed`() {
        val maxEntries = 20
        val entries = 15
        val log = RollingLogEntries(maxEntries)
        for (i in 0 until entries) {
            log.add(LogEntry(UTILS, d, "$i"))
        }

        assertEquals(log.size, entries)
        for (i in 0 until entries) {
            assertEquals(log[i].text, "$i")
        }
    }

    @Test
    fun `Given there are more entries than the limit, the oldest entries are removed, keeping the size to the max`() {
        val maxEntries = 10
        val entries = 15
        val logs = RollingLogEntries(maxEntries)

        for (i in 0 until entries) {
            logs.add(LogEntry(UTILS, d, "$i"))
        }

        assertEquals(logs.size, maxEntries)
        logs.forEachIndexed { i, log ->
            val expectedValue = i + entries - maxEntries
            assertEquals(log.text, "$expectedValue")
        }
    }

    @Test
    fun `When the RollingLogEntries is initialized with non-positive number as the limit, an exception is thrown`() {
        assertThrows(InvalidParameterException::class.java) {
            RollingLogEntries(0)
        }

        assertThrows(InvalidParameterException::class.java) {
            RollingLogEntries(-1)
        }
    }
}
