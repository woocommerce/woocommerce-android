package org.wordpress.android.fluxc.wc.refunds

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundMapper
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundPreviewRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundPreviewRestClient.RefundPreviewResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.dao.RefundDao
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WCRefundStore.Companion.DEFAULT_PAGE
import org.wordpress.android.fluxc.store.WCRefundStore.Companion.DEFAULT_PAGE_SIZE
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RefundStoreTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val restClient = mock<RefundRestClient>()
    private val previewRestClient = mock<RefundPreviewRestClient>()
    private val site = SiteModel()
    private val mapper = RefundMapper(Gson())
    private lateinit var store: WCRefundStore
    private lateinit var refundDao: RefundDao

    private val orderId = 1L
    private val refundId = REFUND_RESPONSE.refundId
    private val error = WooError(
        WooErrorType.INVALID_ID,
        BaseRequest.GenericErrorType.NOT_FOUND,
        "Invalid order ID"
    )

    @Before
    fun setUp() {
        refundDao = databaseRule.db.refundDao

        store = WCRefundStore(
            restClient,
            previewRestClient,
            initCoroutineEngine(),
            mapper,
            refundDao,
        )
    }

    @Test
    fun `fetch all refunds of an order`() = test {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val result = fetchAllTestRefunds()

        assertThat(result.model?.size).isEqualTo(data.size)
        assertThat(result.model?.first()).isEqualTo(mapper.toModel(data.first()))

        val invalidRequestResult = store.fetchAllRefunds(site, 2)
        assertThat(invalidRequestResult.model).isNull()
        assertThat(invalidRequestResult.error).isEqualTo(error)
    }

    @Test
    fun `get all refunds of an order`() = test {
        fetchAllTestRefunds()

        val refunds = store.getAllRefunds(site, orderId)

        assertThat(refunds.size).isEqualTo(1)
        assertThat(refunds.first()).isEqualTo(mapper.toModel(REFUND_RESPONSE))

        val invalidRequestResult = store.getAllRefunds(site, 2)
        assertThat(invalidRequestResult.size).isEqualTo(0)
    }

    @Test
    fun `fetch specific refund`() = test {
        val refund = fetchSpecificTestRefund()

        assertThat(refund.model).isEqualTo(mapper.toModel(REFUND_RESPONSE))
    }

    @Test
    fun `get specific refund`() = test {
        fetchSpecificTestRefund()
        val refund = store.getRefund(site, orderId, refundId)

        assertThat(refund).isEqualTo(mapper.toModel(REFUND_RESPONSE))
    }

    @Test
    fun `when previewRefund succeeds, then server-calculated response is mapped to model`() = test {
        val previewResponse = RefundPreviewResponse(
            breakdown = null,
            subtotal = "100.00",
            tax = "10.00",
            total = "110.00",
            maxRefundable = "200.00",
        )
        val lineItems = listOf(RefundPreviewLineItem.quantityBased(lineItemId = 1L, quantity = 2))
        whenever(previewRestClient.previewRefund(site, orderId, lineItems))
            .thenReturn(WooPayload(previewResponse))

        val result = store.previewRefund(site, orderId, lineItems)

        assertThat(result.model).isEqualTo(mapper.toPreviewModel(previewResponse))
        assertThat(result.model?.total?.toPlainString()).isEqualTo("110.00")
    }

    @Test
    fun `when previewRefund route is missing, then API_NOT_FOUND is surfaced for fallback`() = test {
        val lineItems = listOf(RefundPreviewLineItem.quantityBased(lineItemId = 1L, quantity = 1))
        val notFound = WooError(WooErrorType.API_NOT_FOUND, BaseRequest.GenericErrorType.NOT_FOUND)
        whenever(previewRestClient.previewRefund(site, orderId, lineItems))
            .thenReturn(WooPayload(notFound))

        val result = store.previewRefund(site, orderId, lineItems)

        assertThat(result.model).isNull()
        assertThat(result.error.type).isEqualTo(WooErrorType.API_NOT_FOUND)
    }

    @Test
    fun `when createComputedItemsRefund succeeds, then response is mapped to model`() = test {
        val lineItems = listOf(ComputedRefundLineItem.quantityBased(lineItemId = 1L, quantity = 2))
        whenever(
            restClient.createComputedRefund(
                site = site,
                orderId = orderId,
                reason = "reason",
                apiRefund = false,
                apiRestock = true,
                amount = null,
                lineItems = lineItems,
            )
        ).thenReturn(WooPayload(REFUND_RESPONSE))

        val result = store.createComputedItemsRefund(
            site = site,
            orderId = orderId,
            reason = "reason",
            autoRefund = false,
            restockItems = true,
            amount = null,
            items = lineItems,
        )

        assertThat(result.model).isEqualTo(mapper.toModel(REFUND_RESPONSE))
    }

    @Test
    fun `given an amount override, when createComputedItemsRefund, then amount is passed as a string`() = test {
        val lineItems = listOf(ComputedRefundLineItem.quantityBased(lineItemId = 1L, quantity = 2))
        whenever(
            restClient.createComputedRefund(
                site = site,
                orderId = orderId,
                reason = "",
                apiRefund = true,
                apiRestock = false,
                amount = "12.34",
                lineItems = lineItems,
            )
        ).thenReturn(WooPayload(REFUND_RESPONSE))

        val result = store.createComputedItemsRefund(
            site = site,
            orderId = orderId,
            reason = "",
            autoRefund = true,
            restockItems = false,
            amount = "12.34".toBigDecimal(),
            items = lineItems,
        )

        assertThat(result.model).isEqualTo(mapper.toModel(REFUND_RESPONSE))
    }

    @Test
    fun `when createComputedItemsRefund fails, then the error is surfaced`() = test {
        val lineItems = listOf(ComputedRefundLineItem.quantityBased(lineItemId = 1L, quantity = 1))
        val notFound = WooError(WooErrorType.API_NOT_FOUND, BaseRequest.GenericErrorType.NOT_FOUND)
        whenever(
            restClient.createComputedRefund(
                site = site,
                orderId = orderId,
                reason = "",
                apiRefund = false,
                apiRestock = true,
                amount = null,
                lineItems = lineItems,
            )
        ).thenReturn(WooPayload(notFound))

        val result = store.createComputedItemsRefund(
            site = site,
            orderId = orderId,
            reason = "",
            autoRefund = false,
            restockItems = true,
            amount = null,
            items = lineItems,
        )

        assertThat(result.model).isNull()
        assertThat(result.error.type).isEqualTo(WooErrorType.API_NOT_FOUND)
    }

    private suspend fun fetchSpecificTestRefund(): WooResult<WCRefundModel> {
        val fetchRefundsPayload = WooPayload(
            REFUND_RESPONSE
        )
        whenever(restClient.fetchRefund(site, orderId, refundId)).thenReturn(
                fetchRefundsPayload
        )

        whenever(restClient.fetchRefund(site, 2, refundId)).thenReturn(
            WooPayload(error)
        )
        return store.fetchRefund(site, orderId, refundId)
    }

    private suspend fun fetchAllTestRefunds(): WooResult<List<WCRefundModel>> {
        val data = arrayOf(REFUND_RESPONSE, REFUND_RESPONSE)
        val fetchRefundsPayload = WooPayload(
            data
        )
        whenever(
            restClient.fetchAllRefunds(
                site,
                orderId,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(
                fetchRefundsPayload
        )
        whenever(
            restClient.fetchAllRefunds(
                site,
                2,
                DEFAULT_PAGE,
                DEFAULT_PAGE_SIZE
            )
        ).thenReturn(
            WooPayload(error)
        )
        return store.fetchAllRefunds(site, orderId)
    }
}
