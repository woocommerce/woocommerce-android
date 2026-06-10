package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosIsWooBelowCatalogFixVersionTest {
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val fetchWooVersion: FetchActiveWCPluginVersion = mock()

    @Rule
    @JvmField
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private lateinit var sut: WooPosIsWooBelowCatalogFixVersion

    @Before
    fun setup() {
        sut = WooPosIsWooBelowCatalogFixVersion(
            getWooCoreVersion = getWooCoreVersion,
            fetchWooVersion = fetchWooVersion,
        )
    }

    @Test
    fun `given cached WC version below 11_0_0, when invoked, then returns true`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.9.0")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given cached WC version equal to 11_0_0, when invoked, then returns false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("11.0.0")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given cached WC version above 11_0_0, when invoked, then returns false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("11.2.0")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given no cached version but fetched version below 11_0_0, when invoked, then returns true`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn("10.0.0")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given version is unknown, when invoked, then returns false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn(null)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }
}
