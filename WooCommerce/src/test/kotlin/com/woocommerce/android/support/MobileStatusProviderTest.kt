package com.woocommerce.android.support

import android.content.Context
import android.content.pm.PackageManager
import com.woocommerce.android.AppPrefs.CardReaderOnboardingStatus
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.background.GetBackgroundRestrictions
import com.woocommerce.android.background.GetBackgroundRestrictions.BackgroundRestrictions
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.support.zendesk.MobileStatusProvider
import com.woocommerce.android.support.zendesk.ZendeskEnvironmentDataSource
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.troubleshooting.useCases.NotificationSystemStatusProvider
import com.woocommerce.android.ui.woopos.util.datastore.WooPosSyncTimestampManager
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.DeviceInfoWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.FeatureFlagRepository.FeatureFlagState
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.locale.LocaleProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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

    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus = mock {
        on { invoke(anyOrNull()) } doReturn PushNotificationRegistrationStatus.Status.REGISTERED_BOTH
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
        on { invoke() } doReturn "9.4.2"
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

    private val syncTimestampManager: WooPosSyncTimestampManager = mock {
        on { getFullSyncLastCompletedTimestamp() } doReturn FULL_SYNC_MILLIS
        on { getProductsLastSyncTimestamp() } doReturn PRODUCTS_SYNC_MILLIS
        on { getVariationsLastSyncTimestamp() } doReturn VARIATIONS_SYNC_MILLIS
    }

    private val accountStore: AccountStore = mock {
        on { account } doReturn AccountModel().apply { userId = 12345678L }
    }

    private val siteStore: SiteStore = mock {
        on { sites } doReturn listOf(SiteModel(), SiteModel(), SiteModel())
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
        pushNotificationRegistrationStatus = pushNotificationRegistrationStatus,
        getBackgroundRestrictions = getBackgroundRestrictions,
        deviceFeatures = deviceFeatures,
        getWooCorePluginCachedVersion = getWooCorePluginCachedVersion,
        appPrefs = appPrefs,
        accountStore = accountStore,
        siteStore = siteStore,
        wooCommerceStore = wooCommerceStore,
        syncTimestampManager = syncTimestampManager
    )

    @Test
    fun `when the report is generated, then it contains all the expected sections`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains(
            MobileStatusProvider.REPORT_HEADING,
            MobileStatusProvider.HEADING_APP,
            MobileStatusProvider.HEADING_DEVICE,
            MobileStatusProvider.HEADING_CONNECTIVITY,
            MobileStatusProvider.HEADING_NOTIFICATIONS,
            MobileStatusProvider.HEADING_ACCOUNT,
            MobileStatusProvider.HEADING_STORE,
            MobileStatusProvider.HEADING_PAYMENTS,
            MobileStatusProvider.HEADING_POS,
            MobileStatusProvider.HEADING_FEATURE_FLAGS,
            MobileStatusProvider.HEADING_EXPERIMENTAL
        )
    }

    @Test
    fun `when the report is generated, then every section heading states its scope`() = testBlocking {
        val report = sut(SiteModel().apply { url = "https://selected.com" })

        val headings = report.lines().filter { it.startsWith("## ") }
        assertThat(headings).isNotEmpty
        assertThat(headings).allMatch {
            it.endsWith(MobileStatusProvider.SCOPE_APP_WIDE) || it.endsWith("(selected store: https://selected.com)")
        }
    }

    @Test
    fun `when the report is generated, then store scoped sections name the selected store`() = testBlocking {
        val report = sut(SiteModel().apply { url = "https://selected.com" })

        assertThat(report).contains("${MobileStatusProvider.HEADING_STORE} (selected store: https://selected.com)")
        assertThat(report).contains("${MobileStatusProvider.HEADING_PAYMENTS} (selected store: https://selected.com)")
        assertThat(report).contains("${MobileStatusProvider.HEADING_POS} (selected store: https://selected.com)")
    }

    @Test
    fun `given no selected site, when the report is generated, then store scoped headings say so`() = testBlocking {
        val report = sut(null)

        assertThat(report).contains("${MobileStatusProvider.HEADING_STORE} ${MobileStatusProvider.SCOPE_NO_STORE}")
        assertThat(report).contains("${MobileStatusProvider.HEADING_PAYMENTS} ${MobileStatusProvider.SCOPE_NO_STORE}")
        assertThat(report).contains("${MobileStatusProvider.HEADING_POS} ${MobileStatusProvider.SCOPE_NO_STORE}")
    }

    @Test
    fun `when the report is generated, then the scope legend is included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains(MobileStatusProvider.SCOPE_LEGEND)
    }

    @Test
    fun `when the report is generated, then it links to the field reference`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains(MobileStatusProvider.FIELD_REFERENCE)
    }

    @Test
    fun `when the report is generated, then device and OS information is included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Model: Google Pixel 8")
        assertThat(report).contains("OS: Android 15 (API 35)")
        assertThat(report).contains("Screen: 411x914 dp")
        assertThat(report).contains("Version: 21.3 (2103003)")
    }

    @Test
    fun `given no installer of record, when the report is generated, then the app is reported as sideloaded`() =
        testBlocking {
            // A gradle or adb install has no installer of record, which is not the same as being unable to look it up
            val report = providerWithInstaller { null }(SiteModel())

            assertThat(report).contains("Install source: sideloaded (installed outside an app store, not from Play)")
        }

    @Test
    fun `given an installer of record, when the report is generated, then it is reported`() = testBlocking {
        val report = providerWithInstaller { PLAY_STORE }(SiteModel())

        assertThat(report).contains("Install source: $PLAY_STORE")
    }

    @Test
    fun `given the installer lookup fails, when the report is generated, then the source is unknown`() = testBlocking {
        val report = providerWithInstaller { throw IllegalArgumentException() }(SiteModel())

        assertThat(report).contains("Install source: unknown")
    }

    @Test
    fun `when the report is generated, then the device locale and app language are reported separately`() =
        testBlocking {
            val report = sut(SiteModel())

            assertThat(report).contains("Device locale: en-US")
            assertThat(report).contains("App language: en-GB")
        }

    @Test
    fun `when the report is generated, then network information is split into separate lines`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Network Type: WiFi")
        assertThat(report).contains("Carrier: Test Carrier")
        assertThat(report).contains("Country Code: GB")
    }

    @Test
    fun `when the report is generated, then notification state is included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Play Services: available")
        assertThat(report).contains("Permission granted: true")
        assertThat(report).contains("Disabled channels: REVIEW")
        assertThat(report).contains("New order sound: changed from the default")
        assertThat(report).contains("Data saver: true")
    }

    @Test
    fun `when the report is generated, then push registration is reported with the store it belongs to`() =
        testBlocking {
            val report = sut(SiteModel().apply { url = "https://selected.com" })

            val storeSection = report.substringAfter(MobileStatusProvider.HEADING_STORE).substringBefore("\n## ")
            assertThat(storeSection).contains("Push registration: REGISTERED_BOTH")
            val notificationsSection = report
                .substringAfter(MobileStatusProvider.HEADING_NOTIFICATIONS)
                .substringBefore("\n## ")
            assertThat(notificationsSection).doesNotContain("Push registration")
        }

    @Test
    fun `when the report is generated, then the FCM token is redacted to its last characters`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("FCM token: present (…klmnop)")
        assertThat(report).doesNotContain("abcdefghijklmnop")
    }

    @Test
    fun `given no FCM token, when the report is generated, then the token is reported as missing`() = testBlocking {
        appPrefs.stub { on { getFCMToken() } doReturn "" }

        val report = sut(SiteModel())

        assertThat(report).contains("FCM token: missing")
    }

    @Test
    fun `given a site with a blog id, when the report is generated, then both blog id and store id are included`() =
        testBlocking {
            val site = SiteModel().apply {
                siteId = 987654321L
                url = "https://example.com"
            }

            val report = sut(site)

            assertThat(report).contains("Blog ID: 987654321")
            assertThat(report).contains("Store ID: store-uuid")
            assertThat(report).contains("Woo core version: 9.4.2")
        }

    @Test
    fun `given an application password site, when the report is generated, then blog id is reported as not set`() =
        testBlocking {
            // Application password sites never get a blog id assigned, so it stays 0
            val site = SiteModel().apply {
                siteId = 0L
                url = "https://example.com"
            }

            val report = sut(site)

            assertThat(report)
                .contains("Blog ID: not set (stores connected with application passwords do not have one)")
            assertThat(report).contains("Store ID: store-uuid")
        }

    @Test
    fun `when the report is generated, then the payment plugin state is included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("WooPayments: active 8.1.0")
        assertThat(report).contains("Stripe extension: not installed")
    }

    @Test
    fun `given an inactive plugin, when the report is generated, then it is reported as installed but not active`() =
        testBlocking {
            wooCommerceStore.stub {
                on { getSitePlugins(any<SiteModel>()) } doReturn listOf(
                    sitePlugin(
                        "woocommerce-gateway-stripe/woocommerce-gateway-stripe",
                        isActive = false,
                        version = "9.0.1"
                    )
                )
            }

            val report = sut(SiteModel())

            assertThat(report).contains("Stripe extension: installed, not active 9.0.1")
            assertThat(report).contains("WooPayments: not installed")
        }

    @Test
    fun `given no cached plugins, when the report is generated, then plugin state is not claimed to be missing`() =
        testBlocking {
            wooCommerceStore.stub { on { getSitePlugins(any<SiteModel>()) } doReturn emptyList() }

            val report = sut(SiteModel())

            assertThat(report).contains("Payment plugins: unknown (none cached for this site)")
            assertThat(report).doesNotContain("WooPayments: not installed")
        }

    @Test
    fun `when the report is generated, then the in-person payments plugin in use is included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("In-person payments plugin: WOOCOMMERCE_PAYMENTS 11.0.0")
        assertThat(report).contains("In-person payments plugin chosen by merchant: true")
        assertThat(report).contains("In-person payments onboarding: CARD_READER_ONBOARDING_COMPLETED")
    }

    @Test
    fun `given no preferred plugin, when the report is generated, then it is reported as not set`() = testBlocking {
        appPrefs.stub { on { getCardReaderPreferredPlugin(any(), any(), any()) } doReturn null }

        val report = sut(SiteModel())

        assertThat(report).contains("In-person payments plugin: not set")
    }

    @Test
    fun `given no cached plugins, when the report is generated, then in-person payments state is still included`() = testBlocking {
        wooCommerceStore.stub { on { getSitePlugins(any<SiteModel>()) } doReturn emptyList() }

        val report = sut(SiteModel())

        assertThat(report).contains("In-person payments plugin: WOOCOMMERCE_PAYMENTS 11.0.0")
    }

    @Test
    fun `when the report is generated, then POS availability and local catalog sync times are included`() =
        testBlocking {
            val report = sut(SiteModel())

            assertThat(report).contains("POS tab visible: true")
            assertThat(report).contains("POS launchable: true")
            assertThat(report).contains("Local catalog full sync: 2026-07-29T09:29:49Z")
            assertThat(report).contains("Local catalog products sync: 2026-07-29T09:29:50Z")
            assertThat(report).contains("Local catalog variations sync: 2026-07-29T09:29:51Z")
        }

    @Test
    fun `given the catalog was never synced, when the report is generated, then it says never`() = testBlocking {
        syncTimestampManager.stub {
            on { getFullSyncLastCompletedTimestamp() } doReturn null
            on { getProductsLastSyncTimestamp() } doReturn null
            on { getVariationsLastSyncTimestamp() } doReturn null
        }

        val report = sut(SiteModel())

        assertThat(report).contains("Local catalog full sync: never")
        assertThat(report).contains("Local catalog products sync: never")
        assertThat(report).contains("Local catalog variations sync: never")
    }

    @Test
    fun `given POS is not visible, when the report is generated, then it points at the log for the reason`() =
        testBlocking {
            appPrefs.stub { on { isPOSTabVisibleForSite(any()) } doReturn false }

            val report = sut(SiteModel())

            assertThat(report).contains("POS Tab Not visible reason")
            assertThat(report).contains("POS cannot be launched")
        }

    @Test
    fun `given POS is available, when the report is generated, then the log pointer is omitted`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).doesNotContain("POS Tab Not visible reason")
    }

    @Test
    fun `given no selected site, when the report is generated, then the POS section degrades`() = testBlocking {
        val report = sut(null)

        assertThat(report).contains("Not applicable while no store is selected")
        assertThat(report).doesNotContain("POS tab visible")
    }

    @Test
    fun `when the report is generated, then every connected site is listed`() = testBlocking {
        siteStore.stub {
            on { sites } doReturn listOf(
                SiteModel().apply { url = "https://first.com" },
                SiteModel().apply { url = "https://second.com" }
            )
        }

        val report = sut(SiteModel())

        assertThat(report).contains("All connected sites:")
        assertThat(report).contains("https://first.com")
        assertThat(report).contains("https://second.com")
    }

    @Test
    fun `given a site address from the form, when the report is generated, then it is included`() = testBlocking {
        val report = sut(SiteModel(), siteAddress = "https://typed-by-merchant.com")

        assertThat(report).contains("Address given in the form: https://typed-by-merchant.com")
    }

    @Test
    fun `given no site address, when the report is generated, then the line is omitted`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).doesNotContain("Address given in the form")
    }

    @Test
    fun `given no selected site, when the report is generated, then it is still produced`() = testBlocking {
        val report = sut(null)

        assertThat(report).contains(MobileStatusProvider.SCOPE_NO_STORE)
        assertThat(report).contains("Connected stores: 3")
        assertThat(report).contains(MobileStatusProvider.HEADING_DEVICE)
    }

    @Test
    fun `given a logged out user, when the report is generated, then the user is reported as not logged in`() =
        testBlocking {
            accountStore.stub { on { account } doReturn AccountModel() }

            val report = sut(null)

            assertThat(report).contains("WPCom user ID: not logged in")
        }

    @Test
    fun `when the report is generated, then every feature flag is included with its effective value`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Remote values loaded: true")
        FeatureFlag.entries.forEach { flag ->
            assertThat(report).contains("${flag.remoteFlagKey}: true (remote)")
        }
    }

    @Test
    fun `given a flag falls back to its compiled-in value, when the report is generated, then the source says so`() =
        testBlocking {
            val flag = FeatureFlag.entries.first()
            featureFlagRepository.stub {
                on { getFlagState(flag) } doReturn FeatureFlagRepository.FeatureFlagState(
                    flag = flag,
                    localValue = true,
                    remoteValue = null,
                    overrideValue = null
                )
            }

            val report = sut(SiteModel())

            assertThat(report).contains("${flag.remoteFlagKey}: true (compiled-in default)")
        }

    @Test
    fun `given no remote flag values, when the report is generated, then remote values are reported as not loaded`() =
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

            assertThat(report)
                .contains("Remote values loaded: false (no remote fetch has ever succeeded on this install)")
        }

    @Test
    fun `when the report is generated, then experimental feature toggles are included`() = testBlocking {
        val report = sut(SiteModel())

        assertThat(report).contains("Product add-ons: true")
        assertThat(report).contains("Jetpack app passwords: false")
        assertThat(report).contains("POS local catalog: true")
    }

    @Test
    fun `given a section fails, when the report is generated, then the other sections are still produced`() =
        testBlocking {
            deviceInfo.stub { on { name } doThrow RuntimeException("boom") }

            val report = sut(SiteModel())

            assertThat(report).contains(MobileStatusProvider.SECTION_UNAVAILABLE)
            assertThat(report).contains("Play Services: available")
            assertThat(report).contains("Product add-ons: true")
        }

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
        pushNotificationRegistrationStatus = pushNotificationRegistrationStatus,
        getBackgroundRestrictions = getBackgroundRestrictions,
        deviceFeatures = deviceFeatures,
        getWooCorePluginCachedVersion = getWooCorePluginCachedVersion,
        appPrefs = appPrefs,
        accountStore = accountStore,
        siteStore = siteStore,
        wooCommerceStore = wooCommerceStore,
        syncTimestampManager = syncTimestampManager
    )

    private companion object {
        const val PLAY_STORE = "com.android.vending"
        const val PACKAGE_NAME = "com.woocommerce.android"

        // 2026-07-29T09:29:49Z, :50Z and :51Z
        const val FULL_SYNC_MILLIS = 1785317389000L
        const val PRODUCTS_SYNC_MILLIS = 1785317390000L
        const val VARIATIONS_SYNC_MILLIS = 1785317391000L
    }

    private fun sitePlugin(name: String, isActive: Boolean, version: String) = SitePluginModel(
        siteId = LocalId(1),
        name = name,
        version = version,
        slug = name.substringAfterLast('/'),
        authorName = "author",
        isActive = isActive
    )
}
