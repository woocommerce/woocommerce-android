package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

/**
 * Decides whether the QR login entry point should be offered on this device.
 *
 * We skip the QR flow entirely (falling straight through to the existing site URL step) when:
 *   - The [FeatureFlag.QR_LOGIN] flag is off.
 *   - The device has no camera ([android.content.pm.PackageManager.FEATURE_CAMERA_ANY]).
 *     Scanning a QR is the whole point of the flow; without a camera it's dead weight.
 *   - Google Play Services aren't available. The bundled ML Kit variant we ship also transitively
 *     pulls in `play-services-mlkit-barcode-scanning` and uses it when present. To avoid shipping
 *     a scan button that might silently fail on GMS-less devices (AOSP builds, some Huawei phones,
 *     etc.) we gate on GMS being present. If the bundled fallback later proves reliable across
 *     that fleet this check can be loosened.
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

        if (!deviceFeatures.isGooglePlayServicesAvailable()) {
            WooLog.d(WooLog.T.LOGIN, "QR login unavailable: Google Play Services missing")
            return false
        }

        return true
    }
}
