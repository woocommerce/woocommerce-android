package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class FetchRefundPreviewTest : BaseUnitTest() {
    private val site = SiteModel().apply { id = LOCAL_SITE_ID }
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn site
    }
    private val refundStore: WCRefundStore = mock()
    private val availabilityCache = ServerRefundAvailabilityCache()
    private val resolveRefundFlow: ResolveRefundFlow = mock {
        on { invoke() } doReturn RefundCalculationFlow.ServerComputed(WOO_VERSION)
    }

    private val sut = FetchRefundPreview(refundStore, selectedSite, availabilityCache, resolveRefundFlow)

    private val selectedItems = listOf(
        ProductRefundListItem(
            orderItem = OrderTestUtils.generateTestOrderItems().first().copy(itemId = PRODUCT_LINE_ID),
            quantity = 2
        )
    )

    @Test
    fun `given store is not eligible, when fetching, then local calculation is used without a request`() =
        testBlocking {
            // GIVEN
            whenever(resolveRefundFlow.invoke()).thenReturn(RefundCalculationFlow.LocalComputed)

            // WHEN
            val result = sut(ORDER_ID, selectedItems)

            // THEN
            assertThat(result).isEqualTo(FetchRefundPreview.Result.FallbackToLocal)
            verify(refundStore, never()).previewRefund(any(), any(), any())
        }

    @Test
    fun `given nothing to send, when fetching, then local calculation is used without a request`() = testBlocking {
        // WHEN
        val result = sut(ORDER_ID, emptyList())

        // THEN
        assertThat(result).isEqualTo(FetchRefundPreview.Result.FallbackToLocal)
        verify(refundStore, never()).previewRefund(any(), any(), any())
    }

    @Test
    fun `given preview succeeds, when fetching, then server totals are returned and store marked available`() =
        testBlocking {
            // GIVEN
            val preview = givenPreview()
            whenever(refundStore.previewRefund(any(), any(), any())).thenReturn(WooResult(preview))

            // WHEN
            val result = sut(ORDER_ID, selectedItems)

            // THEN
            assertThat(result).isEqualTo(FetchRefundPreview.Result.ServerCalculated(preview))
            assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID, WOO_VERSION)).isTrue()
            verify(refundStore).previewRefund(
                site,
                ORDER_ID,
                listOf(RefundPreviewLineItem.quantityBased(lineItemId = PRODUCT_LINE_ID, quantity = 2))
            )
        }

    @Test
    fun `given preview route is missing, when fetching, then local calculation is used and store marked unavailable`() =
        testBlocking {
            // GIVEN
            givenPreviewError(WooErrorType.API_NOT_FOUND, apiErrorCode = "rest_no_route")

            // WHEN
            val result = sut(ORDER_ID, selectedItems)

            // THEN
            assertThat(result).isEqualTo(FetchRefundPreview.Result.FallbackToLocal)
            assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID, WOO_VERSION)).isFalse()
        }

    @Test
    fun `given preview exceeds what is left to refund, when fetching, then local calculation is used`() =
        testBlocking {
            // GIVEN
            givenPreviewError(
                WooErrorType.INVALID_RESPONSE,
                apiErrorCode = "woocommerce_rest_preview_exceeds_max_refundable"
            )

            // WHEN
            val result = sut(ORDER_ID, selectedItems)

            // THEN
            assertThat(result).isEqualTo(FetchRefundPreview.Result.FallbackToLocal)
            assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID, WOO_VERSION)).isNull()
        }

    @Test
    fun `given store rejects the preview, when fetching, then store message is returned`() = testBlocking {
        // GIVEN
        givenPreviewError(
            WooErrorType.INVALID_RESPONSE,
            apiErrorCode = "woocommerce_rest_line_item_already_refunded",
            message = "Line item already refunded."
        )

        // WHEN
        val result = sut(ORDER_ID, selectedItems)

        // THEN
        assertThat(result).isEqualTo(FetchRefundPreview.Result.Error(storeMessage = "Line item already refunded."))
    }

    @Test
    fun `given preview fails with a network error, when fetching, then error without store message is returned`() =
        testBlocking {
            // GIVEN
            givenPreviewError(WooErrorType.TIMEOUT, apiErrorCode = null, message = "timeout")

            // WHEN
            val result = sut(ORDER_ID, selectedItems)

            // THEN
            assertThat(result).isEqualTo(FetchRefundPreview.Result.Error(storeMessage = null))
            assertThat(availabilityCache.isAvailable(LOCAL_SITE_ID, WOO_VERSION)).isNull()
        }

    private suspend fun givenPreviewError(type: WooErrorType, apiErrorCode: String?, message: String? = null) {
        whenever(refundStore.previewRefund(any(), any(), any())).thenReturn(
            WooResult(
                WooError(
                    type = type,
                    original = BaseRequest.GenericErrorType.UNKNOWN,
                    message = message,
                    apiErrorCode = apiErrorCode
                )
            )
        )
    }

    private fun givenPreview(): WCRefundPreview {
        val emptySection = WCRefundPreview.Section(emptyList(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)
        return WCRefundPreview(
            subtotal = BigDecimal("10.00"),
            tax = BigDecimal("1.00"),
            total = BigDecimal("11.00"),
            maxRefundable = BigDecimal("50.00"),
            breakdown = WCRefundPreview.Breakdown(emptySection, emptySection, emptySection)
        )
    }

    private companion object {
        const val ORDER_ID = 1L
        const val LOCAL_SITE_ID = 7
        const val WOO_VERSION = "11.3.0"
        const val PRODUCT_LINE_ID = 21L
    }
}
