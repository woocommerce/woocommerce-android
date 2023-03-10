package com.woocommerce.android.support.zendesk

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import java.util.Locale
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.NetworkUtils

class ZendeskDeviceDataSource {
    val totalAvailableMemorySize get() = DeviceUtils.getTotalAvailableMemorySize()
    val deviceLogs get() = WooLog.toString().takeLast(maxLogfileLength)
    val localeLanguage get() = Locale.getDefault().language

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
            "${networkTypeLabel} $networkType",
            "${networkCarrierLabel} $carrierName",
            "${networkCountryCodeLabel} ${countryCodeLabel.uppercase(Locale.getDefault())}"
        ).joinToString(separator = "\n")
    }

    companion object Constants {
        const val networkWifi = "WiFi"
        const val networkWWAN = "Mobile"
        const val networkTypeLabel = "Network Type:"
        const val networkCarrierLabel = "Carrier:"
        const val networkCountryCodeLabel = "Country Code:"
        const val unknownValue = "unknown"

        const val maxLogfileLength: Int = 63000 // Max characters allowed in the system status report field
    }
}
