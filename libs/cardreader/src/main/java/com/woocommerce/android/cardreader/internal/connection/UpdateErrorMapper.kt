package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType

internal class UpdateErrorMapper(private val batteryLevelProvider: () -> Float?) {
    fun map(error: TerminalException.TerminalErrorCode): SoftwareUpdateStatusErrorType =
        when (error) {
            TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_BATTERY_LOW ->
                SoftwareUpdateStatusErrorType.BatteryLow(batteryLevelProvider.invoke())
            TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_INTERRUPTED ->
                SoftwareUpdateStatusErrorType.Interrupted
            TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_READER_ERROR ->
                SoftwareUpdateStatusErrorType.ReaderError
            TerminalException.TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_SERVER_ERROR ->
                SoftwareUpdateStatusErrorType.ServerError
            else -> SoftwareUpdateStatusErrorType.Failed
        }
}
