package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore

@ExperimentalCoroutinesApi
class RegisterDeviceTest : BaseUnitTest() {
    lateinit var sut: RegisterDevice

    private val appPrefs: AppPrefsWrapper = mock {
        on { getFCMToken() } doReturn TEST_TOKEN
    }
    private val accountStore: AccountStore = mock {
        on { hasAccessToken() } doReturn true
    }
    private val notificationRepository: NotificationRepository = mock()
    private val isDeviceRegisteredForPushNotifications: IsDeviceRegisteredForPushNotifications = mock {
        on { invoke() } doReturn IsDeviceRegisteredForPushNotifications.Status.UNREGISTERED
    }
    private val pushNotificationRepository: PushNotificationRepository = mock()

    @Before
    fun setUp() {
        sut = RegisterDevice(
            appPrefs,
            accountStore,
            notificationRepository,
            isDeviceRegisteredForPushNotifications,
            pushNotificationRepository
        )
    }

    @Test
    fun `do not register device if user is not logged in`() = testBlocking {
        // given
        whenever(accountStore.hasAccessToken()).thenReturn(false)

        // when
        sut.invoke(FORCEFULLY)

        // then
        verify(notificationRepository, never()).registerDevice(TEST_TOKEN)
    }

    @Test
    fun `do not register device if there's no messaging token`() = testBlocking {
        // given
        whenever(appPrefs.getFCMToken()).thenReturn("")

        // when
        sut.invoke(FORCEFULLY)

        // then
        verify(notificationRepository, never()).registerDevice(TEST_TOKEN)
    }

    @Test
    fun `register device if user is logged in and there's messaging token`() = testBlocking {
        // when
        sut.invoke(FORCEFULLY)

        // then
        verify(notificationRepository).registerDevice(TEST_TOKEN)
    }

    companion object {
        private const val TEST_TOKEN = "123"
    }
}
