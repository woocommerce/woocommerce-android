package com.woocommerce.android.notifications.push

import android.content.SharedPreferences
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    private val selectedSite: SelectedSite = mock()
    private val sharedPreferences: SharedPreferences = mock()

    private lateinit var sut: PushNotificationRegistrationStatus

    @Before
    fun setUp() {
        whenever(prefsWrapper.getFluxCPreferences()).thenReturn(sharedPreferences)

        sut = PushNotificationRegistrationStatus(
            prefsWrapper,
            pushNotificationRepository,
            selectedSite
        )
    }

    @Test
    fun `given device id exists, when invoked, then returns REGISTERED`() = testBlocking {
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn("device-id-123")

        val result = sut()

        assertThat(result).isEqualTo(Status.REGISTERED)
    }

    @Test
    fun `given device id is null, when invoked, then returns UNREGISTERED`() = testBlocking {
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn(null)

        val result = sut()

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }

    @Test
    fun `given device id is empty, when invoked, then returns UNREGISTERED`() = testBlocking {
        whenever(sharedPreferences.getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null))
            .thenReturn("")

        val result = sut()

        assertThat(result).isEqualTo(Status.UNREGISTERED)
    }
}
