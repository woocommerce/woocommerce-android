package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.AccountStore

@ExperimentalCoroutinesApi
class RegisterDeviceTest : BaseUnitTest() {
    private lateinit var sut: RegisterDevice

    private val appPrefs: AppPrefsWrapper = mock()
    private val accountStore: AccountStore = mock()
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()

    @Before
    fun setUp() {
        setupDefaultMocks()
        createSut()
    }

    private fun setupDefaultMocks() {
        whenever(appPrefs.getFCMToken()).thenReturn(TEST_TOKEN)
        whenever(accountStore.hasAccessToken()).thenReturn(true)
    }

    private fun createSut() {
        sut = RegisterDevice(
            appPrefs,
            accountStore,
            pushNotificationRegistrationStatus,
            pushNotificationRepository
        )
    }

    @Test
    fun `given no FCM token, when invoked forcefully, then does not register`() = testBlocking {
        whenever(appPrefs.getFCMToken()).thenReturn("")
        setupStatus(Status.UNREGISTERED)

        sut(FORCEFULLY)

        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(TEST_TOKEN)
    }

    @Test
    fun `given no FCM token, when invoked if needed, then does not register`() = testBlocking {
        whenever(appPrefs.getFCMToken()).thenReturn("")
        setupStatus(Status.UNREGISTERED)

        sut(IF_NEEDED)

        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(TEST_TOKEN)
    }

    @Test
    fun `given user not logged in, when invoked forcefully, then does not register in WPCom`() = testBlocking {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        setupStatus(Status.UNREGISTERED)

        sut(FORCEFULLY)

        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given user logged in and UNREGISTERED, when invoked forcefully, then registers in WPCom`() = testBlocking {
        setupStatus(Status.UNREGISTERED)

        sut(FORCEFULLY)

        verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given user logged in and WOO_REGISTERED, when invoked forcefully, then registers in WPCom`() = testBlocking {
        setupStatus(Status.WOO_REGISTERED)

        sut(FORCEFULLY)

        verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given user logged in and WPCOM_REGISTERED, when invoked forcefully, then registers in WPCom`() = testBlocking {
        setupStatus(Status.WPCOM_REGISTERED)

        sut(FORCEFULLY)

        verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given REGISTERED_IN_BOTH, when invoked forcefully, then registers in WPCom`() = testBlocking {
        setupStatus(Status.REGISTERED_IN_BOTH)

        sut(FORCEFULLY)

        verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given UNREGISTERED, when invoked if needed, then registers in WPCom`() = testBlocking {
        setupStatus(Status.UNREGISTERED)

        sut(IF_NEEDED)

        verify(pushNotificationRepository).registerPushTokenInWpComSystem(TEST_TOKEN)
    }

    @Test
    fun `given WPCOM_REGISTERED, when invoked if needed, then does not register`() = testBlocking {
        setupStatus(Status.WPCOM_REGISTERED)

        sut(IF_NEEDED)

        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(TEST_TOKEN)
    }

    @Test
    fun `given WOO_REGISTERED, when invoked if needed, then does not register`() = testBlocking {
        setupStatus(Status.WOO_REGISTERED)

        sut(IF_NEEDED)

        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(TEST_TOKEN)
    }

    @Test
    fun `given REGISTERED_IN_BOTH, when invoked if needed, then does not register`() = testBlocking {
        setupStatus(Status.REGISTERED_IN_BOTH)

        sut(IF_NEEDED)

        verify(pushNotificationRepository, never()).registerPushTokenInWpComSystem(TEST_TOKEN)
        verify(pushNotificationRepository, never()).registerPushTokenInWooCoreSystem(TEST_TOKEN)
    }

    private suspend fun setupStatus(status: Status) {
        whenever(pushNotificationRegistrationStatus()).thenReturn(status)
    }

    companion object {
        private const val TEST_TOKEN = "test-fcm-token-123"
    }
}
