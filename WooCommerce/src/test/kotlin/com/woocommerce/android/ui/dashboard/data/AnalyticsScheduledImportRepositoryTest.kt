package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

class AnalyticsScheduledImportRepositoryTest {
    private val site = SiteModel()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(site)
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val repository = AnalyticsScheduledImportRepository(
        wooCommerceStore,
        selectedSite
    )

    @Test
    fun `when setting is enabled, then isEnabled returns true`() = runTest {
        whenever(wooCommerceStore.fetchAnalyticsScheduledImportEnabled(site)).thenReturn(WooResult(true))

        val result = repository.isEnabled()

        assertThat(result.isError).isFalse
        assertThat(result.model).isTrue
    }

    @Test
    fun `when setting is disabled, then isEnabled returns false`() = runTest {
        whenever(wooCommerceStore.fetchAnalyticsScheduledImportEnabled(site)).thenReturn(WooResult(false))

        val result = repository.isEnabled()

        assertThat(result.isError).isFalse
        assertThat(result.model).isFalse
    }

    @Test
    fun `when fetch fails, then isEnabled propagates the error`() = runTest {
        whenever(wooCommerceStore.fetchAnalyticsScheduledImportEnabled(site)).thenReturn(getFailureWooResult())

        val result = repository.isEnabled()

        assertThat(result.isError).isTrue
    }

    @Test
    fun `when enabling the setting succeeds, then setEnabled returns true`() = runTest {
        whenever(wooCommerceStore.updateAnalyticsScheduledImportEnabled(site, true)).thenReturn(WooResult(true))

        val result = repository.setEnabled(true)

        assertThat(result.isError).isFalse
        assertThat(result.model).isTrue
    }

    @Test
    fun `when disabling the setting succeeds, then setEnabled returns false`() = runTest {
        whenever(wooCommerceStore.updateAnalyticsScheduledImportEnabled(site, false)).thenReturn(WooResult(false))

        val result = repository.setEnabled(false)

        assertThat(result.isError).isFalse
        assertThat(result.model).isFalse
    }

    @Test
    fun `when update fails, then setEnabled propagates the error`() = runTest {
        whenever(wooCommerceStore.updateAnalyticsScheduledImportEnabled(site, true)).thenReturn(getFailureWooResult())

        val result = repository.setEnabled(true)

        assertThat(result.isError).isTrue
    }

    private fun getFailureWooResult() = WooResult<Boolean>(
        error = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.NETWORK_ERROR,
            message = "Failed to fetch analytics scheduled import setting"
        )
    )
}
