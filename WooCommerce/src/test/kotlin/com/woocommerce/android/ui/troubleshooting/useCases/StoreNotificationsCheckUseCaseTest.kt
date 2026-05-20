package com.woocommerce.android.ui.troubleshooting.useCases

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.notifications.push.RegisterDevice
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class StoreNotificationsCheckUseCaseTest : BaseUnitTest() {
    private lateinit var sut: StoreNotificationsCheckUseCase
    private lateinit var notificationSystemStatusProvider: NotificationSystemStatusProvider
    private lateinit var appPrefsWrapper: AppPrefsWrapper
    private lateinit var selectedSite: SelectedSite
    private lateinit var pushNotificationRegistrationStatus: PushNotificationRegistrationStatus
    private lateinit var registerDevice: RegisterDevice

    private val site = SiteModel().apply { siteId = TEST_SITE_ID }

    @Before
    fun setUp() {
        notificationSystemStatusProvider = mock()
        appPrefsWrapper = mock()
        selectedSite = mock {
            on { get() }.thenReturn(site)
        }
        pushNotificationRegistrationStatus = mock()
        registerDevice = mock()
        sut = StoreNotificationsCheckUseCase(
            notificationSystemStatusProvider = notificationSystemStatusProvider,
            appPrefsWrapper = appPrefsWrapper,
            selectedSite = selectedSite,
            pushNotificationRegistrationStatus = pushNotificationRegistrationStatus,
            registerDevice = registerDevice
        )
    }

    @Test
    fun `given notification permission is granted, when check runs, then emit success`() = testBlocking {
        whenever(notificationSystemStatusProvider.hasPostNotificationsPermission()).thenReturn(true)

        val stateEvents = sut.checkPermission().toList()

        assertThat(stateEvents[0]).isEqualTo(InProgress)
        assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
    }

    @Test
    fun `given notification permission is denied, when check runs, then emit permission failure`() = testBlocking {
        whenever(notificationSystemStatusProvider.hasPostNotificationsPermission()).thenReturn(false)

        val stateEvents = sut.checkPermission().toList()

        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).technicalDetails)
            .contains(StoreNotificationsCheckUseCase.ERROR_NOTIFICATION_PERMISSION_DENIED)
    }

    @Test
    fun `given app notifications are disabled, when check runs, then emit app notifications failure`() =
        testBlocking {
            whenever(notificationSystemStatusProvider.areAppNotificationsEnabled()).thenReturn(false)

            val stateEvents = sut.checkAppNotificationsEnabled().toList()

            assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
            assertThat((stateEvents[1] as Failure).technicalDetails)
                .contains(StoreNotificationsCheckUseCase.ERROR_APP_NOTIFICATIONS_DISABLED)
        }

    @Test
    fun `given a notification channel is disabled, when check runs, then emit channel failure`() = testBlocking {
        whenever(notificationSystemStatusProvider.disabledWooNotificationChannels())
            .thenReturn(listOf(NotificationChannelType.NEW_ORDER))

        val stateEvents = sut.checkNotificationChannelsEnabled().toList()

        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).technicalDetails)
            .contains(StoreNotificationsCheckUseCase.ERROR_NOTIFICATION_CHANNELS_DISABLED)
            .contains(NotificationChannelType.NEW_ORDER.name)
    }

    @Test
    fun `given fcm token is missing, when check runs, then emit token failure`() = testBlocking {
        whenever(appPrefsWrapper.getFCMToken()).thenReturn("")

        val stateEvents = sut.checkPushToken().toList()

        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).technicalDetails)
            .contains(StoreNotificationsCheckUseCase.ERROR_FCM_TOKEN_MISSING)
    }

    @Test
    fun `given push registration exists, when check runs, then emit success`() = testBlocking {
        whenever(pushNotificationRegistrationStatus(TEST_SITE_ID))
            .thenReturn(PushNotificationRegistrationStatus.Status.REGISTERED_WOO_ONLY)

        val stateEvents = sut.checkPushRegistration().toList()

        assertThat(stateEvents[1]).isInstanceOf(Success::class.java)
    }

    @Test
    fun `given push registration is missing, when check runs, then emit registration failure`() = testBlocking {
        whenever(pushNotificationRegistrationStatus(TEST_SITE_ID))
            .thenReturn(PushNotificationRegistrationStatus.Status.UNREGISTERED)

        val stateEvents = sut.checkPushRegistration().toList()

        assertThat(stateEvents[1]).isInstanceOf(Failure::class.java)
        assertThat((stateEvents[1] as Failure).technicalDetails)
            .contains(StoreNotificationsCheckUseCase.ERROR_PUSH_NOTIFICATIONS_UNREGISTERED)
    }

    @Test
    fun `given push registration succeeds, when registering push notifications, then return success`() =
        testBlocking {
            whenever(appPrefsWrapper.getFCMToken()).thenReturn("token")
            whenever(pushNotificationRegistrationStatus(TEST_SITE_ID))
                .thenReturn(PushNotificationRegistrationStatus.Status.REGISTERED_WOO_ONLY)

            val result = sut.registerPushNotifications()

            assertThat(result.isSuccess).isTrue()
            verify(registerDevice).invoke(RegisterDevice.Trigger.APP_FOREGROUND)
        }

    private companion object {
        const val TEST_SITE_ID = 123L
    }
}
