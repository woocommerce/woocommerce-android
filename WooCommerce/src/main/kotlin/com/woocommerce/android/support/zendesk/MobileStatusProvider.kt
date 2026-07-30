package com.woocommerce.android.support.zendesk

import android.content.Context
import android.os.Build
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.background.GetBackgroundRestrictions
import com.woocommerce.android.extensions.logInformation
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.locale.LocaleProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.time.Instant
import java.time.format.DateTimeFormatter
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
    private val localeProvider: LocaleProvider,
    private val featureFlagRepository: FeatureFlagRepository,
    private val notificationSystemStatusProvider: NotificationSystemStatusProvider,
    private val notificationChannelsHandler: NotificationChannelsHandler,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus,
    private val getBackgroundRestrictions: GetBackgroundRestrictions,
    private val deviceFeatures: DeviceFeatures,
    private val getWooCorePluginCachedVersion: GetWooCorePluginCachedVersion,
    private val appPrefs: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val wooCommerceStore: WooCommerceStore,
    private val syncTimestampManager: WooPosSyncTimestampManager
) {
    /**
     * @param siteAddress the address the merchant typed into the support form, which can differ from the selected
     * site when they are contacting us precisely because the app picked up the wrong store. Absent when the report
     * is produced outside the support form.
     */
    suspend operator fun invoke(selectedSite: SiteModel?, siteAddress: String? = null): String = buildString {
        appendLine(REPORT_HEADING)
        appendLine(SCOPE_LEGEND)
        appendLine(FIELD_REFERENCE)
        val storeScope = storeScope(selectedSite)
        appendSection(HEADING_APP, SCOPE_APP_WIDE) { appSection() }
        appendSection(HEADING_DEVICE, SCOPE_APP_WIDE) { deviceSection() }
        appendSection(HEADING_CONNECTIVITY, SCOPE_APP_WIDE) { connectivitySection() }
        appendSection(HEADING_NOTIFICATIONS, SCOPE_APP_WIDE) { notificationsSection() }
        appendSection(HEADING_ACCOUNT, SCOPE_APP_WIDE) { accountSection(siteAddress) }
        appendSection(HEADING_STORE, storeScope) { storeSection(selectedSite) }
        appendSection(HEADING_PAYMENTS, storeScope) { paymentsSection(selectedSite) }
        appendSection(HEADING_POS, storeScope) { posSection(selectedSite) }
        appendSection(HEADING_FEATURE_FLAGS, SCOPE_APP_WIDE) { featureFlagsSection() }
        appendSection(HEADING_EXPERIMENTAL, SCOPE_APP_WIDE) { experimentalFeaturesSection() }
    }

    /**
     * Names the store the store-scoped sections describe, rather than saying only that they are store-scoped: a
     * merchant with several stores needs to know which one the values below belong to.
     */
    private fun storeScope(selectedSite: SiteModel?) = selectedSite
        ?.url.orEmpty().ifEmpty { null }
        ?.let { "(selected store: $it)" }
        ?: SCOPE_NO_STORE

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

    /**
     * Push registration is deliberately absent: it is the one notification value keyed on a single store, so it is
     * reported with the store it belongs to rather than among these device-level settings.
     */
    private fun notificationsSection(): List<String> {
        val disabledChannels = notificationSystemStatusProvider.disabledWooNotificationChannels()
        return listOf(
            entry("Play Services", if (deviceFeatures.isGooglePlayServicesAvailable()) "available" else "unavailable"),
            entry("Permission granted", notificationSystemStatusProvider.hasPostNotificationsPermission()),
            entry("App notifications enabled", notificationSystemStatusProvider.areAppNotificationsEnabled()),
            entry("Disabled channels", disabledChannels.joinToString().ifEmpty { NONE }),
            entry("New order sound", notificationChannelsHandler.checkNewOrderNotificationSound().describe()),
            entry("FCM token", fcmTokenState())
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
            entry("Connected stores", siteStore.sites.size)
        ) + allSites()
    }

    /**
     * The store the app currently has selected, which every following store-scoped section also describes.
     */
    private suspend fun storeSection(selectedSite: SiteModel?): List<String> {
        if (selectedSite == null) return listOf(NO_STORE_SELECTED_HINT)

        return with(selectedSite) {
            listOf(
                // Blog ID is 0 for application password sites, and Store ID is missing until the first successful
                // system status fetch. They fail in opposite conditions, so both are reported, each saying why it
                // is absent rather than leaving a bare "not set" to be interpreted.
                entry("Blog ID", siteId.takeIf { it != 0L } ?: "$NOT_SET ($REASON_NO_BLOG_ID)"),
                entry(
                    "Store ID",
                    appPrefs.getWCStoreID(siteId).orEmpty().ifEmpty { "$NOT_SET ($REASON_NO_STORE_ID)" }
                ),
                entry("Auth method", connectionType.name),
                entry(
                    "Jetpack",
                    "installed=$isJetpackInstalled connected=$isJetpackConnected CP=$isJetpackCPConnected"
                ),
                entry("Plan", "${planShortName.orEmpty().ifEmpty { UNKNOWN }} ($planId)"),
                entry("Woo core version", getWooCorePluginCachedVersion() ?: UNKNOWN),
                entry("Push registration", pushNotificationRegistrationStatus(siteId).name)
            )
        }
    }

    /**
     * Every connected site, not just the selected one — merchants often report a problem on a store other than
     * the one the app currently has selected.
     */
    private fun allSites() = siteStore.sites
        .takeIf { it.isNotEmpty() }
        ?.map { entry(it.url.orEmpty().ifEmpty { UNKNOWN }, it.logInformation) }
        ?.let { listOf("", "All connected sites:") + it }
        .orEmpty()

    /**
     * Which payment gateway is in play decides how an in-person payments problem is triaged. Today this is only
     * visible as Zendesk tags, which the merchant-facing report cannot show.
     */
    private suspend fun paymentsSection(selectedSite: SiteModel?): List<String> {
        if (selectedSite == null) return listOf(NO_STORE_SELECTED_HINT)

        // Read from the plugin cache rather than fetching, to keep ticket creation off the network. The cache can
        // be empty if nothing has fetched plugins for this site yet, which is not the same as "not installed".
        val plugins = wooCommerceStore.getSitePlugins(selectedSite)
        val installState = if (plugins.isEmpty()) {
            listOf(entry("Payment plugins", "$UNKNOWN (none cached for this site)"))
        } else {
            listOf(
                entry("WooPayments", plugins.stateOf(PluginType.WOOCOMMERCE_PAYMENTS)),
                entry("Stripe extension", plugins.stateOf(PluginType.STRIPE_EXTENSION_GATEWAY))
            )
        }

        return installState + inPersonPayments(selectedSite)
    }

    /**
     * Which gateway drives in-person payments, which is not derivable from install state when both plugins are
     * present. Read straight from the prefs rather than through `GetActivePaymentsPlugin`, which falls back to a
     * network fetch.
     */
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

    /**
     * The timestamps cover the POS local catalog only, not general order or product sync, so they are labelled
     * as such. The reason a merchant cannot see or launch POS is deliberately not computed here: both
     * `WooPosTabShouldBeVisible` and `WooPosCanBeLaunchedInTab` write these prefs as a side effect of evaluating,
     * and a status report must not mutate what it reports. They already log the reason, and that log is attached
     * to the same ticket, so the report points at it instead.
     */
    private suspend fun posSection(selectedSite: SiteModel?): List<String> {
        if (selectedSite == null) return listOf(NO_STORE_SELECTED_HINT)

        val tabVisible = appPrefs.isPOSTabVisibleForSite(selectedSite.id)
        val launchable = appPrefs.isPOSLaunchableForSite(selectedSite.id)
        return listOfNotNull(
            entry("POS tab visible", tabVisible),
            entry("POS launchable", launchable),
            entry("Local catalog full sync", syncTimestampManager.getFullSyncLastCompletedTimestamp().asUtcOrNever()),
            entry("Local catalog products sync", syncTimestampManager.getProductsLastSyncTimestamp().asUtcOrNever()),
            entry(
                "Local catalog variations sync",
                syncTimestampManager.getVariationsLastSyncTimestamp().asUtcOrNever()
            ),
            POS_REASON_HINT.takeIf { !tabVisible || !launchable }
        )
    }

    private fun Long?.asUtcOrNever() =
        this?.let { DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(it)) } ?: NEVER

    /**
     * Reports every flag with its effective value rather than only the enabled ones: an absent key is ambiguous
     * between disabled, renamed and deleted, which makes a bare list unreadable for Happiness Engineers.
     */
    private fun featureFlagsSection(): List<String> {
        val states = FeatureFlag.entries.map { featureFlagRepository.getFlagState(it) }
        // Remote values are persisted and never cleared, so a merchant whose fetch has never succeeded is running
        // entirely on compiled-in defaults. There is no other way for support to tell.
        val remoteValuesLoaded = states.any { it.remoteValue != null }
        return listOf(
            entry(
                "Remote values loaded",
                if (remoteValuesLoaded) true else "false ($REASON_NO_REMOTE_FLAGS)"
            )
        ) +
            states
                .sortedBy { it.flag.remoteFlagKey }
                .map { entry(it.flag.remoteFlagKey, "${it.effectiveValue} (${it.source()})") }
    }

    /**
     * Where the effective value came from. Without this a flag reads the same whether it was set remotely or is
     * simply the value the app shipped with, which are very different findings when a rollout is misbehaving.
     */
    private fun FeatureFlagRepository.FeatureFlagState.source() = when {
        overrideValue != null -> "debug override"
        remoteValue != null -> "remote"
        else -> "compiled-in default"
    }

    /**
     * Kept in sync by hand with the toggles in `BetaFeaturesFragment`, which carries a matching reminder. The
     * prefs behind these have no shared registry to iterate, and each toggle has its own visibility rules and
     * setter, so a data-driven list would cost more than it saves for three entries.
     */
    private fun experimentalFeaturesSection() = listOf(
        entry("Product add-ons", appPrefs.isProductAddonsEnabled),
        entry("Jetpack app passwords", appPrefs.jetpackAppPasswordsEnabled),
        entry("POS local catalog", appPrefs.wooPosLocalCatalogEnabled)
    )

    /**
     * A missing installer of record and a failed lookup mean different things: the first says the APK was
     * sideloaded rather than installed from a store, which is a finding in itself, while the second says we
     * could not tell. They are reported separately so an HE is not left guessing which one they are looking at.
     */
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

    private fun fcmTokenState() = appPrefs.getFCMToken()
        .takeIf { it.isNotBlank() }
        ?.let { "present (…${it.takeLast(REDACTED_TOKEN_LENGTH)})" }
        ?: MISSING

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

        /** Stands in for the store name when the app has no store selected, so the scope is never blank. */
        const val SCOPE_NO_STORE = "(no store selected)"

        /**
         * Spelled out once at the top rather than relying on the reader inferring what the scopes mean, because
         * this report is read by Happiness Engineers and by merchants in Help & Support alike.
         */
        const val SCOPE_LEGEND =
            "Scopes: $SCOPE_APP_WIDE values cover the whole app on this device. " +
                "(selected store: ...) values cover only the named store."

        /**
         * What each field means and what it can hold lives in the repository rather than in the report, which is
         * attached to every ticket and is also shown to merchants: a per-field gloss here would double its length
         * for readers who already know the fields.
         */
        const val FIELD_REFERENCE = "Field reference: " +
            "https://github.com/woocommerce/woocommerce-android/blob/trunk/docs/mobile-status-report.md"

        const val HEADING_APP = "## App"
        const val HEADING_DEVICE = "## Device"
        const val HEADING_CONNECTIVITY = "## Connectivity"
        const val HEADING_NOTIFICATIONS = "## Notifications"
        const val HEADING_ACCOUNT = "## Account & Stores"
        const val HEADING_STORE = "## Store Details"
        const val HEADING_PAYMENTS = "## Payments"
        const val HEADING_POS = "## Point of Sale"
        const val HEADING_FEATURE_FLAGS = "## Feature Flags"
        const val HEADING_EXPERIMENTAL = "## Experimental Features"

        const val SECTION_UNAVAILABLE = "Info not found"
        private const val UNKNOWN = "unknown"
        private const val SIDELOADED = "sideloaded"
        private const val NONE = "none"
        private const val MISSING = "missing"
        private const val NOT_SET = "not set"
        private const val NOT_LOGGED_IN = "not logged in"
        private const val NEVER = "never"
        private const val NO_STORE_SELECTED_HINT = "Not applicable while no store is selected"

        // Why a value is absent, said in the report itself. A bare "not set" reads as a fault in every one of
        // these cases, when in fact each is the expected state for a particular kind of store or setup.
        private const val REASON_NO_BLOG_ID = "stores connected with application passwords do not have one"
        private const val REASON_NO_STORE_ID = "no store system status has been fetched yet"
        private const val REASON_NO_REMOTE_FLAGS = "no remote fetch has ever succeeded on this install"
        private const val REASON_SIDELOADED = "installed outside an app store, not from Play"
        private const val POS_REASON_HINT =
            "Reason is logged - search application_log.txt for " +
                "\"POS Tab Not visible reason\" or \"POS cannot be launched\""
        private const val REDACTED_TOKEN_LENGTH = 6
    }
}
