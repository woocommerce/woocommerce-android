package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.ReaderType
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosCardReaderFacade @Inject constructor(
    private val cardReaderManager: CardReaderManager,
) {
    val readerStatus: StateFlow<CardReaderStatus> = cardReaderManager.readerStatus
    val softwareUpdateAvailability: Flow<SoftwareUpdateAvailability> = cardReaderManager.softwareUpdateAvailability
    val batteryStatus: Flow<CardReaderBatteryStatus> = cardReaderManager.batteryStatus

    fun cancelReconnection() {
        cardReaderManager.cancelReconnection()
    }

    fun getConnectedReaderType(): CardReaderType {
        val status = readerStatus.value
        return if (status is CardReaderStatus.Connected && ReaderType.isBuiltInReaderType(status.cardReader.type)) {
            CardReaderType.BUILT_IN
        } else {
            CardReaderType.EXTERNAL
        }
    }
}
