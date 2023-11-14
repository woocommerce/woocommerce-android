package com.woocommerce.android.cardreader.connection.event

import com.stripe.stripeterminal.external.models.BatteryStatus.CRITICAL
import com.stripe.stripeterminal.external.models.BatteryStatus.LOW
import com.stripe.stripeterminal.external.models.BatteryStatus.NOMINAL
import com.stripe.stripeterminal.external.models.BatteryStatus.UNKNOWN

sealed class CardReaderBatteryStatus {
    data class StatusChanged(
        val batteryLevel: Float,
        val batteryStatus: BatteryStatus,
        val isCharging: Boolean
    ) : CardReaderBatteryStatus()

    object Warning : CardReaderBatteryStatus()
    object Unknown : CardReaderBatteryStatus()
}

enum class BatteryStatus {
    UNKNOWN,
    CRITICAL,
    LOW,
    NOMINAL,
}

internal fun com.stripe.stripeterminal.external.models.BatteryStatus.toLocalBatteryStatus() =
    when (this) {
        UNKNOWN -> BatteryStatus.UNKNOWN
        CRITICAL -> BatteryStatus.CRITICAL
        LOW -> BatteryStatus.LOW
        NOMINAL -> BatteryStatus.NOMINAL
    }
