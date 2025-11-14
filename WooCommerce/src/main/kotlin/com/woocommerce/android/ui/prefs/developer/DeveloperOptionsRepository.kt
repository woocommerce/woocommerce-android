package com.woocommerce.android.ui.prefs.developer

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject

class DeveloperOptionsRepository @Inject constructor(
    private val appPrefs: AppPrefs,
    private val clearCardReaderDataAction: ClearCardReaderDataAction,
    private val cardReaderManager: CardReaderManager,
    private val crashLogging: CrashLogging,
    @AppCoroutineScope appScope: CoroutineScope
) {
    private val appPrefsTrigger = appPrefs.observePrefs().shareIn(appScope, started = SharingStarted.WhileSubscribed())
        .onStart { emit(Unit) }

    fun isSimulatedCardReaderEnabled(): Boolean = appPrefs.isSimulatedReaderEnabled

    fun observeSimulatedCardReaderEnabled() = appPrefsTrigger
        .map { appPrefs.isSimulatedReaderEnabled }
        .distinctUntilChanged()

    fun changeSimulatedReaderState(isChecked: Boolean) {
        appPrefs.isSimulatedReaderEnabled = isChecked
    }

    suspend fun clearSelectedCardReader() {
        clearCardReaderDataAction.invoke()
    }

    fun getUpdateSimulatedReaderOption(): CardReaderManager.SimulatorUpdateFrequency {
        return CardReaderManager.SimulatorUpdateFrequency.valueOf(appPrefs.updateReaderOptionSelected)
    }

    fun updateSimulatedReaderOption(selectedOption: CardReaderManager.SimulatorUpdateFrequency) {
        appPrefs.updateReaderOptionSelected = selectedOption.toString()
        reinitializeSimulatedReaderIfNeeded()
    }

    fun isInteracPaymentEnabled(): Boolean = appPrefs.isInteracEnabled

    fun observeInteracPaymentEnabled() = appPrefsTrigger
        .map { appPrefs.isInteracEnabled }
        .distinctUntilChanged()

    fun changeEnableInteracPaymentState(isChecked: Boolean) {
        appPrefs.isInteracEnabled = isChecked
        reinitializeSimulatedReaderIfNeeded()
    }

    fun observeSavedPrivacyBannerSettings() = appPrefsTrigger
        .map { appPrefs.savedPrivacySettings }
        .distinctUntilChanged()

    fun changeSavedPrivacyBannerSettings(isChecked: Boolean) {
        appPrefs.savedPrivacySettings = isChecked
    }

    private fun reinitializeSimulatedReaderIfNeeded() {
        if (cardReaderManager.initialized) {
            cardReaderManager.reinitializeSimulatedTerminal(
                updateFrequency = getUpdateSimulatedReaderOption(),
                useInterac = appPrefs.isInteracEnabled
            )
        }
    }

    fun sendTestSentryReport() {
        appPrefs.setCrashReportingEnabled(true)

        crashLogging.sendReport(
            exception = Exception("Test Sentry report from Developer Options"),
            tags = mapOf("source" to "developer_options"),
            message = "This is a test report triggered from Developer Options"
        )
    }
}
