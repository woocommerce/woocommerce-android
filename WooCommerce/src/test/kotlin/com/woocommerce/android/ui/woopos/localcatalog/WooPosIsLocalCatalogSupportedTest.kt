package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogFileApproachEnabled
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.tab.WooPosCanBeLaunchedInTab
import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.FetchActiveWCPluginVersion
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId

@ExperimentalCoroutinesApi
class WooPosIsLocalCatalogSupportedTest : BaseUnitTest() {

    private var featureFlagM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private var fileApproachEnabled: WooPosLocalCatalogFileApproachEnabled = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private var getWooVersion: GetWooCorePluginCachedVersion = mock()
    private var fetchWooVersion: FetchActiveWCPluginVersion = mock()
    private var logger: WooPosLogWrapper = mock()
    private var posTabShouldBeVisible: WooPosTabShouldBeVisible = mock()
    private var posCanBeLaunchedInTab: WooPosCanBeLaunchedInTab = mock()

    private lateinit var isLocalCatalogSupported: WooPosIsLocalCatalogSupported

    companion object {
        private val SITE_ID = LocalOrRemoteId.LocalId(123)
    }

    @Before
    fun setup() = testBlocking {
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(fileApproachEnabled.invoke()).thenReturn(true)
        whenever(getWooVersion()).thenReturn("10.5.0")
        whenever(posTabShouldBeVisible.invoke(false)).thenReturn(Result.success(true))
        whenever(posCanBeLaunchedInTab.invoke(false)).thenReturn(WooPosLaunchability.Launchable)

        val variationsEndpointChecker = WooPosIsLocalCatalogVariationsEndpointAvailable(
            getWooVersion = getWooVersion,
            fetchWooVersion = fetchWooVersion,
            logger = logger,
            dispatchers = coroutinesTestRule.testDispatchers
        )

        isLocalCatalogSupported = WooPosIsLocalCatalogSupported(
            wooPosLocalCatalogM1Enabled = featureFlagM1Enabled,
            fileApproachEnabled = fileApproachEnabled,
            prefsRepo = preferencesRepository,
            isVariationsEndpointAvailable = variationsEndpointChecker,
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
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given feature flag disabled, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(featureFlagM1Enabled.invoke()).thenReturn(false)

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given file approach disabled and variations endpoint not available, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(fileApproachEnabled.invoke()).thenReturn(false)
        whenever(getWooVersion()).thenReturn("10.2.9")

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given file approach disabled and all paginated sync checks pass, when check invoked, then returns true`() = testBlocking {
        // GIVEN
        whenever(fileApproachEnabled.invoke()).thenReturn(false)
        whenever(getWooVersion()).thenReturn("10.3.0")
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(SITE_ID)).thenReturn(true)

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given file approach disabled and periodic sync disabled, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(fileApproachEnabled.invoke()).thenReturn(false)
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(SITE_ID)).thenReturn(false)

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given file approach enabled and WC version below minimum, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("10.4.9")

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given file approach enabled and WC version unknown, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn(null)

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given file approach enabled and cached version null but fetched version sufficient, when check invoked, then returns true`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn(null)
        whenever(fetchWooVersion()).thenReturn("10.5.0")

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given POS tab should not be visible, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(posTabShouldBeVisible.invoke(false)).thenReturn(Result.success(false))

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

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
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given POS cannot be launched in tab, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(posCanBeLaunchedInTab.invoke(false)).thenReturn(
            WooPosLaunchability.NotLaunchable(
                WooPosLaunchability.NonLaunchabilityReason.UnsupportedCurrency
            )
        )

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }
}
