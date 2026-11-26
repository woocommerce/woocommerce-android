package com.woocommerce.android.ui.woopos.cardreader

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.CardReaderStatus
import com.woocommerce.android.cardreader.connection.event.CardReaderBatteryStatus
import com.woocommerce.android.cardreader.connection.event.SoftwareUpdateAvailability
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
}
