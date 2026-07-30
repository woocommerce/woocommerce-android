package com.woocommerce.android.support.zendesk

import android.content.Context
import android.os.Build
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.background.GetBackgroundRestrictions
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
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
    private val localeProvider: LocaleProvider,
    private val featureFlagRepository: FeatureFlagRepository,
    private val notificationSystemStatusProvider: NotificationSystemStatusProvider,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val getBackgroundRestrictions: GetBackgroundRestrictions,
    private val deviceFeatures: DeviceFeatures,
    private val appPrefs: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore
) {
    /**
     * @param siteAddress the address the merchant typed into the support form, which can differ from the selected
     * site when they are contacting us precisely because the app picked up the wrong store. Absent when the report
     * is produced outside the support form.
     */
    suspend operator fun invoke(selectedSite: SiteModel?, siteAddress: String? = null): String = buildString {
        appendLine(REPORT_HEADING)

        appendSection(HEADING_APP) { appSection() }
        appendSection(HEADING_DEVICE) { deviceSection() }
        appendSection(HEADING_CONNECTIVITY) { connectivitySection() }
        appendSection(HEADING_NOTIFICATIONS) { notificationsSection() }
        appendSection(HEADING_ACCOUNT) { accountSection(siteAddress) }
        appendSection(HEADING_FEATURE_FLAGS) { featureFlagsSection() }
        appendSection(HEADING_EXPERIMENTAL) { experimentalFeaturesSection() }
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

    // Push registration is not here: it is keyed on a store, so it sits with the store it belongs to.
    private fun notificationsSection(): List<String> {
        val disabledChannels = notificationSystemStatusProvider.disabledWooNotificationChannels()
        return listOf(
            entry("Play Services", if (deviceFeatures.isGooglePlayServicesAvailable()) "available" else "unavailable"),
            entry("Permission granted", notificationSystemStatusProvider.hasPostNotificationsPermission()),
            entry("App notifications enabled", notificationSystemStatusProvider.areAppNotificationsEnabled()),
            entry("Disabled channels", disabledChannels.joinToString().ifEmpty { NONE }),
            entry("New order sound", notificationChannelsHandler.checkNewOrderNotificationSound().describe()),
            entry("Push token", pushTokenState())
        ) + backgroundRestrictions()
    }

    private fun NewOrderNotificationSoundStatus.describe() = when (this) {
        NewOrderNotificationSoundStatus.DEFAULT -> "default"
        NewOrderNotificationSoundStatus.DISABLED -> "disabled"
        NewOrderNotificationSoundStatus.SOUND_MODIFIED -> "changed from the default"
    }

    private fun backgroundRestrictions() = with(getBackgroundRestrictions()) {
        listOf(
            entry("Background restricted", isBackgroundRestricted),
            entry("Power save mode", isPowerSaveModeEnabled),
            entry("Data saver", isDataSaverEnabled)
        )
    }

    private fun accountSection(siteAddress: String?): List<String> {
        val userId = accountStore.account?.userId ?: 0L
        return listOfNotNull(
            entry("WPCom user ID", userId.takeIf { it != 0L } ?: NOT_LOGGED_IN),
            siteAddress?.takeIf { it.isNotBlank() }?.let { entry("Address given in the form", it) },
            entry("Connected stores", wooSites().size)
        ) + allSites()
    }

    // Non-Woo sites are excluded here and from the count: the site store holds every site on the WPCom account.
    private fun wooSites() = siteStore.sites.filter { it.hasWooCommerce }

    private fun allSites() = wooSites()
        .takeIf { it.isNotEmpty() }
        ?.map { entry(it.url.orEmpty().ifEmpty { UNKNOWN }, it.logInformation) }
        ?.let { listOf("", "All connected stores:") + it }
        .orEmpty()

    private fun featureFlagsSection(): List<String> {
        val states = FeatureFlag.entries.map { featureFlagRepository.getFlagState(it) }
        return listOf(entry("Remote values loaded", states.any { it.remoteValue != null })) +
            states
                .sortedBy { it.flag.remoteFlagKey }
                .map { entry(it.flag.remoteFlagKey, "${it.effectiveValue} (${it.source()})") }
    }

    private fun FeatureFlagRepository.FeatureFlagState.source() = when {
        overrideValue != null -> "debug override"
        remoteValue != null -> "remote"
        else -> "compiled-in default"
    }

    // Kept in sync by hand with the toggles in `BetaFeaturesFragment`, which carries a matching reminder.
    private fun experimentalFeaturesSection() = listOf(
        entry("Product add-ons", appPrefs.isProductAddonsEnabled),
        entry("Jetpack app passwords", appPrefs.jetpackAppPasswordsEnabled),
        entry("POS local catalog", appPrefs.wooPosLocalCatalogEnabled)
    )

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

    private fun pushTokenState() = appPrefs.getFCMToken()
        .takeIf { it.isNotBlank() }
        ?.let { "present (…${it.takeLast(REDACTED_TOKEN_LENGTH)})" }
        ?: MISSING

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
        private const val HEADING_NOTIFICATIONS = "## Notifications"
        private const val HEADING_ACCOUNT = "## Account & Stores"
        private const val HEADING_FEATURE_FLAGS = "## Feature Flags"
        private const val HEADING_EXPERIMENTAL = "## Experimental Features"

        private const val SECTION_UNAVAILABLE = "Info not found"
        private const val UNKNOWN = "unknown"
        private const val SIDELOADED = "sideloaded"
        private const val NONE = "none"
        private const val MISSING = "missing"
        private const val NOT_LOGGED_IN = "not logged in"
        private const val REDACTED_TOKEN_LENGTH = 6
    }
}
