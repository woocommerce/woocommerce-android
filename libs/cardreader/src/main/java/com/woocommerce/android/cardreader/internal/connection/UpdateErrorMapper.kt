package com.woocommerce.android.cardreader.internal.connection

import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateStatusErrorType

internal class UpdateErrorMapper(private val batteryLevelProvider: () -> Float?) {
    fun map(error: TerminalErrorCode): SoftwareUpdateStatusErrorType =
        when (error) {
            TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_BATTERY_LOW ->
                SoftwareUpdateStatusErrorType.BatteryLow(batteryLevelProvider.invoke())
            TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_INTERRUPTED ->
                SoftwareUpdateStatusErrorType.Interrupted
            TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_READER_ERROR ->
                SoftwareUpdateStatusErrorType.ReaderError
            TerminalErrorCode.READER_SOFTWARE_UPDATE_FAILED_SERVER_ERROR ->
                SoftwareUpdateStatusErrorType.ServerError
            else -> SoftwareUpdateStatusErrorType.Failed
        }
}
