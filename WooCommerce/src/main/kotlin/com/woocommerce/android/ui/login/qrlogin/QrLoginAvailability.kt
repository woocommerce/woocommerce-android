package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.ui.login.qrlogin.QrLoginAvailability.Companion.ROLLOUT_BUCKETS_ENABLED
import com.woocommerce.android.ui.login.qrlogin.QrLoginAvailability.Companion.ROLLOUT_BUCKET_MAX
import com.woocommerce.android.ui.login.qrlogin.QrLoginAvailability.Companion.ROLLOUT_BUCKET_MIN
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import javax.inject.Inject
import kotlin.random.Random

/**
 * Decides whether the QR login flow should be offered.
 *
 * The remote feature flag gates both entry points: we require an explicit `remoteValue == true`
 * (a `null`/not-yet-loaded remote value is treated as off) since this runs in the login flow before
 * remote flags are guaranteed to have loaded. Only a debug override bypasses this.
 *
 * For the in-app entry point ([isAvailable]):
 *  - The device must have a camera — we don't gate on Google Play Services: we ship the bundled
 *    ML Kit barcode scanning variant, which works on GMS-less devices. If the scanner fails at
 *    runtime,
 *    [com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.onBindingException]
 *    surfaces a snackbar.
 *  - Each install is assigned a number in [ROLLOUT_BUCKET_MIN]..[ROLLOUT_BUCKET_MAX] on first read
 *    and persisted, so the decision is stable across restarts. Only installs in
 *    [ROLLOUT_BUCKETS_ENABLED] get the entry point. A debug override bypasses the bucket check.
 *
 * For deep-link entry ([isAvailableForDeepLink]), e.g. scanning a QR from wp-admin with a
 * 3rd-party camera: the bucket is bypassed (the user already chose this flow), and the camera
 * check is skipped (they already have the token; no scanning needed in-app).
 */
class QrLoginAvailability @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val deviceFeatures: DeviceFeatures,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    @Suppress("ReturnCount")
    fun isAvailable(): Boolean {
        val flagState = featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)
        val override = flagState.overrideValue
        if (override != null) {
            if (!override) return false
        } else {
            if (flagState.remoteValue != true) return false
            if (getOrAssignRolloutBucket() !in ROLLOUT_BUCKETS_ENABLED) {
                WooLog.d(WooLog.T.LOGIN, "QR login unavailable: outside rollout bucket")
                return false
            }
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

    private fun getOrAssignRolloutBucket(): Int =
        appPrefsWrapper.qrLoginRolloutBucket ?: Random.nextInt(ROLLOUT_BUCKET_MIN, ROLLOUT_BUCKET_MAX + 1)
            .also { appPrefsWrapper.qrLoginRolloutBucket = it }

    companion object {
        const val ROLLOUT_BUCKET_MIN = 1
        const val ROLLOUT_BUCKET_MAX = 10
        val ROLLOUT_BUCKETS_ENABLED = 1..1
    }
}
