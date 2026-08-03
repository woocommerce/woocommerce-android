package com.woocommerce.android.support.zendesk

import android.content.Context
import android.os.Build
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

/**
 * Builds the Mobile Status Report attached to support tickets — the app-level counterpart to the server-side
 * System Status Report, which carries no device or app information.
 *
 * Every section is best-effort: a failing lookup degrades to [SECTION_UNAVAILABLE] rather than failing the report,
 * because this runs on the ticket creation path and must never block a merchant from contacting support.
 */
class MobileStatusProvider @Inject constructor(
    @ApplicationContext private val context: Context,
    private val envDataSource: ZendeskEnvironmentDataSource,
    private val deviceInfo: DeviceInfoWrapper,
    private val localeProvider: LocaleProvider
) {
    @Suppress("UnusedParameter")
    suspend operator fun invoke(selectedSite: SiteModel?): String = buildString {
        appendLine(REPORT_HEADING)

        appendSection(HEADING_APP) { appSection() }
        appendSection(HEADING_DEVICE) { deviceSection() }
        appendSection(HEADING_CONNECTIVITY) { connectivitySection() }
    }

    private fun appSection(): List<String> {
        val versionName = envDataSource.generateVersionName(context)
        val versionCode = envDataSource.generateVersionCode(context)
        return listOf(
            entry("Version", "$versionName ($versionCode)"),
            entry("Build", "${BuildConfig.FLAVOR} / ${BuildConfig.BUILD_TYPE}"),
            entry("Install source", installSource())
        )
    }

    private fun deviceSection() = listOf(
        entry("Model", deviceInfo.name),
        entry("OS", "Android ${deviceInfo.osName} (API ${deviceInfo.osVersionCode})"),
        entry("Free space", envDataSource.totalAvailableMemorySize),
        entry("Screen", "${deviceInfo.screenWidthDp}x${deviceInfo.screenHeightDp} dp"),
        entry("Device locale", deviceInfo.localeTag ?: UNKNOWN),
        entry("App language", localeProvider.provideLocale()?.toLanguageTag() ?: UNKNOWN)
    )

    // Reuses the Zendesk network field's lookup, which already returns `Key: Value` lines.
    private fun connectivitySection() = envDataSource.generateNetworkInformation(context).lines()

    // A missing installer of record means the APK was sideloaded; a failed lookup means we could not tell.
    private fun installSource() = runCatching {
        val packageManager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(context.packageName)
        }
    }.fold(
        onSuccess = { installer -> installer ?: SIDELOADED },
        onFailure = { UNKNOWN }
    )

    private fun entry(key: String, value: Any?) = "$key: $value"

    private suspend fun StringBuilder.appendSection(heading: String, content: suspend () -> List<String>) {
        appendLine()
        appendLine(heading)
        runCatching { content() }
            .onSuccess { lines -> lines.forEach { appendLine(it) } }
            .onFailure { appendLine(SECTION_UNAVAILABLE) }
    }

    companion object {
        private const val REPORT_HEADING = "### Mobile Status Report generated via the WooCommerce Android app ###"

        private const val HEADING_APP = "## App"
        private const val HEADING_DEVICE = "## Device"
        private const val HEADING_CONNECTIVITY = "## Connectivity"

        private const val SECTION_UNAVAILABLE = "Info not found"
        private const val UNKNOWN = "unknown"
        private const val SIDELOADED = "sideloaded"
    }
}
