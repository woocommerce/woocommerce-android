package com.woocommerce.android.util

import com.woocommerce.android.util.RollingLogEntries.LogEntry
import com.woocommerce.android.util.WooLog.LogLevel.d
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.UTILS
import org.junit.Assert.assertThrows
import org.junit.Test
import java.security.InvalidParameterException
import kotlin.test.assertEquals

class RollingLogEntriesTest {
    @Test
    fun `Verify all entries are present if limit not reached`() {
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
    fun `Verify oldest entries are discarded if limit reached`() {
        val maxEntries = 10
        val entries = 15
        val logs = RollingLogEntries(maxEntries)

        for (i in 0 until entries) {
            logs.add(LogEntry(T.UTILS, d, "$i"))
        }

        assertEquals(logs.size, maxEntries)
        logs.forEachIndexed { i, log ->
            val expectedValue = i + entries - maxEntries
            assertEquals(log.text, "$expectedValue")
        }
    }

    @Test
    fun `Verify an exception is thrown if the limit is less than or equal to 0`() {
        assertThrows(InvalidParameterException::class.java) {
            RollingLogEntries(0)
        }

        assertThrows(InvalidParameterException::class.java) {
            RollingLogEntries(-1)
        }
    }
}
