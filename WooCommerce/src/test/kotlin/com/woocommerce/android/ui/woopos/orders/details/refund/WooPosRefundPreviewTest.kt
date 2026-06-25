package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosRefundPreviewTest {

    private val refundStore: WCRefundStore = mock()
    private val selectedSite: com.woocommerce.android.tools.SelectedSite = mock()
    private val availabilityCache = WooPosV4RefundAvailabilityCache()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_REFUND_V4) } doReturn true
    }

    private val site = SiteModel().apply { siteId = SITE_ID }
    private val lineItems = listOf(RefundV4LineItem(lineItemId = 1L, quantity = 1))

    private val sut by lazy {
        whenever(selectedSite.get()).thenReturn(site)
        // The version mock defaults to null (unknown) → not below support → cases probe the network,
        // unless a test stubs a specific version.
        WooPosRefundPreview(refundStore, selectedSite, availabilityCache, getWooCoreVersion, featureFlagRepository)
    }

    @Test
    fun `given v4 flag disabled, when invoked, then falls back without probing or marking availability`() = runTest {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_REFUND_V4)).thenReturn(false)

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        assertThat(availabilityCache.isV4Available(SITE_ID)).isNull()
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given v4 available, when preview succeeds, then returns server-calculated and marks available`() = runTest {
        // GIVEN
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(preview()))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosRefundPreview.Result.ServerCalculated::class.java)
        assertThat(availabilityCache.isV4Available(SITE_ID)).isTrue()
    }

    @Test
    fun `given preview returns 404, when invoked, then falls back to local and marks unavailable`() = runTest {
        // GIVEN
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(WooError(WooErrorType.API_NOT_FOUND, GenericErrorType.NOT_FOUND)))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        assertThat(availabilityCache.isV4Available(SITE_ID)).isFalse()
    }

    @Test
    fun `given non-404 error, when invoked, then returns Error`() = runTest {
        // GIVEN
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.NETWORK_ERROR)))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.Error)
    }

    @Test
    fun `given WC older than 10_9_0, when invoked, then falls back without probing and marks unavailable`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("10.8.0")

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        assertThat(availabilityCache.isV4Available(SITE_ID)).isFalse()
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given WC at 10_9_0, when invoked, then probes v4`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("10.9.0")
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(preview()))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosRefundPreview.Result.ServerCalculated::class.java)
        verify(refundStore).previewRefund(eq(site), eq(ORDER_ID), eq(lineItems))
    }

    @Test
    fun `given WC version unknown, when invoked, then still probes v4`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn(null)
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(preview()))

        // WHEN
        sut(ORDER_ID, lineItems)

        // THEN
        verify(refundStore).previewRefund(eq(site), eq(ORDER_ID), eq(lineItems))
    }

    @Test
    fun `given v4 known unavailable, when invoked, then falls back without probing`() = runTest {
        // GIVEN
        availabilityCache.markV4Unavailable(SITE_ID)

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given empty selection, when invoked, then falls back without probing`() = runTest {
        // WHEN
        val result = sut(ORDER_ID, emptyList())

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    private fun preview() = WCRefundPreview(
        subtotal = BigDecimal("100.00"),
        tax = BigDecimal("10.00"),
        total = BigDecimal("110.00"),
        maxRefundable = BigDecimal("200.00"),
        breakdown = WCRefundPreview.Breakdown(
            products = emptySection(),
            shipping = emptySection(),
            fees = emptySection(),
        ),
    )

    private fun emptySection() = WCRefundPreview.Section(
        items = emptyList(),
        subtotal = BigDecimal.ZERO,
        tax = BigDecimal.ZERO,
        total = BigDecimal.ZERO,
    )

    private companion object {
        private const val SITE_ID = 7L
        private const val ORDER_ID = 123L
    }
}
