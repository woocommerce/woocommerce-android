package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosIsLocalCatalogSupportedTest : BaseUnitTest() {

    private var featureFlagM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private var getWooVersion: GetWooCorePluginCachedVersion = mock()
    private var fetchWooVersion: FetchActiveWCPluginVersion = mock()
    private var logger: WooPosLogWrapper = mock()
    private var posTabShouldBeVisible: WooPosTabShouldBeVisible = mock()
    private var posCanBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()

    private lateinit var isLocalCatalogSupported: WooPosIsLocalCatalogSupported

    @Before
    fun setup() = testBlocking {
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(getWooVersion()).thenReturn("10.5.0")
        whenever(posTabShouldBeVisible.invoke(false)).thenReturn(Result.success(true))
        whenever(posCanBeLaunchedInTab.invoke(false)).thenReturn(WooPosLaunchability.Launchable)

        isLocalCatalogSupported = WooPosIsLocalCatalogSupported(
            wooPosLocalCatalogM1Enabled = featureFlagM1Enabled,
            getWooVersion = getWooVersion,
            fetchWooVersion = fetchWooVersion,
            posTabShouldBeVisible = posTabShouldBeVisible,
            posCanBeLaunchedInTab = posCanBeLaunchedInTab,
            wooPosLogWrapper = logger,
        )
    }

    @Test
    fun `given all conditions met, when check invoked, then returns true`() = testBlocking {
        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given feature flag disabled, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(false)

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given WC version below minimum, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("10.4.9")

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given WC version unknown, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn(null)

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given cached version null but fetched version sufficient, when check invoked, then returns true`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn("10.5.0")

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given POS tab should not be visible, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(posTabShouldBeVisible.invoke(false)).thenReturn(Result.success(false))

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given POS tab visibility check fails, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(posTabShouldBeVisible.invoke(false)).thenReturn(
            Result.failure(Exception("Visibility check failed"))
        )

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given POS cannot be launched in tab, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(posCanBeLaunchedInTab.invoke(false)).thenReturn(
            WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.UnsupportedWooCommerceVersion
            )
        )

        // WHEN
        val result = isLocalCatalogSupported()

        // THEN
        assertThat(result).isFalse()
    }
}
