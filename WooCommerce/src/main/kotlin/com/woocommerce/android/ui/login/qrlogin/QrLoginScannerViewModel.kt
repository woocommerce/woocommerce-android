package com.woocommerce.android.ui.login.qrlogin

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Processes a scanned QR code coming from [com.woocommerce.android.ui.barcodescanner.BarcodeScanningFragment]
 * and dispatches the parsed payload to the hosting [com.woocommerce.android.ui.login.LoginActivity].
 */
@HiltViewModel
class QrLoginScannerViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val parser: QrLoginPayloadParser
) : ScopedViewModel(savedState) {

    private var processed = false

    fun onScanResult(status: CodeScannerStatus) {
        if (processed) return
        processed = true

        when (status) {
            is CodeScannerStatus.Success -> {
                when (val payload = parser.parse(status.code)) {
                    is QrLoginPayload.SiteAppPassword -> triggerEvent(Dispatch.SiteAppPassword(payload))
                    is QrLoginPayload.WpComToken -> triggerEvent(Dispatch.WpComToken(payload))
                    is QrLoginPayload.UrlOnly -> triggerEvent(Dispatch.UrlOnly(payload))
                    QrLoginPayload.Invalid -> {
                        processed = false
                        triggerEvent(Dispatch.InvalidPayload)
                    }
                }
            }

            is CodeScannerStatus.Failure -> {
                processed = false
                triggerEvent(Dispatch.ScannerFailure)
            }

            CodeScannerStatus.NotFound -> {
                processed = false
            }
        }
    }

    sealed class Dispatch : Event() {
        data class SiteAppPassword(val payload: QrLoginPayload.SiteAppPassword) : Dispatch()
        data class WpComToken(val payload: QrLoginPayload.WpComToken) : Dispatch()
        data class UrlOnly(val payload: QrLoginPayload.UrlOnly) : Dispatch()
        data object InvalidPayload : Dispatch()
        data object ScannerFailure : Dispatch()
    }
}
