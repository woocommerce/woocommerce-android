package com.woocommerce.android.support.zendesk

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.extensions.stateLogInformation
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import java.util.Locale

class ZendeskEnvironmentDataSource {
    val totalAvailableMemorySize: String get() = DeviceUtils.getTotalAvailableMemorySize()
    val deviceLanguage: String get() = Locale.getDefault().language
    val deviceLogs get() = WooLog.toString().takeLast(maxLogfileLength)

    fun generateVersionName(context: Context) = PackageUtils.getVersionName(context)

    @Suppress("DEPRECATION")
    fun generateNetworkInformation(context: Context): String {
        val networkType = when (NetworkUtils.getActiveNetworkInfo(context)?.type) {
            ConnectivityManager.TYPE_WIFI -> networkWifi
            ConnectivityManager.TYPE_MOBILE -> networkWWAN
            else -> unknownValue
        }
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
        } ?: "not_selected"

    private val SiteModel.hostURL: String
        get() = UrlUtils.removeScheme(url)
            .let { StringUtils.removeTrailingSlash(it) }
            .takeUnless { TextUtils.isEmpty(it) }
            ?: UrlUtils.getHost(xmlRpcUrl)

    companion object Constants {
        // Platform
        const val platformTag = "Android"
        const val sourcePlatform = "Mobile_-_Woo_Android"

        // Network
        const val networkWifi = "WiFi"
        const val networkWWAN = "Mobile"
        const val networkTypeLabel = "Network Type:"
        const val networkCarrierLabel = "Carrier:"
        const val networkCountryCodeLabel = "Country Code:"
        const val unknownValue = "unknown"

        // URL
        const val blogSeparator = "\n----------\n"
        const val noneValue = "none"

        const val maxLogfileLength: Int = 63000 // Max characters allowed in the system status report field
    }
}
