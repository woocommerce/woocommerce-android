package com.woocommerce.android.ui.orders.creation

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

@ExperimentalCoroutinesApi
class IsCurrencyQueryParamSupportedTest : BaseUnitTest() {
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val fetchWooVersion: FetchActiveWCPluginVersion = mock()

    private val sut = IsCurrencyQueryParamSupported(
        getWooCoreVersion = getWooCoreVersion,
        fetchWooVersion = fetchWooVersion,
    )

    @Test
    fun `given cached WC version below 11_1_0, when invoked, then returns false`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("11.0.9")

        assertThat(sut()).isFalse()
    }

    @Test
    fun `given cached WC version equal to 11_1_0, when invoked, then returns true`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("11.1.0")

        assertThat(sut()).isTrue()
    }

    @Test
    fun `given cached WC version above 11_1_0, when invoked, then returns true`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("11.2.0")

        assertThat(sut()).isTrue()
    }

    @Test
    fun `given cached WC version, when invoked, then the version is not fetched`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn("11.1.0")

        sut()

        verify(fetchWooVersion, never()).invoke()
    }

    @Test
    fun `given no cached WC version, when invoked, then falls back to the fetched version`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn("11.1.0")

        assertThat(sut()).isTrue()
    }

    @Test
    fun `given the WC version is unknown, when invoked, then returns false`() = testBlocking {
        whenever(getWooCoreVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn(null)

        assertThat(sut()).isFalse()
    }
}
