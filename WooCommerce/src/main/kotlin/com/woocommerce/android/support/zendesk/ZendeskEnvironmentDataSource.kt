package com.woocommerce.android.support.zendesk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.os.StatFs
import android.telephony.TelephonyManager
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.extensions.stateLogInformation
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToLong

class ZendeskEnvironmentDataSource @Inject constructor() {
    /**
     * Free space on the partition the app stores its data on.
     *
     * Measured here rather than via `DeviceUtils.getTotalAvailableMemorySize()`, which adds the free space of the
     * internal and the external storage together. On a device with emulated external storage — which is every
     * current device — both are views of the same partition, so it reported roughly twice the space actually free.
     * It also stopped formatting at megabytes, so a half-full 128 GB phone read as `104,857MB`.
     */
    val totalAvailableMemorySize: String
        get() = formatAvailableSpace(StatFs(Environment.getDataDirectory().path).availableBytes)

    val deviceLanguage: String get() = Locale.getDefault().language

    fun generateVersionName(context: Context) = PackageUtils.getVersionName(context)

    fun generateNetworkInformation(context: Context): String {
        val networkType = generateNetworkType(context)
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        val carrierName = telephonyManager?.networkOperatorName ?: unknownValue
        val countryCodeLabel = telephonyManager?.networkCountryIso ?: unknownValue
        return listOf(
            "$networkTypeLabel $networkType",
            "$networkCarrierLabel $carrierName",
            "$networkCountryCodeLabel ${countryCodeLabel.uppercase(Locale.getDefault())}"
        ).joinToString(separator = "\n")
    }

    /**
     * This is a small helper function which just joins the `logInformation` of all the sites passed in with a separator.
     */
    fun generateCombinedLogInformationOfSites(allSites: List<SiteModel>?): String {
        return allSites?.let { sites ->
            sites.joinToString(separator = blogSeparator) { it.logInformation }
        } ?: noneValue
    }

    fun generateHostData(selectedSite: SiteModel?) =
        selectedSite?.let {
            "${selectedSite.hostURL} (${selectedSite.stateLogInformation})"
        } ?: unknownHostValue

    suspend fun getFullDeviceLogs() = WooLog.getCurrentLogEntries().joinToString("\n")

    fun trimDeviceLogs(logs: String) = logs.takeLast(maxLogfileLength)

    private val SiteModel.hostURL: String
        get() = UrlUtils.removeScheme(url)
            .let { StringUtils.removeTrailingSlash(it) }
            .takeUnless { TextUtils.isEmpty(it) }
            ?: UrlUtils.getHost(xmlRpcUrl)

    /**
     * This is a helper function which returns information about the network state of the app to be sent to Zendesk, which
     * could prove useful for the Happiness Engineers while debugging the users' issues.
     */
    private fun generateNetworkType(context: Context) =
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
            .run { this as? ConnectivityManager }
            ?.let { it.getNetworkCapabilities(it.activeNetwork) }
            ?.let {
                when {
                    it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> networkWifi
                    it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> networkWWAN
                    else -> unknownValue
                }
            } ?: unknownValue

    companion object Constants {
        // Platform
        const val sourcePlatform = "Mobile_-_Woo_Android"

        // Network
        const val networkWifi = "WiFi"
        const val networkWWAN = "Mobile"
        const val networkTypeLabel = "Network Type:"
        const val networkCarrierLabel = "Carrier:"
        const val networkCountryCodeLabel = "Country Code:"
        const val unknownValue = "unknown"
        const val unknownHostValue = "not_selected"

        // URL
        const val blogSeparator = "\n----------\n"
        const val noneValue = "none"

        const val maxLogfileLength: Int = 63000 // Max characters allowed in the system status report field

        // Storage — SI units, matching what Android Settings shows since API 26
        private const val bytesPerUnit = 1000.0
        private const val displayedDecimalFactor = 10.0
        private val spaceUnits = listOf("B", "KB", "MB", "GB", "TB")

        @VisibleForTesting
        internal fun formatAvailableSpace(bytes: Long): String {
            var value = bytes.toDouble()
            var unitIndex = 0
            // Compare the displayed (one-decimal) value, so e.g. 999.99 MB promotes to 1.0 GB, not 1000.0 MB
            while (roundToDisplayed(value) >= bytesPerUnit && unitIndex < spaceUnits.lastIndex) {
                value /= bytesPerUnit
                unitIndex++
            }
            return String.format(Locale.US, "%.1f %s", value, spaceUnits[unitIndex])
        }

        private fun roundToDisplayed(value: Double) =
            (value * displayedDecimalFactor).roundToLong() / displayedDecimalFactor
    }
}
