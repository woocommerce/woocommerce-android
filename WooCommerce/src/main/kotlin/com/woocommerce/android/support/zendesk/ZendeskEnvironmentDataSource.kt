package com.woocommerce.android.support.zendesk

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.telephony.TelephonyManager
import android.text.TextUtils
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.extensions.stateLogInformation
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.StringUtils
import org.wordpress.android.util.UrlUtils
import java.util.Locale

class ZendeskEnvironmentDataSource {
    val totalAvailableMemorySize: String get() = DeviceUtils.getTotalAvailableMemorySize()
    val deviceLanguage: String get() = Locale.getDefault().language
    val deviceLogs get() = WooLog.toString().takeLast(maxLogfileLength)

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
    }
}
