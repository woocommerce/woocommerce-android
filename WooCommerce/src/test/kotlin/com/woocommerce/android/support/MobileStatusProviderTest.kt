package com.woocommerce.android.support

import android.content.Context
import android.content.pm.PackageManager
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.background.GetBackgroundRestrictions
import com.woocommerce.android.background.GetBackgroundRestrictions.BackgroundRestrictions
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.FeatureFlagRepository.FeatureFlagState
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
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

    private val appPrefs: AppPrefsWrapper = mock {
        on { getFCMToken() } doReturn "abcdefghijklmnop"
        on { isProductAddonsEnabled } doReturn true
        on { jetpackAppPasswordsEnabled } doReturn false
        on { wooPosLocalCatalogEnabled } doReturn true
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
        appPrefs = appPrefs,
        accountStore = accountStore,
        siteStore = siteStore
    )

    /**
     * The whole report in one assertion. Field-by-field `contains` checks cannot see a field that silently
     * disappears, a section that lands in the wrong order, or a stray blank line; this does, and it doubles as
     * the readable example of what the report looks like. The tests below cover only what this fixture cannot
     * express — branches, and values it has no second combination for.
     */
    @Test
    fun `when the report is generated, then it matches the expected report`() =
        testBlocking {
            val report = sut(SiteModel())

            assertThat(report.trimEnd()).isEqualTo(EXPECTED_REPORT)
        }

    @Test
    fun `given a section fails, when the report is generated, then only that section degrades`() = testBlocking {
        deviceInfo.stub { on { name } doThrow RuntimeException("boom") }

        val report = sut(SiteModel())

        assertThat(report.section("## Device")).isEqualTo("Info not found")
        assertThat(report).contains("Play Services: available")
        assertThat(report).contains("Product add-ons: true")
    }

    /**
     * The golden report covers data saver on with the other two off. Three booleans cannot be told apart by a
     * single combination, so this pins the remaining two labels to their own fields.
     */
    @Test
    fun `given power save and background restriction are on, when the report is generated, then each is labelled`() =
        testBlocking {
            stubRestrictions(isPowerSaveModeEnabled = true, isBackgroundRestricted = true)

            val report = sut(SiteModel())

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

            val report = sut(SiteModel())

            assertThat(report).contains("Connected stores: 1")
            assertThat(report).contains("https://store.example.com")
            assertThat(report).doesNotContain("personal-blog")
        }

    @Test
    fun `given no push token, when the report is generated, then it is reported as missing`() = testBlocking {
        appPrefs.stub { on { getFCMToken() } doReturn "" }

        val report = sut(SiteModel())

        assertThat(report).contains("Push token: missing")
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

            val report = sut(SiteModel())

            assertThat(report).contains("Remote values loaded: false")
            assertThat(report).contains("ai_support_chat: false (compiled-in default)")
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
        appPrefs = appPrefs,
        accountStore = accountStore,
        siteStore = siteStore
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
        private val EXPECTED_REPORT = """
            ### Mobile Status Report generated via the WooCommerce Android app ###

            ## App
            Version: 21.3 (2103003)
            Build: wasabi / debug
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
        """.trimIndent().trim()

        const val PLAY_STORE = "com.android.vending"
        const val PACKAGE_NAME = "com.woocommerce.android"
    }
}
