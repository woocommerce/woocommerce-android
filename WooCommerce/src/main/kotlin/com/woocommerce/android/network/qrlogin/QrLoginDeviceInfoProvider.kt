package com.woocommerce.android.network.qrlogin

import android.os.Build
import com.google.gson.annotations.SerializedName
import com.woocommerce.android.BuildConfig
import javax.inject.Inject

/**
 * Wraps the Android `Build` and `BuildConfig` lookups behind an injectable seam so the
 * QR exchange request can include device metadata without making `QrLoginRestClient`
 * untestable.
 *
 * Production reads `Build.MODEL` / `Build.BRAND` / `Build.VERSION.RELEASE` / `BuildConfig.VERSION_NAME`;
 * tests pass a fake instance with deterministic values.
 *
 * The fields here are mirrored on the server side under the `device` whitelist in
 * `MobileAppQRLogin::DEVICE_PAYLOAD_KEYS` (WooCommerce Core). Each field is length-capped
 * server-side, so we don't need to truncate locally.
 */
class QrLoginDeviceInfoProvider @Inject constructor() {
    fun get(): QrLoginDeviceInfo = QrLoginDeviceInfo(
        os = OS_NAME,
        osVersion = Build.VERSION.RELEASE.orEmpty(),
        model = Build.MODEL.orEmpty(),
        brand = Build.BRAND.orEmpty(),
        appVersion = BuildConfig.VERSION_NAME.orEmpty(),
    )

    private companion object {
        const val OS_NAME = "Android"
    }
}

/**
 * JSON-serializable device metadata sent on the QR login exchange request.
 *
 * Field names map to the server whitelist defined in
 * `MobileAppQRLogin::DEVICE_PAYLOAD_KEYS` (WooCommerce Core). Anything outside that
 * whitelist is silently dropped server-side, so adding new fields here without a
 * matching server change is safe but pointless.
 */
data class QrLoginDeviceInfo(
    @SerializedName("os") val os: String,
    @SerializedName("os_version") val osVersion: String,
    @SerializedName("model") val model: String,
    @SerializedName("brand") val brand: String,
    @SerializedName("app_version") val appVersion: String,
)
