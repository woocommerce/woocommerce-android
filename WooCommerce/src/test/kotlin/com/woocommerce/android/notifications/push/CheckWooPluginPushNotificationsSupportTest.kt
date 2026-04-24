package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.push.CheckWooPluginPushNotificationsSupport.Result
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CheckWooPluginPushNotificationsSupportTest : BaseUnitTest() {
    private val fetchActiveWCPluginVersion: FetchActiveWCPluginVersion = mock()
    private val getWooCorePluginCachedVersion: GetWooCorePluginCachedVersion = mock()
    private val minSupportedWooVersion = CheckWooPluginPushNotificationsSupport.PUSH_NOTIFICATIONS_MIN_WC_VERSION

    private val sut = CheckWooPluginPushNotificationsSupport(
        fetchActiveWCPluginVersion,
        getWooCorePluginCachedVersion
    )

    @Test
    fun `given compatible version, when invoked, then returns Compatible`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn(minSupportedWooVersion)

        val result = sut(forceRefresh = true)

        assertThat(result).isEqualTo(Result.Compatible)
    }

    @Test
    fun `given newer version, when invoked, then returns Compatible`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn("11.0.0")

        val result = sut(forceRefresh = true)

        assertThat(result).isEqualTo(Result.Compatible)
    }

    @Test
    fun `given incompatible version, when invoked, then returns UpdateRequired with version`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn("9.0.0")

        val result = sut(forceRefresh = true)

        assertThat(result).isEqualTo(Result.UpdateRequired(currentVersion = "9.0.0"))
    }

    @Test
    fun `given fetch fails, when invoked, then returns Error`() = testBlocking {
        whenever(fetchActiveWCPluginVersion()).thenReturn(null)

        val result = sut(forceRefresh = true)

        assertThat(result).isEqualTo(Result.Error)
    }

    @Test
    fun `given compatible cached version, when invoked with forceRefresh=false, then returns Compatible`() =
        testBlocking {
            whenever(getWooCorePluginCachedVersion()).thenReturn(minSupportedWooVersion)

            val result = sut(forceRefresh = false)

            assertThat(result).isEqualTo(Result.Compatible)
        }

    @Test
    fun `given incompatible cached version, when invoked with forceRefresh=false, then returns UpdateRequired`() =
        testBlocking {
            whenever(getWooCorePluginCachedVersion()).thenReturn("9.0.0")

            val result = sut(forceRefresh = false)

            assertThat(result).isEqualTo(Result.UpdateRequired(currentVersion = "9.0.0"))
        }

    @Test
    fun `given null cached version, when invoked with forceRefresh=false, then returns Error`() = testBlocking {
        whenever(getWooCorePluginCachedVersion()).thenReturn(null)

        val result = sut(forceRefresh = false)

        assertThat(result).isEqualTo(Result.Error)
    }

    @Test
    fun `given forceRefresh=false, when invoked, then does not call fetchActiveWCPluginVersion`() = testBlocking {
        whenever(getWooCorePluginCachedVersion()).thenReturn(minSupportedWooVersion)

        sut(forceRefresh = false)

        verify(fetchActiveWCPluginVersion, never()).invoke()
    }
}
