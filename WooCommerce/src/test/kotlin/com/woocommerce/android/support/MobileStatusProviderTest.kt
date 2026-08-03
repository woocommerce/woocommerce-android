package com.woocommerce.android.support

import android.content.Context
import android.content.pm.PackageManager
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.background.GetBackgroundRestrictions
import com.woocommerce.android.background.GetBackgroundRestrictions.BackgroundRestrictions
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider
import com.woocommerce.android.ui.woopos.localcatalog.WooPosIsLocalCatalogSupported
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.FeatureFlagRepository.FeatureFlagState
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.plugin.SitePluginModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.pos.localcatalog.WooPosLocalCatalogStore
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class MobileStatusProviderTest : BaseUnitTest() {
    private val envDataSource: ZendeskEnvironmentDataSource = mock {
        on { generateVersionName(any()) } doReturn "21.3"
        on { generateVersionCode(any()) } doReturn 2103003
        on { totalAvailableMemorySize } doReturn "12.4 GB"
        on { generateNetworkInformation(any()) } doReturn
            "Network Type: WiFi\nCarrier: Test Carrier\nCountry Code: GB"
    }

    private val deviceInfo: DeviceInfoWrapper = mock {
        on { name } doReturn "Google Pixel 8"
        on { osName } doReturn "15"
        on { osVersionCode } doReturn 35
        on { screenWidthDp } doReturn 411
        on { screenHeightDp } doReturn 914
        on { localeTag } doReturn "en-US"
    }

    private val localeProvider: LocaleProvider = mock {
        on { provideLocale() } doReturn Locale.UK
    }

    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { getFlagState(any()) } doAnswer { invocation ->
            FeatureFlagState(
                flag = invocation.getArgument(0),
                localValue = false,
                remoteValue = true,
                overrideValue = null
            )
        }
    }

    private val notificationSystemStatusProvider: NotificationSystemStatusProvider = mock {
        on { hasPostNotificationsPermission() } doReturn true
        on { areAppNotificationsEnabled() } doReturn true
        on { disabledWooNotificationChannels() } doReturn listOf(NotificationChannelType.REVIEW)
    }

    private val notificationChannelsHandler: NotificationChannelsHandler = mock {
        on { checkNewOrderNotificationSound() } doReturn NewOrderNotificationSoundStatus.SOUND_MODIFIED
    }

    private val getBackgroundRestrictions: GetBackgroundRestrictions = mock {
        on { invoke() } doReturn BackgroundRestrictions(
            isDataSaverEnabled = true,
            isPowerSaveModeEnabled = false,
            isBackgroundRestricted = false
        )
    }

    private val deviceFeatures: DeviceFeatures = mock {
        on { isGooglePlayServicesAvailable() } doReturn true
    }

    private val getWooCorePluginCachedVersion: GetWooCorePluginCachedVersion = mock {
        on { invoke() } doReturn "10.9.2"
    }

    private val appPrefs: AppPrefsWrapper = mock {
        on { getFCMToken() } doReturn "abcdefghijklmnop"
        on { getWCStoreID(any()) } doReturn "store-uuid"
        on { isProductAddonsEnabled } doReturn true
        on { jetpackAppPasswordsEnabled } doReturn false
        on { wooPosLocalCatalogEnabled } doReturn true
        on { getCardReaderPreferredPlugin(any(), any(), any()) } doReturn PluginType.WOOCOMMERCE_PAYMENTS
        on { getCardReaderPreferredPluginVersion(any(), any(), any(), any()) } doReturn "11.0.0"
        on { isCardReaderPluginExplicitlySelected(any(), any(), any()) } doReturn true
        on { getCardReaderOnboardingStatus(any(), any(), any()) } doReturn
            CardReaderOnboardingStatus.CARD_READER_ONBOARDING_COMPLETED
        on { isPOSTabVisibleForSite(any()) } doReturn true
        on { isPOSLaunchableForSite(any()) } doReturn true
    }

    private val posLocalCatalogStore: WooPosLocalCatalogStore = mock {
        on { getProductCount(any()) } doReturn Result.success(1250)
        on { getVariationCount(any()) } doReturn Result.success(3420)
    }

    private val isLocalCatalogSupported: WooPosIsLocalCatalogSupported = mock {
        on { asOfLastEvaluation(any(), any()) } doReturn true
    }

    private val syncTimestampManager: WooPosSyncTimestampManager = mock {
        on { getFullSyncLastCompletedTimestamp() } doReturn FULL_SYNC_MILLIS
        on { getProductsLastSyncTimestamp() } doReturn PRODUCTS_SYNC_MILLIS
        on { getVariationsLastSyncTimestamp() } doReturn VARIATIONS_SYNC_MILLIS
        on { isCatalogFileBlocked() } doReturn false
    }

    private val posPreferencesRepository: WooPosPreferencesRepository = mock {
        on { allowCellularDataUpdate } doReturn flowOf(true)
        on { getLastUsedTimestamp() } doReturn POS_LAST_USED_MILLIS
    }

    private val accountStore: AccountStore = mock {
        on { account } doReturn AccountModel().apply { userId = 12345678L }
    }

    private val siteStore: SiteStore = mock {
        on { sites } doReturn listOf(
            wooSite("https://first.example.com"),
            wooSite("https://second.example.com"),
            wooSite("https://third.example.com")
        )
    }

    private val wooCommerceStore: WooCommerceStore = mock {
        on { getSitePlugins(any<SiteModel>()) } doReturn listOf(
            sitePlugin("woocommerce-payments/woocommerce-payments", isActive = true, version = "8.1.0")
        )
    }

    private val sut = MobileStatusProvider(
        context = mock(),
        envDataSource = envDataSource,
        deviceInfo = deviceInfo,
        localeProvider = localeProvider,
        featureFlagRepository = featureFlagRepository,
        notificationSystemStatusProvider = notificationSystemStatusProvider,
        notificationChannelsHandler = notificationChannelsHandler,
        getBackgroundRestrictions = getBackgroundRestrictions,
        deviceFeatures = deviceFeatures,
        getWooCorePluginCachedVersion = getWooCorePluginCachedVersion,
        appPrefs = appPrefs,
        accountStore = accountStore,
        siteStore = siteStore,
        wooCommerceStore = wooCommerceStore,
        posLocalCatalogStore = posLocalCatalogStore,
        syncTimestampManager = syncTimestampManager,
        posPreferencesRepository = posPreferencesRepository,
        isLocalCatalogSupported = isLocalCatalogSupported
    )

    /**
     * The whole report in one assertion. Field-by-field `contains` checks cannot see a field that silently
     * disappears, a section that lands in the wrong order, or a stray blank line; this does, and it doubles as
     * the readable example of what the report looks like. The tests below cover only what this fixture cannot
     * express — branches, and values it has no second combination for.
     */
    @Test
    fun `given a selected store, when the report is generated, then it matches the expected report`() =
        testBlocking {
            val report = sut(SiteModel().apply { url = "https://example.com" })

            assertThat(report.trimEnd()).isEqualTo(EXPECTED_REPORT)
        }

    @Test
    fun `given no selected store, when the report is generated, then the store sections are omitted`() =
        testBlocking {
            val report = sut(null)

            assertThat(report).endsWith("# No store selected\n")
            assertThat(report).doesNotContain("## Store Details")
            assertThat(report).doesNotContain("## Store Notifications")
            assertThat(report).doesNotContain("## Payments")
            assertThat(report).doesNotContain("## Point of Sale")
            // The app-wide half is still reported in full.
            assertThat(report).contains("## Device")
            assertThat(report).contains("Connected stores: 3")
        }

    @Test
    fun `given a store with no url, when the report is generated, then the band names it another way`() =
        testBlocking {
            val report = sut(SiteModel().apply { name = "Jirka's Store" })

            assertThat(report).contains("# Selected store: Jirka's Store")
        }

    @Test
    fun `given a section fails, when the report is generated, then only that section degrades`() = testBlocking {
        deviceInfo.stub { on { name } doThrow RuntimeException("boom") }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report.section("## Device")).isEqualTo("Info not found")
        assertThat(report).contains("Play Services: available")
        assertThat(report).contains("Product add-ons: true")
    }

    @Test
    fun `given a single field fails, when the report is generated, then the rest of its section survives`() =
        testBlocking {
            syncTimestampManager.stub {
                on { getProductsLastSyncTimestamp() } doThrow NumberFormatException("malformed")
            }

            val report = sut(SiteModel().apply { url = "https://example.com" })

            val pos = report.section("## Point of Sale")
            assertThat(pos).contains("Products timestamp: unknown")
            assertThat(pos).doesNotContain("Info not found")
            assertThat(pos).contains("Local catalog full sync: 2026-07-29T09:29:49Z")
        }

    /**
     * The golden report covers data saver on with the other two off. Three booleans cannot be told apart by a
     * single combination, so this pins the remaining two labels to their own fields.
     */
    @Test
    fun `given power save and background restriction are on, when the report is generated, then each is labelled`() =
        testBlocking {
            stubRestrictions(isPowerSaveModeEnabled = true, isBackgroundRestricted = true)

            val report = sut(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Background restricted: true")
            assertThat(report).contains("Power save mode: true")
            assertThat(report).contains("Data saver: false")
        }

    @Test
    fun `given non Woo sites on the account, when the report is generated, then only stores are reported`() =
        testBlocking {
            siteStore.stub {
                on { sites } doReturn listOf(
                    wooSite("https://store.example.com"),
                    SiteModel().apply { url = "https://personal-blog.example.com" }
                )
            }

            val report = sut(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Connected stores: 1")
            assertThat(report).contains("https://store.example.com")
            assertThat(report).doesNotContain("personal-blog")
        }

    @Test
    fun `given no push token, when the report is generated, then it is reported as missing`() = testBlocking {
        appPrefs.stub { on { getFCMToken() } doReturn "" }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report).contains("Push token: missing")
    }

    @Test
    fun `given a Jetpack connected store, when the report is generated, then the auth method says so`() =
        testBlocking {
            val site = SiteModel().apply {
                url = "https://example.com"
                origin = SiteModel.ORIGIN_WPCOM_REST
                setIsJetpackConnected(true)
            }

            val report = sut(site)

            assertThat(report).contains("Auth method: Jetpack")
        }

    /**
     * `connectionType` answers `Jetpack` for this site in production, which the report would state as fact.
     */
    @Test
    fun `given a store the app cannot classify, when the report is generated, then the auth method is unknown`() =
        testBlocking {
            val site = SiteModel().apply {
                url = "https://example.com"
                origin = SiteModel.ORIGIN_WPCOM_REST
            }

            val report = sut(site)

            assertThat(report).contains("Auth method: unknown")
        }

    @Test
    fun `given no remote flag values, when the report is generated, then they are reported as not loaded`() =
        testBlocking {
            featureFlagRepository.stub {
                on { getFlagState(any()) } doAnswer { invocation ->
                    FeatureFlagState(
                        flag = invocation.getArgument(0),
                        localValue = false,
                        remoteValue = null,
                        overrideValue = null
                    )
                }
            }

            val report = sut(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Remote values loaded: false")
            assertThat(report).contains("ai_support_chat: false (compiled-in default)")
        }

    @Test
    fun `given no plugins are cached, when the report is generated, then the plugins are unknown`() =
        testBlocking {
            wooCommerceStore.stub { on { getSitePlugins(any<SiteModel>()) } doReturn emptyList() }

            val report = sut(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Payment plugins: unknown")
        }

    @Test
    fun `given the app was installed from Play, when the report is generated, then the installer is reported`() =
        testBlocking {
            val report = providerWithInstaller { PLAY_STORE }(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Install source: $PLAY_STORE")
        }

    @Test
    fun `given no installer of record, when the report is generated, then the app is reported as sideloaded`() =
        testBlocking {
            val report = providerWithInstaller { null }(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Install source: sideloaded")
        }

    @Test
    fun `given the installer lookup fails, when the report is generated, then the source is unknown`() =
        testBlocking {
            val report = providerWithInstaller { throw IllegalArgumentException("nope") }(
                SiteModel().apply { url = "https://example.com" }
            )

            assertThat(report).contains("Install source: unknown")
        }

    @Test
    fun `given the local catalog is not supported, when the report is generated, then the strategy is remote`() =
        testBlocking {
            isLocalCatalogSupported.stub { on { asOfLastEvaluation(any(), any()) } doReturn false }

            val report = sut(SiteModel().apply { url = "https://example.com" })

            assertThat(report).contains("Catalog strategy: remote")
        }

    @Test
    fun `given POS cannot be launched, when the report is generated, then it says so`() = testBlocking {
        appPrefs.stub { on { isPOSLaunchableForSite(any()) } doReturn false }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report).contains("POS launchable: false")
    }

    @Test
    fun `given POS has never been opened, when the report is generated, then it says never`() = testBlocking {
        posPreferencesRepository.stub { on { getLastUsedTimestamp() } doReturn null }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report).contains("POS last opened: never")
    }

    @Test
    fun `given cellular full sync is off, when the report is generated, then it says so`() = testBlocking {
        posPreferencesRepository.stub { on { allowCellularDataUpdate } doReturn flowOf(false) }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report).contains("Full sync on cellular allowed: false")
    }

    @Test
    fun `given the catalog file is blocked, when the report is generated, then it says so`() = testBlocking {
        syncTimestampManager.stub { on { isCatalogFileBlocked() } doReturn true }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report).contains("Catalog file blocked: true")
    }

    @Test
    fun `given the catalog count cannot be read, when the report is generated, then it is unknown`() = testBlocking {
        posLocalCatalogStore.stub {
            on { getProductCount(any()) } doReturn Result.failure(RuntimeException("db gone"))
        }

        val report = sut(SiteModel().apply { url = "https://example.com" })

        assertThat(report).contains("Local catalog products: unknown")
        assertThat(report).contains("Local catalog variations: 3420")
    }

    /** The lines of one section, without its heading, so a section can be asserted on as a whole. */
    private fun String.section(heading: String) =
        substringAfter("$heading\n").substringBefore("\n\n").trim()

    /**
     * Unit tests run with `Build.VERSION.SDK_INT` of 0, so the pre-API-30 `getInstallerPackageName` branch is the
     * one exercised here.
     */
    @Suppress("DEPRECATION")
    private fun providerWithInstaller(installer: () -> String?): MobileStatusProvider {
        val packageManager = mock<PackageManager> {
            on { getInstallerPackageName(PACKAGE_NAME) } doAnswer { installer() }
        }
        return providerWith(
            mock<Context> {
                on { this.packageManager } doReturn packageManager
                on { packageName } doReturn PACKAGE_NAME
            }
        )
    }

    private fun providerWith(context: Context) = MobileStatusProvider(
        context = context,
        envDataSource = envDataSource,
        deviceInfo = deviceInfo,
        localeProvider = localeProvider,
        featureFlagRepository = featureFlagRepository,
        notificationSystemStatusProvider = notificationSystemStatusProvider,
        notificationChannelsHandler = notificationChannelsHandler,
        getBackgroundRestrictions = getBackgroundRestrictions,
        deviceFeatures = deviceFeatures,
        getWooCorePluginCachedVersion = getWooCorePluginCachedVersion,
        appPrefs = appPrefs,
        accountStore = accountStore,
        siteStore = siteStore,
        wooCommerceStore = wooCommerceStore,
        posLocalCatalogStore = posLocalCatalogStore,
        syncTimestampManager = syncTimestampManager,
        posPreferencesRepository = posPreferencesRepository,
        isLocalCatalogSupported = isLocalCatalogSupported
    )

    private fun sitePlugin(name: String, isActive: Boolean, version: String) = SitePluginModel(
        siteId = LocalId(1),
        name = name,
        version = version,
        slug = name.substringAfterLast('/'),
        authorName = "author",
        isActive = isActive
    )

    private fun wooSite(url: String = "") = SiteModel().apply {
        hasWooCommerce = true
        this.url = url
        planShortName = "Business"
        planId = 1008L
    }

    private fun stubRestrictions(
        isDataSaverEnabled: Boolean = false,
        isPowerSaveModeEnabled: Boolean = false,
        isBackgroundRestricted: Boolean = false
    ) = getBackgroundRestrictions.stub {
        on { invoke() } doReturn BackgroundRestrictions(
            isDataSaverEnabled = isDataSaverEnabled,
            isPowerSaveModeEnabled = isPowerSaveModeEnabled,
            isBackgroundRestricted = isBackgroundRestricted
        )
    }

    private companion object {
        // The flavour is whatever variant the tests run under - CI runs jalapeno, not wasabi.
        private val EXPECTED_REPORT = """
            ### Mobile Status Report generated via the WooCommerce Android app ###

            ## App
            Version: 21.3 (2103003)
            Build: ${BuildConfig.FLAVOR} / ${BuildConfig.BUILD_TYPE}
            Install source: unknown

            ## Device
            Model: Google Pixel 8
            OS: Android 15 (API 35)
            Free space: 12.4 GB
            Screen: 411x914 dp
            Device locale: en-US
            App language: en-GB

            ## Connectivity
            Network Type: WiFi
            Carrier: Test Carrier
            Country Code: GB

            ## Notifications
            Play Services: available
            Permission granted: true
            App notifications enabled: true
            Disabled channels: REVIEW
            New order sound: changed from the default
            Push token: present (…klmnop)
            Background restricted: false
            Power save mode: false
            Data saver: true

            ## Account & Stores
            WPCom user ID: 12345678
            Connected stores: 3

            All connected stores:
            https://first.example.com: <Type: (Self-hosted + Jetpack) Plan: Business (1008)>
            https://second.example.com: <Type: (Self-hosted + Jetpack) Plan: Business (1008)>
            https://third.example.com: <Type: (Self-hosted + Jetpack) Plan: Business (1008)>

            ## Feature Flags
            Remote values loaded: true
            age_eligibility_checks: true (remote)
            ai_support_chat: true (remote)
            better_customer_search_m2: true (remote)
            logged_out_ff_panel: true (remote)
            order_creation_auto_tax_rate: true (remote)
            pos_products_fts: true (remote)
            smarter_notifications: true (remote)
            wc_shipping_banner: true (remote)
            woo_app_passwords_for_jetpack_sites: true (remote)
            woo_ipp_australia_woopayments: true (remote)
            woo_ipp_country_expansion: true (remote)
            woo_ipp_country_expansion_eu_extended: true (remote)
            woo_mobile_ai_assistant: true (remote)
            woo_notification_1d_after_free_trial_expires: true (remote)
            woo_notification_1d_before_free_trial_expires: true (remote)
            woo_notification_store_creation_ready: true (remote)
            woo_pos_all_countries: true (remote)
            woo_pos_local_catalog_m1: true (remote)
            woo_pos_mark_order_as_complete: true (remote)
            woo_pos_phone: true (remote)
            woo_pos_refund_v4: true (remote)
            woo_pos_scan_to_pay: true (remote)
            woo_pos_tablet_promo_banner: true (remote)
            woo_pos_tap_to_pay: true (remote)
            woo_qr_code_login: true (remote)
            woo_self_driven_push_notifications_m1: true (remote)

            ## Experimental Features
            Product add-ons: true
            Jetpack app passwords: false
            POS local catalog: true

            # Selected store: https://example.com

            ## Store Details
            Blog ID: not set
            Store ID: store-uuid
            Auth method: ApplicationPasswords
            Site supports app passwords: false
            Jetpack: installed=false connected=false CP=false
            Plan: unknown (0)
            Woo core version: 10.9.2

            ## Payments
            WooPayments: active 8.1.0
            Stripe extension: not installed
            In-person payments plugin: WOOCOMMERCE_PAYMENTS 11.0.0
            In-person payments plugin chosen by merchant: true
            In-person payments onboarding: CARD_READER_ONBOARDING_COMPLETED

            ## Point of Sale
            POS tab visible: true
            POS launchable: true
            Catalog strategy: local catalog
            POS last opened: 2026-07-29T09:29:48Z
            Local catalog products: 1250
            Local catalog variations: 3420
            Local catalog full sync: 2026-07-29T09:29:49Z
            Products timestamp: 2026-07-29T09:29:50Z
            Variations timestamp: 2026-07-29T09:29:51Z
            Catalog file blocked: false
            Full sync on cellular allowed: true
        """.trimIndent().trim()

        const val PLAY_STORE = "com.android.vending"
        const val PACKAGE_NAME = "com.woocommerce.android"

        // 2026-07-29T09:29:49Z, :50Z and :51Z
        const val POS_LAST_USED_MILLIS = 1785317388000L
        const val FULL_SYNC_MILLIS = 1785317389000L
        const val PRODUCTS_SYNC_MILLIS = 1785317390000L
        const val VARIATIONS_SYNC_MILLIS = 1785317391000L
    }
}
