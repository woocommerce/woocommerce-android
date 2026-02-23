package com.woocommerce.android.ui.pushnotifications

import com.woocommerce.android.ui.pushnotifications.CheckWooPluginPushNotificationsSupport.Result
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CheckWooPluginPushNotificationsSupportTest : BaseUnitTest() {
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion = mock()

    private val sut = CheckWooPluginPushNotificationsSupport(fetchActiveWCPluginVersion)

    @Test
    fun `given compatible version, when invoked, then returns Compatible`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn("10.6.0")

        val result = sut()

        assertThat(result).isEqualTo(Result.Compatible)
    }

    @Test
    fun `given newer version, when invoked, then returns Compatible`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn("11.0.0")

        val result = sut()

        assertThat(result).isEqualTo(Result.Compatible)
    }

    @Test
    fun `given incompatible version, when invoked, then returns UpdateRequired with version`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn("9.0.0")

        val result = sut()

        assertThat(result).isEqualTo(Result.UpdateRequired(currentVersion = "9.0.0"))
    }

    @Test
    fun `given fetch fails, when invoked, then returns Error`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn(null)

        val result = sut()

        assertThat(result).isEqualTo(Result.Error)
    }
}
