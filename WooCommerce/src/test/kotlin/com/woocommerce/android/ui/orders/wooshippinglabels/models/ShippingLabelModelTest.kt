package com.woocommerce.android.ui.orders.wooshippinglabels.models

import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.UNKNOWN
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import java.math.BigDecimal
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShippingLabelModelTest : BaseUnitTest() {
    private val model = ShippingLabelModel(
        labelId = 1,
        tracking = "tracking",
        refundableAmount = BigDecimal(1),
        status = UNKNOWN,
        created = Date(0),
        carrierId = "fedex",
        serviceName = "serviceName",
        commercialInvoiceUrl = "commercialInvoiceUrl",
        isCommercialInvoiceSubmittedElectronically = false,
        packageName = "packageName",
        isLetter = false,
        productNames = emptyList(),
        productIds = emptyList(),
        shipmentId = "0",
        receiptItemId = 0L,
        createdDate = Date(0),
        mainReceiptId = 0L,
        rate = BigDecimal(1),
        currency = "USD",
        expiryDate = 32503680000000, // far future date
        usedDate = null
    )

    @Test
    fun `when anonymized, refund should be unavailable`() = testBlocking {
        val sut = model.copy(status = ShippingLabelStatus.ANONYMIZED)

        assertFalse(sut.isRefundAvailable)
    }

    @Test
    fun `when created a week ago, refund should be available`() = testBlocking {
        val now = System.currentTimeMillis()
        val weekAgo = now - TimeUnit.DAYS.toMillis(7)
        val sut = model.copy(createdDate = Date(weekAgo))

        assertTrue(sut.isRefundAvailable)
    }

    @Test
    fun `when created two months ago, refund should be unavailable`() = testBlocking {
        val now = System.currentTimeMillis()
        val twoMonthsAgo = now - TimeUnit.DAYS.toMillis(60)
        val sut = model.copy(createdDate = Date(twoMonthsAgo))

        assertFalse(sut.isRefundAvailable)
    }

    @Test
    fun `when used, refund should be unavailable`() = testBlocking {
        val sut = model.copy(usedDate = null)

        assertFalse(sut.isRefundAvailable)
    }

    @Test
    fun `when expired, refund should be unavailable`() = testBlocking {
        val now = System.currentTimeMillis()
        val dayAgo = now - TimeUnit.DAYS.toMillis(1)
        val sut = model.copy(expiryDate = dayAgo)

        assertFalse(sut.isRefundAvailable)
    }

    @Test
    fun `when usps, refund should be unavailable`() = testBlocking {
        val sut = model.copy(carrierId = "usps")

        assertFalse(sut.isRefundAvailable)
    }

    @Test
    fun `when no tracking, refund should be unavailable`() = testBlocking {
        val sut = model.copy(tracking = "")

        assertFalse(sut.isRefundAvailable)
    }
}
