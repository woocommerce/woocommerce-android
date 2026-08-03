package com.woocommerce.android.support.zendesk

import android.content.Context
import android.os.Build
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.background.GetBackgroundRestrictions
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.tools.connectionTypeOrNull
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsLocalCatalogSupported
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Builds the Mobile Status Report attached to support tickets — the app-level counterpart to the server-side
 * System Status Report, which carries no device or app information. `docs/mobile-status-report.md` is the
 * reference for what every field means and how to read an absent value; keep it in step with this class.
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
    private val getWooCorePluginCachedVersion: GetWooCorePluginCachedVersion,
    private val appPrefs: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore,
    private val posLocalCatalogStore: WooPosLocalCatalogStore,
    private val syncTimestampManager: WooPosSyncTimestampManager,
    private val posPreferencesRepository: WooPosPreferencesRepository,
    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported
) {
    /**
     * @param siteAddress the address the merchant typed into the support form, which can differ from the selected
     * site when they are contacting us precisely because the app picked up the wrong store. Absent when the report
     * is produced outside the support form.
     */
    suspend operator fun invoke(selectedSite: SiteModel?, siteAddress: String? = null): String = buildString {
        appendLine(REPORT_HEADING)
        appendLine(FIELD_REFERENCE)

        appendSection(HEADING_APP) { appSection() }
        appendSection(HEADING_DEVICE) { deviceSection() }
        appendSection(HEADING_CONNECTIVITY) { connectivitySection() }
        appendSection(HEADING_NOTIFICATIONS) { notificationsSection() }
        appendSection(HEADING_ACCOUNT) { accountSection(siteAddress) }
        appendSection(HEADING_FEATURE_FLAGS) { featureFlagsSection() }
        appendSection(HEADING_EXPERIMENTAL) { experimentalFeaturesSection() }

        // Everything above describes the whole app on this device, everything below only the named store. The
        // band says so once, instead of every heading carrying a scope and a legend explaining what it means.
        appendLine()
        appendLine(selectedSite?.let { "# Selected store: ${it.storeLabel()}" } ?: HEADING_NO_STORE)
        if (selectedSite == null) return@buildString

        appendSection(HEADING_STORE) { storeSection(selectedSite) }
        appendSection(HEADING_PAYMENTS) { paymentsSection(selectedSite) }
        appendSection(HEADING_POS) { posSection(selectedSite) }
    }

    private fun SiteModel.storeLabel() = url.orEmpty()
        .ifBlank { name.orEmpty() }
        .ifBlank { "local id $id" }

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

    private suspend fun storeSection(selectedSite: SiteModel) = with(selectedSite) {
        listOf(
            entry("Blog ID", siteId.takeIf { it != 0L } ?: "$NOT_SET ($REASON_NO_BLOG_ID)"),
            entry(
                "Store ID",
                appPrefs.getWCStoreID(siteId).orEmpty().ifEmpty { "$NOT_SET ($REASON_NO_STORE_ID)" }
            ),
            entry("Auth method", connectionTypeOrNull?.name ?: UNKNOWN),
            entry("Site supports app passwords", isApplicationPasswordsSupported),
            entry("Jetpack", "installed=$isJetpackInstalled connected=$isJetpackConnected CP=$isJetpackCPConnected"),
            entry("Plan", "${planShortName.orEmpty().ifEmpty { UNKNOWN }} ($planId)"),
            entry("Woo core version", getWooCorePluginCachedVersion() ?: UNKNOWN)
        )
    }

    // Non-Woo sites are excluded here and from the count: the site store holds every site on the WPCom account.
    private fun wooSites() = siteStore.sites.filter { it.hasWooCommerce }

    private fun allSites() = wooSites()
        .takeIf { it.isNotEmpty() }
        ?.map { entry(it.url.orEmpty().ifEmpty { UNKNOWN }, it.logInformation) }
        ?.let { listOf("", "All connected stores:") + it }
        .orEmpty()

    private suspend fun paymentsSection(selectedSite: SiteModel): List<String> {
        // Read from the plugin cache rather than fetching, to keep ticket creation off the network.
        val plugins = wooCommerceStore.getSitePlugins(selectedSite)
        val installState = if (plugins.isEmpty()) {
            listOf(entry("Payment plugins", "$UNKNOWN ($REASON_NOT_CACHED)"))
        } else {
            listOf(
                entry("WooPayments", plugins.stateOf(PluginType.WOOCOMMERCE_PAYMENTS)),
                entry("Stripe extension", plugins.stateOf(PluginType.STRIPE_EXTENSION_GATEWAY))
            )
        }

        return installState + inPersonPayments(selectedSite)
    }

    // Read straight from the prefs rather than through `GetActivePaymentsPlugin`, which falls back to a fetch.
    private fun inPersonPayments(site: SiteModel): List<String> {
        val preferredPlugin = appPrefs.getCardReaderPreferredPlugin(site.id, site.siteId, site.selfHostedSiteId)
        val version = preferredPlugin?.let {
            appPrefs.getCardReaderPreferredPluginVersion(site.id, site.siteId, site.selfHostedSiteId, it)
        }
        val explicitlySelected =
            appPrefs.isCardReaderPluginExplicitlySelected(site.id, site.siteId, site.selfHostedSiteId)
        return listOf(
            entry(
                "In-person payments plugin",
                preferredPlugin?.let { "${it.name} ${version.orEmpty().ifEmpty { UNKNOWN }}" } ?: NOT_SET
            ),
            entry("In-person payments plugin chosen by merchant", explicitlySelected),
            entry(
                "In-person payments onboarding",
                appPrefs.getCardReaderOnboardingStatus(site.id, site.siteId, site.selfHostedSiteId).name
            )
        )
    }

    private fun List<SitePluginModel>.stateOf(type: PluginType): String {
        val plugin = firstOrNull { it.name.endsWith(type.pluginName) } ?: return "not installed"
        val state = if (plugin.isActive) "active" else "installed, not active"
        return "$state ${plugin.version.orEmpty().ifEmpty { UNKNOWN }}"
    }

    // The prefs are read directly because `WooPosTabShouldBeVisible` and `WooPosCanBeLaunchedInTab` write them as
    // a side effect of evaluating, and a status report must not mutate what it reports.
    private suspend fun posSection(selectedSite: SiteModel): List<String> {
        val tabVisible = appPrefs.isPOSTabVisibleForSite(selectedSite.id)
        val launchable = appPrefs.isPOSLaunchableForSite(selectedSite.id)
        return listOfNotNull(
            entry("POS tab visible", tabVisible),
            entry("POS launchable", launchable),
            entry("Catalog strategy", catalogStrategy(tabVisible, launchable)),
            // The background full sync stops running once POS has gone unopened for long enough, so a catalog
            // that is stale for no other visible reason is explained by this date rather than by a sync failure.
            safeEntry("POS last opened") { posPreferencesRepository.getLastUsedTimestamp().asUtcOrNever() },
            // `unknown` rather than `0` on a failed query: an empty catalog and an unreadable one are
            // different findings, and the sync repository's own accessors collapse both to zero.
            safeEntry("Local catalog products") {
                posLocalCatalogStore.getProductCount(LocalId(selectedSite.id)).getOrNull() ?: UNKNOWN
            },
            safeEntry("Local catalog variations") {
                posLocalCatalogStore.getVariationCount(LocalId(selectedSite.id)).getOrNull() ?: UNKNOWN
            },
            // The sync timestamps parse stored strings, so a malformed value throws rather than reading as absent.
            safeEntry("Local catalog full sync") {
                syncTimestampManager.getFullSyncLastCompletedTimestamp().asUtcOrNever()
            },
            safeEntry("Products timestamp") { syncTimestampManager.getProductsLastSyncTimestamp().asUtcOrNever() },
            safeEntry("Variations timestamp") {
                syncTimestampManager.getVariationsLastSyncTimestamp().asUtcOrNever()
            },
            safeEntry("Catalog file blocked") { syncTimestampManager.isCatalogFileBlocked() },
            // Never disables the background sync, only narrows when it may run, so a merchant who turned this off
            // on a device that is rarely on Wi-Fi has a catalog that falls behind without anything failing.
            safeEntry("Full sync on cellular allowed") {
                posPreferencesRepository.allowCellularDataUpdate.first()
            },
            POS_REASON_HINT.takeIf { !tabVisible || !launchable }
        )
    }

    private fun catalogStrategy(tabVisible: Boolean, launchable: Boolean) =
        if (isLocalCatalogSupported.asOfLastEvaluation(tabVisible, launchable)) "local catalog" else "remote"

    private fun Long?.asUtcOrNever() =
        this?.let { DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(it)) } ?: NEVER

    private fun featureFlagsSection(): List<String> {
        val states = FeatureFlag.entries.map { featureFlagRepository.getFlagState(it) }
        val remoteValuesLoaded = states.any { it.remoteValue != null }
        return listOf(
            entry("Remote values loaded", if (remoteValuesLoaded) true else "false ($REASON_NO_REMOTE_FLAGS)")
        ) +
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
        onSuccess = { installer -> installer ?: "$SIDELOADED ($REASON_SIDELOADED)" },
        onFailure = { UNKNOWN }
    )

    private fun pushTokenState() = appPrefs.getFCMToken()
        .takeIf { it.isNotBlank() }
        ?.let { "present (…${it.takeLast(REDACTED_TOKEN_LENGTH)})" }
        ?: MISSING

    private fun entry(key: String, value: Any?) = "$key: $value"

    // Confines a failing lookup to the field that caused it, rather than losing the whole section with it.
    private suspend fun safeEntry(key: String, value: suspend () -> Any?) =
        runCatching { entry(key, value()) }
            .onFailure { currentCoroutineContext().ensureActive() }
            .getOrElse { entry(key, UNKNOWN) }

    private suspend fun StringBuilder.appendSection(heading: String, content: suspend () -> List<String>) {
        appendLine()
        appendLine(heading)
        runCatching { content() }
            .onSuccess { lines -> lines.forEach { appendLine(it) } }
            .onFailure { appendLine(SECTION_UNAVAILABLE) }
    }

    companion object {
        private const val REPORT_HEADING = "### Mobile Status Report generated via the WooCommerce Android app ###"

        private const val HEADING_NO_STORE = "# No store selected"
        private const val HEADING_APP = "## App"
        private const val HEADING_DEVICE = "## Device"
        private const val HEADING_CONNECTIVITY = "## Connectivity"
        private const val HEADING_NOTIFICATIONS = "## Notifications"
        private const val HEADING_ACCOUNT = "## Account & Stores"
        private const val HEADING_STORE = "## Store Details"
        private const val HEADING_PAYMENTS = "## Payments"
        private const val HEADING_POS = "## Point of Sale"
        private const val HEADING_FEATURE_FLAGS = "## Feature Flags"
        private const val HEADING_EXPERIMENTAL = "## Experimental Features"

        private const val SECTION_UNAVAILABLE = "Info not found"
        private const val UNKNOWN = "unknown"
        private const val SIDELOADED = "sideloaded"
        private const val NONE = "none"
        private const val MISSING = "missing"
        private const val NOT_SET = "not set"
        private const val NOT_LOGGED_IN = "not logged in"
        private const val NEVER = "never"
        private const val REDACTED_TOKEN_LENGTH = 6

        // What every field means, and what an absent value means, lives in the doc rather than in the report.
        private const val FIELD_REFERENCE = "Field reference: " +
            "https://github.com/woocommerce/woocommerce-android/blob/trunk/docs/mobile-status-report.md"

        // Why a value is absent, said in the report itself rather than left to the reader. A bare "not set" reads
        // as a fault in every one of these cases, when each is the expected state for some kind of store or setup.
        private const val REASON_NO_BLOG_ID = "stores connected with application passwords do not have one"
        private const val REASON_NO_STORE_ID = "no store system status has been fetched yet"
        private const val REASON_NOT_CACHED = "none cached for this site"
        private const val REASON_NO_REMOTE_FLAGS =
            "every flag below is on its compiled-in default - no fetch has succeeded on this install, " +
                "none has completed since launch, or the ones that did returned no key listed here"
        private const val REASON_SIDELOADED = "installed outside an app store, not from Play"
        private const val POS_REASON_HINT =
            "Reason is logged - search application_log.txt for " +
                "\"POS Tab Not visible reason\" or \"POS cannot be launched\""
    }
}
