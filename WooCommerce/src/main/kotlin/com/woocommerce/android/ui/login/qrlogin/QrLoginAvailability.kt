package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

/**
 * Decides whether the QR login flow should be offered.
 *
 * The remote feature flag gates both entry points: we require an explicit `remoteValue == true`
 * (a `null`/not-yet-loaded remote value is treated as off) since this runs in the login flow before
 * remote flags are guaranteed to have loaded. Only a debug override bypasses this.
 *
 * For the in-app entry point ([isAvailable]):
 *  - The device must have a camera — we don't gate on Google Play Services: we ship the bundled
 *    ML Kit barcode scanning variant, which works on GMS-less devices. If `bindToLifecycle` still
 *    fails at runtime, [com.woocommerce.android.ui.login.qrlogin.QrLoginScannerFragment] opens the
 *    OS camera app so the merchant can scan the QR there and tap the resulting deep link.
 *
 * For deep-link entry ([isAvailableForDeepLink]), e.g. scanning a QR from wp-admin with a
 * 3rd-party camera: the camera check is skipped (they already have the token; no scanning needed
 * in-app).
 */
class QrLoginAvailability @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val deviceFeatures: DeviceFeatures,
) {
    @Suppress("ReturnCount")
    fun isAvailable(): Boolean {
        val flagState = featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)
        val override = flagState.overrideValue
        if (override != null) {
            if (!override) return false
        } else {
            if (flagState.remoteValue != true) return false
        }

        if (!deviceFeatures.hasCamera()) {
            WooLog.d(WooLog.T.LOGIN, "QR login unavailable: device has no camera")
            return false
        }

        return true
    }

    fun isAvailableForDeepLink(): Boolean {
        val flagState = featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)
        return flagState.overrideValue ?: (flagState.remoteValue == true)
    }
}
