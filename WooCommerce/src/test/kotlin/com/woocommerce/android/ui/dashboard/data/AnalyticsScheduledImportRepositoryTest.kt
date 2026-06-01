package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
    fun `when the cached setting is enabled, then observeIsEnabled emits true`() = runTest {
        whenever(wooCommerceStore.observeAnalyticsScheduledImportEnabled(site)).thenReturn(flowOf(true))

        val result = repository.observeIsEnabled().first()

        assertThat(result).isTrue
    }

    @Test
    fun `when the cached setting is disabled, then observeIsEnabled emits false`() = runTest {
        whenever(wooCommerceStore.observeAnalyticsScheduledImportEnabled(site)).thenReturn(flowOf(false))

        val result = repository.observeIsEnabled().first()

        assertThat(result).isFalse
    }

    @Test
    fun `when the value is not cached, then observeIsEnabled emits false`() = runTest {
        whenever(wooCommerceStore.observeAnalyticsScheduledImportEnabled(site)).thenReturn(flowOf(null))

        val result = repository.observeIsEnabled().first()

        assertThat(result).isFalse
    }

    @Test
    fun `when refresh succeeds, then it returns the fetched value`() = runTest {
        whenever(wooCommerceStore.fetchAnalyticsScheduledImportEnabled(site)).thenReturn(WooResult(true))

        val result = repository.refresh()

        assertThat(result.isError).isFalse
        assertThat(result.model).isTrue
    }

    @Test
    fun `when refresh fails, then it propagates the error`() = runTest {
        whenever(wooCommerceStore.fetchAnalyticsScheduledImportEnabled(site)).thenReturn(getFailureWooResult())

        val result = repository.refresh()

        assertThat(result.isError).isTrue
    }

    @Test
    fun `when setEnabled succeeds, then it returns the updated value`() = runTest {
        whenever(wooCommerceStore.updateAnalyticsScheduledImportEnabled(site, false)).thenReturn(WooResult(false))

        val result = repository.setEnabled(false)

        assertThat(result.isError).isFalse
        assertThat(result.model).isFalse
    }

    @Test
    fun `when setEnabled fails, then it propagates the error`() = runTest {
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
