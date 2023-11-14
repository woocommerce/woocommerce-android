package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class UpdateErrorMapperTest {
    private val batteryLevelProvider: () -> Float? = mock()
    private val mapper = UpdateErrorMapper(batteryLevelProvider)

    @Test
    fun `given bluetooth error, when mapping, then failed returns`() {
        // GIVEN
        val error = TerminalException.TerminalErrorCode.BLUETOOTH_ERROR

        // WHEN
        val result = mapper.map(error)

        // THEN
        assertEquals(SoftwareUpdateStatusErrorType.Failed, result)
    }

    @Test
    fun `given reader error, when mapping, then reader error returns`() {
        // GIVEN
        val error = TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_READER_ERROR

        // WHEN
        val result = mapper.map(error)

        // THEN
        assertEquals(SoftwareUpdateStatusErrorType.ReaderError, result)
    }

    @Test
    fun `given server error, when mapping, then server error returns`() {
        // GIVEN
        val error = TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_SERVER_ERROR

        // WHEN
        val result = mapper.map(error)

        // THEN
        assertEquals(SoftwareUpdateStatusErrorType.ServerError, result)
    }

    @Test
    fun `given battery low error, when mapping, then battery low error returns`() {
        // GIVEN
        val batteryLevel = 0.3f
        whenever(batteryLevelProvider.invoke()).thenReturn(batteryLevel)
        val error = TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_BATTERY_LOW

        // WHEN
        val result = mapper.map(error)

        // THEN
        assertEquals(SoftwareUpdateStatusErrorType.BatteryLow(batteryLevel), result)
    }

    @Test
    fun `given interrupted error, when mapping, then interrupted error returns`() {
        // GIVEN
        val error = TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_INTERRUPTED

        // WHEN
        val result = mapper.map(error)

        // THEN
        assertEquals(SoftwareUpdateStatusErrorType.Interrupted, result)
    }
}
