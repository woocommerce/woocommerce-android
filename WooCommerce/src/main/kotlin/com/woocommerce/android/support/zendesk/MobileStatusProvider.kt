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
 * Builds the Mobile Status Report (MSR) attached to support tickets — the app-level counterpart to the
 * server-side WooCommerce System Status Report (SSR), which carries no device or app information.
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
    suspend operator fun invoke(selectedSite: SiteModel?): String = buildString {
        appendLine(REPORT_HEADING)
        appendSection(HEADING_APP, SCOPE_APP_WIDE) { appSection() }
        appendSection(HEADING_DEVICE, SCOPE_APP_WIDE) { deviceSection() }
        appendSection(HEADING_CONNECTIVITY, SCOPE_APP_WIDE) { connectivitySection() }
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

    /**
     * Reuses the same network lookup as the dedicated Zendesk network field so both stay consistent. The values are
     * duplicated on purpose: the report has to stand on its own once it is shown to merchants in Help & Support.
     * The lookup already returns `Key: Value` lines, so they are emitted verbatim.
     */
    private fun connectivitySection() = envDataSource.generateNetworkInformation(context).lines()

    private fun installSource() = runCatching {
        val packageManager = context.packageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(context.packageName)
        }
    }.getOrNull() ?: UNKNOWN

    private fun entry(key: String, value: Any?) = "$key: $value"

    /**
     * @param scope states who the section's values describe, so a reader never has to infer whether a value covers
     * the whole installation or a single store. Every heading carries one.
     */
    private suspend fun StringBuilder.appendSection(
        heading: String,
        scope: String,
        content: suspend () -> List<String>
    ) {
        appendLine()
        appendLine("$heading $scope")
        runCatching { content() }
            .onSuccess { lines -> lines.forEach { appendLine(it) } }
            .onFailure { appendLine(SECTION_UNAVAILABLE) }
    }

    companion object {
        const val REPORT_HEADING = "### Mobile Status Report generated via the WooCommerce Android app ###"

        /** Values covering the whole installation on this device, as opposed to a single store. */
        const val SCOPE_APP_WIDE = "(app-wide)"

        const val HEADING_APP = "## App"
        const val HEADING_DEVICE = "## Device"
        const val HEADING_CONNECTIVITY = "## Connectivity"

        const val SECTION_UNAVAILABLE = "Info not found"
        private const val UNKNOWN = "unknown"
    }
}
