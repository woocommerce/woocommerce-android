package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId

@ExperimentalCoroutinesApi
class WooPosIsLocalCatalogSupportedTest : BaseUnitTest() {

    private var featureFlagM1Enabled: WooPosLocalCatalogM1Enabled = mock()
    private var preferencesRepository: WooPosPreferencesRepository = mock()
    private var getWooVersion: GetWooCorePluginCachedVersion = mock()
    private var logger: WooPosLogWrapper = mock()

    private lateinit var isLocalCatalogSupported: WooPosIsLocalCatalogSupported

    companion object {
        private val SITE_ID = LocalOrRemoteId.LocalId(123)
    }

    @Before
    fun setup() = testBlocking {
        whenever(featureFlagM1Enabled.invoke()).thenReturn(true)
        whenever(getWooVersion()).thenReturn("10.3.0")
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(any())).thenReturn(true)

        val variationsEndpointChecker = WooPosIsLocalCatalogVariationsEndpointAvailable(
            getWooVersion = getWooVersion,
            logger = logger,
            dispatchers = coroutinesTestRule.testDispatchers
        )

        isLocalCatalogSupported = WooPosIsLocalCatalogSupported(
            wooPosLocalCatalogM1Enabled = featureFlagM1Enabled,
            prefsRepo = preferencesRepository,
            isVariationsEndpointAvailable = variationsEndpointChecker
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
    fun `given variations endpoint not available, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("10.2.9")

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given periodic sync disabled, when check invoked, then returns false`() = testBlocking {
        // GIVEN
        whenever(preferencesRepository.isPeriodicSyncEnabledForSite(SITE_ID)).thenReturn(false)

        // WHEN
        val result = isLocalCatalogSupported(SITE_ID)

        // THEN
        assertThat(result).isFalse()
    }
}
