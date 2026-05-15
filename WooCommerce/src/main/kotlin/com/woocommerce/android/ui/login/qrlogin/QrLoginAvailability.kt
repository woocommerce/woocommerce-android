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
 * Decides whether the QR login entry point should be offered on this device.
 *
 * Hard requirement: the device needs a camera — there's no QR flow without one. We don't gate on
 * Google Play Services: we ship the bundled ML Kit barcode scanning variant, which is designed to
 * work on GMS-less devices. If the scanner fails at runtime (binding error, model init, etc.),
 * [com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.onBindingException] already
 * surfaces a snackbar, so offering the entry point optimistically is safe.
 *
 * We require an explicit remote=true (a null/not-yet-loaded remote
 * value is treated as off) since this is in the login flow so the remote might not be loaded yet.
 * Each install is assigned a number in
 * [ROLLOUT_BUCKET_MIN]..[ROLLOUT_BUCKET_MAX] on first read and persisted, so the decision is stable
 * across restarts. Only installs in [ROLLOUT_BUCKETS_ENABLED] get the in-app entry point via
 * [isAvailable].
 *
 * The bucket only gates discovery from within the app. If the user is already in the flow from
 * elsewhere (e.g. scanned a QR from wp-admin with a 3rd-party camera), use
 * [isAvailableForDeepLink], which still respects the feature flag and camera check but ignores the
 * rollout bucket.
 */
class QrLoginAvailability @Inject constructor(
    private val featureFlagRepository: FeatureFlagRepository,
    private val deviceFeatures: DeviceFeatures,
    private val appPrefsWrapper: AppPrefsWrapper,
) {
    fun isAvailable(): Boolean = isAvailable(applyRolloutBucket = true)

    fun isAvailableForDeepLink(): Boolean = isAvailable(applyRolloutBucket = false)

    @Suppress("ReturnCount")
    private fun isAvailable(applyRolloutBucket: Boolean): Boolean {
        val flagState = featureFlagRepository.getFlagState(FeatureFlag.QR_LOGIN)
        val override = flagState.overrideValue
        if (override != null) {
            if (!override) return false
        } else {
            if (flagState.remoteValue != true) return false
            if (applyRolloutBucket && getOrAssignRolloutBucket() !in ROLLOUT_BUCKETS_ENABLED) {
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

    private fun getOrAssignRolloutBucket(): Int =
        appPrefsWrapper.qrLoginRolloutBucket ?: Random.nextInt(ROLLOUT_BUCKET_MIN, ROLLOUT_BUCKET_MAX + 1)
            .also { appPrefsWrapper.qrLoginRolloutBucket = it }

    companion object {
        const val ROLLOUT_BUCKET_MIN = 1
        const val ROLLOUT_BUCKET_MAX = 10
        val ROLLOUT_BUCKETS_ENABLED = 1..1
    }
}
