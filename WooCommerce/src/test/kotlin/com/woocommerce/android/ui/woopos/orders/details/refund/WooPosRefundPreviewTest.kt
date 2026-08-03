package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.tools.SelectedSite
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
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
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
    private val selectedSite: SelectedSite = mock()
    private val availabilityCache = WooPosServerRefundAvailabilityCache()

    // Defaults to a version that supports server refunds; version-gating tests override it.
    // An unknown (null) version fails closed to the local flow.
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock {
        on { invoke() } doReturn WooPosResolveRefundFlow.MIN_WC_VERSION_FOR_SERVER_REFUNDS
    }
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_REFUND_V4) } doReturn true
    }

    private val site = SiteModel().apply {
        id = LOCAL_SITE_ID
        siteId = SITE_ID
    }
    private val lineItems = listOf(RefundPreviewLineItem.quantityBased(lineItemId = 1L, quantity = 1))

    private val sut by lazy {
        whenever(selectedSite.get()).thenReturn(site)
        WooPosRefundPreview(refundStore, selectedSite, availabilityCache, resolveRefundFlowFor(selectedSite))
    }

    private fun resolveRefundFlowFor(selectedSite: SelectedSite) = WooPosResolveRefundFlow(
        selectedSite = selectedSite,
        availabilityCache = availabilityCache,
        getWooCoreVersion = getWooCoreVersion,
        featureFlagRepository = featureFlagRepository,
    )

    @Test
    fun `given flag disabled, when invoked, then falls back without probing or marking availability`() = runTest {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_REFUND_V4)).thenReturn(false)

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID)).isNull()
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given eligible store, when preview succeeds, then returns server-calculated and marks available`() = runTest {
        // GIVEN
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(preview()))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosRefundPreview.Result.ServerCalculated::class.java)
        assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID)).isTrue()
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
        assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID)).isFalse()
    }

    @Test
    fun `given non-404 error, when invoked, then returns Error without api error`() = runTest {
        // GIVEN
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.NETWORK_ERROR)))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.Error(apiError = null))
    }

    @Test
    fun `given mapped refund error, when invoked, then returns Error carrying the api error`() = runTest {
        // GIVEN
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = GenericErrorType.UNKNOWN,
                        apiErrorCode = "preview_exceeds_max_refundable"
                    )
                )
            )

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN — the mapped error propagates and does not poison the availability verdict.
        assertThat(result).isEqualTo(
            WooPosRefundPreview.Result.Error(apiError = WooPosRefundApiError.AmountExceedsOrderRemaining)
        )
        assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID)).isNull()
    }

    @Test
    fun `given mapped refund error, when invoked, then does not fall back to local`() = runTest {
        // GIVEN — only API_NOT_FOUND (no preview route) may trigger the local fallback.
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(
                WooResult(
                    WooError(
                        type = WooErrorType.GENERIC_ERROR,
                        original = GenericErrorType.UNKNOWN,
                        apiErrorCode = "order_not_refundable"
                    )
                )
            )

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosRefundPreview.Result.Error::class.java)
        assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID)).isNull()
    }

    @Test
    fun `given WC older than 11_1_0, when invoked, then falls back without probing`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("11.0.5")

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isEqualTo(WooPosRefundPreview.Result.FallbackToLocal)
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given WC at 11_1_0, when invoked, then probes the preview route`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn("11.1.0")
        whenever(refundStore.previewRefund(eq(site), eq(ORDER_ID), eq(lineItems)))
            .thenReturn(WooResult(preview()))

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosRefundPreview.Result.ServerCalculated::class.java)
        verify(refundStore).previewRefund(eq(site), eq(ORDER_ID), eq(lineItems))
    }

    @Test
    fun `given WC version unknown, when invoked, then falls back to local without probing`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion.invoke()).thenReturn(null)

        // WHEN
        val result = sut(ORDER_ID, lineItems)

        // THEN
        assertThat(result).isInstanceOf(WooPosRefundPreview.Result.FallbackToLocal::class.java)
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given server refunds known unavailable, when invoked, then falls back without probing`() = runTest {
        // GIVEN
        availabilityCache.markUnavailable(LOCAL_SITE_ID)

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

    @Test
    fun `given two self-hosted sites share remote siteId 0, when one is unavailable, then other still probes`() =
        runTest {
            // GIVEN two distinct local sites that both report remote siteId 0 (self-hosted/WPAPI).
            val siteA = SiteModel().apply {
                id = 101
                siteId = 0L
            }
            val siteB = SiteModel().apply {
                id = 102
                siteId = 0L
            }
            availabilityCache.markUnavailable(siteA.localId().value)

            val selectedSiteB: SelectedSite = mock()
            whenever(selectedSiteB.get()).thenReturn(siteB)
            whenever(refundStore.previewRefund(eq(siteB), eq(ORDER_ID), eq(lineItems)))
                .thenReturn(WooResult(preview()))
            val sutForB = WooPosRefundPreview(
                refundStore,
                selectedSiteB,
                availabilityCache,
                resolveRefundFlowFor(selectedSiteB),
            )

            // WHEN siteB requests a preview
            val result = sutForB(ORDER_ID, lineItems)

            // THEN siteA's verdict does not poison siteB — it probes the route and is marked available.
            assertThat(result).isInstanceOf(WooPosRefundPreview.Result.ServerCalculated::class.java)
            assertThat(availabilityCache.isAvailable(siteB.localId().value)).isTrue()
            verify(refundStore).previewRefund(eq(siteB), eq(ORDER_ID), eq(lineItems))
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
        private const val LOCAL_SITE_ID = 11
        private const val SITE_ID = 7L
        private const val ORDER_ID = 123L
    }
}
