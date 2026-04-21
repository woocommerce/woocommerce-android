package com.woocommerce.android.util.logs

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogEntryTest : BaseUnitTest() {
    @Test
    fun `when parsing a valid serialized entry, then the entry is correctly reconstructed`() {
        // GIVEN
        val original = LogEntry(WooLog.T.ORDERS, WooLog.LogLevel.d, "Test message")

        // WHEN
        val parsed = LogEntry.fromString(original.toString())

        // THEN
        assertThat(parsed).isEqualTo(original)
    }

    @Test
    fun `when parsing an entry with multiline text, then the entry is correctly reconstructed`() {
        // GIVEN
        val multilineText = "StackTrace: java.lang.Exception\n    at com.foo.Bar.run(Bar.kt:10)"
        val original = LogEntry(WooLog.T.UTILS, WooLog.LogLevel.e, multilineText)

        // WHEN
        val parsed = LogEntry.fromString(original.toString())

        // THEN
        val entry = requireNotNull(parsed)
        assertThat(entry.tag).isEqualTo(WooLog.T.UTILS)
        assertThat(entry.text).isEqualTo(multilineText)
    }

    @Test
    fun `when parsing corrupted input, then null is returned`() {
        assertThat(LogEntry.fromString("not a log entry at all")).isNull()
    }

    @Test
    fun `when parsing an entry with deprecated NOTIFS tag, then it is migrated to NOTIFICATIONS`() {
        // GIVEN
        val input = "[Mar-02 14:00:00:000 NOTIFS d] Some notification"

        // WHEN
        val parsed = LogEntry.fromString(input)

        // THEN
        val entry = requireNotNull(parsed)
        assertThat(entry.tag).isEqualTo(WooLog.T.NOTIFICATIONS)
    }
}
