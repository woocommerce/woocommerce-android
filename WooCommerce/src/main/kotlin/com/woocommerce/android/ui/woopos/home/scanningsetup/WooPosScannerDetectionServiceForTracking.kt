package com.woocommerce.android.ui.woopos.home.scanningsetup

import com.woocommerce.android.ui.woopos.common.util.ScannerInfo
import com.woocommerce.android.ui.woopos.common.util.WooPosScannerDetectionUtil
import com.woocommerce.android.ui.woopos.home.scanningsetup.WooPosScanningSetupState.BarcodeReaderDevice
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class WooPosScannerDetectionServiceForTracking @Inject constructor(
    private val scannerDetectionUtil: WooPosScannerDetectionUtil,
    private val analyticsTracker: WooPosScanningSetupAnalyticsTracker,
) {
    private var detectionJob: Job? = null
    private var hasDetectedScanner = false

    fun startPeriodicDetection(
        coroutineScope: CoroutineScope,
        getSelectedDevice: () -> BarcodeReaderDevice?
    ) {
        if (detectionJob?.isActive == true) return
        hasDetectedScanner = false

        detectionJob = coroutineScope.launch {
            while (!hasDetectedScanner) {
                val scannerInfo = scannerDetectionUtil.detectConnectedScanner()
                if (scannerInfo is ScannerInfo.Connected) {
                    hasDetectedScanner = true
                    val selectedDevice = getSelectedDevice()
                    val scannerInfoString = scannerDetectionUtil.getScannerInfoString(scannerInfo)
                    analyticsTracker.trackScannerConnected(selectedDevice, scannerInfoString)
                } else {
                    delay(SCANNER_DETECTION_INTERVAL_MS)
                }
            }
        }
    }

    fun stopPeriodicDetection() {
        detectionJob?.cancel()
        detectionJob = null
        hasDetectedScanner = false
    }

    private companion object {
        private const val SCANNER_DETECTION_INTERVAL_MS = 3000L
    }
}
