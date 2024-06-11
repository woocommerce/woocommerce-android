package com.woocommerce.android.ui.prefs

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.NotificationChannelsHandler
import com.woocommerce.android.notifications.NotificationChannelsHandler.NewOrderNotificationSoundStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore

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

    private val view: MainSettingsContract.View = mock()
    private lateinit var presenter: MainSettingsPresenter

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        presenter = MainSettingsPresenter(
            selectedSite = selectedSite,
            accountStore = accountStore,
            wooCommerceStore = wooCommerceStore,
            featureAnnouncementRepository = featureAnnouncementRepository,
            buildConfigWrapper = buildConfigWrapper,
            accountRepository = accountRepository,
            notificationChannelsHandler = notificationChannelsHandler,
            analyticsTracker = analyticsTracker,
            getWooVersion = getWooVersion,
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

            verify(view).showNotificationsSettingsScreen()
        }

    @Test
    fun `given order notification sound modified, when notifications button clicked, then open notifications settings`() =
        testBlocking {
            setup {
                whenever(notificationChannelsHandler.checkNewOrderNotificationSound())
                    .thenReturn(NewOrderNotificationSoundStatus.SOUND_MODIFIED)
            }

            presenter.onNotificationsClicked()

            verify(view).showNotificationsSettingsScreen()
        }
}
