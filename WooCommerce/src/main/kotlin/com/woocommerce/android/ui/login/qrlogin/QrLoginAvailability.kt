package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

/**
 * Decides whether the QR login entry point should be offered on this device.
 *
 * Hard requirement: the device needs a camera — there's no QR flow without one. We don't gate on
 * Google Play Services: we ship the bundled ML Kit barcode scanning variant, which is designed to
 * work on GMS-less devices. If the scanner fails at runtime (binding error, model init, etc.),
 * [com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.onBindingException] already
 * surfaces a snackbar, so offering the entry point optimistically is safe.
 */
class QrLoginAvailability @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val deviceFeatures: DeviceFeatures
) {
    fun isAvailable(): Boolean {
        if (!featureFlagRepository.isEnabled(FeatureFlag.QR_LOGIN)) return false

        if (!deviceFeatures.hasCamera()) {
            WooLog.d(WooLog.T.LOGIN, "QR login unavailable: device has no camera")
            return false
        }

        return true
    }
}
