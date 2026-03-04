package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils

@ExperimentalCoroutinesApi
class PushNotificationRegistrationStatusTest : BaseUnitTest() {
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock()
    private val pushNotificationRepository: PushNotificationRepository = mock()
    private val sharedPreferences: SharedPreferences = mock()

    private lateinit var sut: PushNotificationRegistrationStatus

    @Before
    fun setUp() {
        whenever(prefsWrapper.getFluxCPreferences()).thenReturn(sharedPreferences)

        sut = PushNotificationRegistrationStatus(
            prefsWrapper,
            pushNotificationRepository
        )
    }

    @Test
    fun `given both Woo and WPCom registered, when invoked, then returns REGISTERED_IN_BOTH`() = testBlocking {
        setupWpComRegistration(isRegistered = true)
        setupWooRegistration(siteId = TEST_SITE_ID, isRegistered = true)

        val result = sut(TEST_SITE_ID)

        assertThat(result).isEqualTo(Status.REGISTERED_BOTH)
    }

    @Test
    fun `given only Woo registered, when invoked, then returns WOO_REGISTERED`() = testBlocking {
        setupWpComRegistration(isRegistered = false)
        setupWooRegistration(siteId = TEST_SITE_ID, isRegistered = true)

        val result = sut(TEST_SITE_ID)

        assertThat(result).isEqualTo(Status.REGISTERED_WOO_ONLY)
    }

    @Test
    fun `given only WPCom registered, when invoked, then returns WPCOM_REGISTERED`() = testBlocking {
        setupWpComRegistration(isRegistered = true)
        setupWooRegistration(siteId = TEST_SITE_ID, isRegistered = false)

        val result = sut(TEST_SITE_ID)

        assertThat(result).isEqualTo(Status.REGISTERED_WPCOM_ONLY)
    }

    @Test
    fun `given neither registered, when invoked, then returns UNREGISTERED`() = testBlocking {
        setupWpComRegistration(isRegistered = false)
        setupWooRegistration(siteId = TEST_SITE_ID, isRegistered = false)

        val result = sut(TEST_SITE_ID)

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }

    @Test
    fun `given null site id, when invoked, then returns status based on WPCom only`() = testBlocking {
        setupWpComRegistration(isRegistered = true)

        val result = sut(null)

        assertThat(result).isEqualTo(Status.REGISTERED_WPCOM_ONLY)
    }

    @Test
    fun `given null site id and no WPCom registration, when invoked, then returns UNREGISTERED`() = testBlocking {
        setupWpComRegistration(isRegistered = false)

        val result = sut(null)

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }

    @Test
    fun `given WPCom device id is empty string, when invoked, then treats as not registered`() = testBlocking {
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn("")
        setupWooRegistration(siteId = TEST_SITE_ID, isRegistered = false)

        val result = sut(TEST_SITE_ID)

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }

    private fun setupWpComRegistration(isRegistered: Boolean) {
        val deviceId = if (isRegistered) "device-id-123" else null
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn(deviceId)
    }

    private fun setupWooRegistration(siteId: Long, isRegistered: Boolean) {
        whenever(pushNotificationRepository.observeWooPushTokenRegisteredForSite(siteId))
            .thenReturn(flowOf(isRegistered))
    }

    companion object {
        private const val TEST_SITE_ID = 123L
    }
}
