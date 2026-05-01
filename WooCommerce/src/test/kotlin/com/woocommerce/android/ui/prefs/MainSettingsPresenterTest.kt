package com.woocommerce.android.ui.prefs

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.notifications.push.PushNotificationRepository
import com.woocommerce.android.notifications.push.ShouldShowEnablePushNotificationsUi
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainSettingsPresenterTest : BaseUnitTest() {
    private val accountRepository: AccountRepository = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val featureAnnouncementRepository: FeatureAnnouncementRepository = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val accountStore: AccountStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val notificationChannelsHandler: NotificationChannelsHandler = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val getWooVersion: GetWooCorePluginCachedVersion = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock()
    private val shouldShowEnablePushNotificationsUi: ShouldShowEnablePushNotificationsUi = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()

    private val view: MainSettingsContract.View = mock()
    private lateinit var presenter: MainSettingsPresenter

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        presenter = MainSettingsPresenter(
            selectedSite = selectedSite,
            accountStore = accountStore,
            wooCommerceStore = wooCommerceStore,
            featureAnnouncementRepository = featureAnnouncementRepository,
            shouldShowEnablePushNotificationsUi = shouldShowEnablePushNotificationsUi,
            buildConfigWrapper = buildConfigWrapper,
            accountRepository = accountRepository,
            notificationChannelsHandler = notificationChannelsHandler,
            analyticsTracker = analyticsTracker,
            getWooVersion = getWooVersion,
            appPrefs = appPrefs,
            ciabSiteGateKeeper = ciabSiteGateKeeper,
            featureFlagRepository = featureFlagRepository,
            pushNotificationRepository = pushNotificationRepository
        )
        presenter.takeView(view)
    }

    @Test
    fun `given cha-ching sound enabled, when notifications button clicked, then open device notification settings`() =
        testBlocking {
            setup {
                whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
                    .thenReturn(NewOrderNotificationSoundStatus.DEFAULT)
            }

            presenter.onNotificationsClicked()

            verify(view).showDeviceAppNotificationSettings()
            verify(analyticsTracker).track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED)
        }

    @Test
    fun `given cha-ching sound disabled, when notifications button clicked, then open notifications settings`() =
        testBlocking {
            setup {
                whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
                    .thenReturn(NewOrderNotificationSoundStatus.DISABLED)
            }

            presenter.onNotificationsClicked()

            verify(view).showNotificationsSettingsScreen(showSmarterNotifications = false)
        }

    @Test
    fun `given order notification sound modified, when notifications button clicked, then open notifications settings`() =
        testBlocking {
            setup {
                whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
                    .thenReturn(NewOrderNotificationSoundStatus.SOUND_MODIFIED)
            }

            presenter.onNotificationsClicked()

            verify(view).showNotificationsSettingsScreen(showSmarterNotifications = false)
        }

    @Test
    fun `given smarter notifications enabled for Woo-driven site, when notifications button clicked, then open smarter settings`() =
        testBlocking {
            setup {
                val site = mock<SiteModel> { on { siteId } doReturn SITE_ID }
                whenever(featureFlagRepository.isEnabled(FeatureFlag.SMARTER_NOTIFICATIONS)).thenReturn(true)
                whenever(selectedSite.getIfExists()).thenReturn(site)
                whenever(pushNotificationRepository.isWooPushTokenRegisteredForSite(SITE_ID)).thenReturn(true)
            }

            presenter.setupNotificationsOption()
            advanceUntilIdle()
            presenter.onNotificationsClicked()

            verify(view).showNotificationsSettingsScreen(showSmarterNotifications = true)
        }

    @Test
    fun `given smarter notifications enabled for non-Woo-driven site, when notifications button clicked, then open device notification settings`() =
        testBlocking {
            setup {
                val site = mock<SiteModel> { on { siteId } doReturn SITE_ID }
                whenever(featureFlagRepository.isEnabled(FeatureFlag.SMARTER_NOTIFICATIONS)).thenReturn(true)
                whenever(selectedSite.getIfExists()).thenReturn(site)
                whenever(pushNotificationRepository.isWooPushTokenRegisteredForSite(SITE_ID)).thenReturn(false)
                whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
                    .thenReturn(NewOrderNotificationSoundStatus.DEFAULT)
            }

            presenter.setupNotificationsOption()
            advanceUntilIdle()
            presenter.onNotificationsClicked()

            verify(view).showDeviceAppNotificationSettings()
            verify(analyticsTracker).track(AnalyticsEvent.SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED)
        }

    @Test
    fun `given smarter notifications enabled for non-Woo-driven site with modified sound, when notifications button clicked, then open notifications settings`() =
        testBlocking {
            setup {
                val site = mock<SiteModel> { on { siteId } doReturn SITE_ID }
                whenever(featureFlagRepository.isEnabled(FeatureFlag.SMARTER_NOTIFICATIONS)).thenReturn(true)
                whenever(selectedSite.getIfExists()).thenReturn(site)
                whenever(pushNotificationRepository.isWooPushTokenRegisteredForSite(SITE_ID)).thenReturn(false)
                whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
                    .thenReturn(NewOrderNotificationSoundStatus.SOUND_MODIFIED)
            }

            presenter.setupNotificationsOption()
            advanceUntilIdle()
            presenter.onNotificationsClicked()

            verify(view).showNotificationsSettingsScreen(showSmarterNotifications = false)
        }

    @Test
    fun `given smarter notifications enabled for Woo-driven site, when settings shown, then show smarter option`() =
        testBlocking {
            setup {
                val site = mock<SiteModel> { on { siteId } doReturn SITE_ID }
                whenever(featureFlagRepository.isEnabled(FeatureFlag.SMARTER_NOTIFICATIONS)).thenReturn(true)
                whenever(selectedSite.getIfExists()).thenReturn(site)
                whenever(pushNotificationRepository.isWooPushTokenRegisteredForSite(SITE_ID)).thenReturn(true)
            }

            presenter.setupNotificationsOption()
            advanceUntilIdle()

            verify(view).handleNotificationsOption(showSmarterNotifications = true)
        }

    @Test
    fun `given smarter notifications enabled for non-Woo-driven site, when settings shown, then show default option`() =
        testBlocking {
            setup {
                val site = mock<SiteModel> { on { siteId } doReturn SITE_ID }
                whenever(featureFlagRepository.isEnabled(FeatureFlag.SMARTER_NOTIFICATIONS)).thenReturn(true)
                whenever(selectedSite.getIfExists()).thenReturn(site)
                whenever(pushNotificationRepository.isWooPushTokenRegisteredForSite(SITE_ID)).thenReturn(false)
            }

            presenter.setupNotificationsOption()
            advanceUntilIdle()

            verify(view).handleNotificationsOption(showSmarterNotifications = false)
        }

    @Test
    fun `given WPCom suspended website using app passwords, when settings shown, then hide jeptack installation`() = testBlocking {
        setup {
            whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
            whenever(appPrefs.isSiteWPComSuspended).thenReturn(true)
        }

        presenter.setupJetpackInstallOption()

        verify(view).handleJetpackInstallOption(supportsJetpackInstallation = false)
    }

    @Test
    fun `given push notifications option should be shown, when setup is called, then show option`() = testBlocking {
        val shouldShowPushNotificationOption = MutableStateFlow(true)
        setup {
            whenever(shouldShowEnablePushNotificationsUi()).thenReturn(shouldShowPushNotificationOption)
        }

        presenter.setupEnablePushNotificationsOption()
        advanceUntilIdle()

        verify(view).setEnablePushNotificationsOptionVisible(true)
    }

    @Test
    fun `given push notifications option should be hidden, when setup is called, then hide option`() = testBlocking {
        val shouldShowPushNotificationOption = MutableStateFlow(false)
        setup {
            whenever(shouldShowEnablePushNotificationsUi()).thenReturn(shouldShowPushNotificationOption)
        }

        presenter.setupEnablePushNotificationsOption()
        advanceUntilIdle()

        verify(view).setEnablePushNotificationsOptionVisible(false)
    }

    @Test
    fun `given push notifications option visibility changes, when setup is called, then update option reactively`() =
        testBlocking {
            val shouldShowPushNotificationOption = MutableStateFlow(true)
            setup {
                whenever(shouldShowEnablePushNotificationsUi()).thenReturn(shouldShowPushNotificationOption)
            }

            presenter.setupEnablePushNotificationsOption()
            advanceUntilIdle()

            shouldShowPushNotificationOption.value = false
            advanceUntilIdle()

            verify(view, times(1)).setEnablePushNotificationsOptionVisible(true)
            verify(view, times(1)).setEnablePushNotificationsOptionVisible(false)
        }

    @Test
    fun `given CIAB site, when checking plugins section visibility, then plugins section is hidden`() =
        testBlocking {
            setup {
                whenever(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.Plugins))
                    .thenReturn(false)
            }

            assertFalse(presenter.isPluginsSectionVisible)
        }

    @Test
    fun `given non-CIAB site, when checking plugins section visibility, then plugins section is visible`() =
        testBlocking {
            setup {
                whenever(ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.Plugins))
                    .thenReturn(true)
            }

            assertTrue(presenter.isPluginsSectionVisible)
        }

    @Test
    fun `given push notifications option setup, when view is dropped, then flow collection is cancelled`() = testBlocking {
        val shouldShowPushNotificationOption = MutableSharedFlow<Boolean>(replay = 1)
        setup {
            whenever(shouldShowEnablePushNotificationsUi()).thenReturn(shouldShowPushNotificationOption)
        }

        presenter.setupEnablePushNotificationsOption()
        advanceUntilIdle()
        assertEquals(1, shouldShowPushNotificationOption.subscriptionCount.value)

        presenter.dropView()
        advanceUntilIdle()
        assertThat(shouldShowPushNotificationOption.subscriptionCount.value).isEqualTo(0)
    }

    companion object {
        private const val SITE_ID = 123L
    }
}
