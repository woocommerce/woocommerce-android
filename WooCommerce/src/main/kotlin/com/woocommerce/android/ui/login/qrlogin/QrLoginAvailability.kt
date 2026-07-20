package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject

/**
 * Decides whether the QR login flow should be offered.
 *
 * [FeatureFlag.QR_LOGIN] gates both entry points via its effective value (override → remote →
 * local). No remote flag is configured for it, so the local value (enabled) applies; creating
 * the `woo_qr_code_login` remote key would act as a kill-switch.
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
    fun isAvailable(): Boolean {
        if (!featureFlagRepository.isEnabled(FeatureFlag.QR_LOGIN)) return false

        if (!deviceFeatures.hasCamera()) {
            WooLog.d(WooLog.T.LOGIN, "QR login unavailable: device has no camera")
            return false
        }

        return true
    }

    fun isAvailableForDeepLink(): Boolean = featureFlagRepository.isEnabled(FeatureFlag.QR_LOGIN)
}
