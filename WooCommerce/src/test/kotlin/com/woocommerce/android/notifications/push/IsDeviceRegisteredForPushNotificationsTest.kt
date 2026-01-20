package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import com.woocommerce.android.notifications.push.IsDeviceRegisteredForPushNotifications.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils

class IsDeviceRegisteredForPushNotificationsTest {
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper = mock()
    private val pushNotificationsStore: PushNotificationsStore = mock()
    private val sharedPreferences: SharedPreferences = mock()

    private lateinit var sut: IsDeviceRegisteredForPushNotifications

    @Before
    fun setUp() {
        whenever(prefsWrapper.getFluxCPreferences()).thenReturn(sharedPreferences)

        sut = IsDeviceRegisteredForPushNotifications(
            prefsWrapper,
            pushNotificationsStore
        )
    }

    @Test
    fun `given device id exists, when invoked, then returns REGISTERED`() {
        whenever(sharedPreferences.getString(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn("device-id-123")

        val result = sut()

        assertThat(result).isEqualTo(Status.REGISTERED)
    }

    @Test
    fun `given device id is null, when invoked, then returns UNREGISTERED`() {
        whenever(sharedPreferences.getString(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn(null)

        val result = sut()

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }

    @Test
    fun `given device id is empty, when invoked, then returns UNREGISTERED`() {
        whenever(sharedPreferences.getString(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn("")

        val result = sut()

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }
}
