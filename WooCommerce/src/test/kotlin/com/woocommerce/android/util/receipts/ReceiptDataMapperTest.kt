package com.woocommerce.android.util.receipts

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderModel.FeeLine
import org.wordpress.android.fluxc.model.WCOrderModel.LineItem
import org.wordpress.android.fluxc.model.WCOrderModel.ShippingLine

class ReceiptDataMapperTest : BaseUnitTest() {
    private lateinit var receiptDataMapper: ReceiptDataMapper
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val mockedSite: SiteModel = mock()
    private val mockedOrder: WCOrderModel = spy()

    @Before
    fun setUp() {
        receiptDataMapper = ReceiptDataMapper(resourceProvider, selectedSite)
        whenever(resourceProvider.getString(anyInt())).thenReturn("test")
        whenever(selectedSite.get()).thenReturn(mockedSite)
    }

    @Test
    fun `site's display name used as store name`() {
        val expectedStoreName = "test display name"
        whenever(mockedSite.displayName).thenReturn(expectedStoreName)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.storeName).isEqualTo(expectedStoreName)
    }

    @Test
    fun `verify correct application name static text used`() {
        val expected = "expected app name"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_application_name_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.applicationName).isEqualTo(expected)
    }

    @Test
    fun `verify correct receipt from static text used`() {
        val expected = "expected receipt from"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_from_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.receiptFromFormat).isEqualTo(expected)
    }

    @Test
    fun `verify correct receipt title static text used`() {
        val expected = "expected receipt title"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.receiptTitle).isEqualTo(expected)
    }

    @Test
    fun `verify correct amount paid title static text used`() {
        val expected = "expected amount paid"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_amount_paid_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.amountPaidSectionTitle).isEqualTo(expected)
    }

    @Test
    fun `verify correct date paid title static text used`() {
        val expected = "expected date paid"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_date_paid_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.datePaidSectionTitle).isEqualTo(expected)
    }

    @Test
    fun `verify correct payment method title static text used`() {
        val expected = "expected payment method"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_payment_method_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.paymentMethodSectionTitle).isEqualTo(expected)
    }

    @Test
    fun `verify correct summary section title static text used`() {
        val expected = "expected summary section"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_summary_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.summarySectionTitle).isEqualTo(expected)
    }

    @Test
    fun `verify correct aid title static text used`() {
        val expected = "expected aid"
        whenever(resourceProvider.getString(R.string.card_reader_receipt_aid_title)).thenReturn(expected)

        val result = receiptDataMapper.mapToReceiptData(WCOrderModel(), mock())

        assertThat(result.staticTexts.aid).isEqualTo(expected)
    }

    @Test
    fun `when products list not empty, then added to receipt`() {
        initProducts(order = mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts).isNotEmpty
    }

    @Test
    fun `when product has name, then added as title`() {
        val expectedTitle = "expected title"
        initProducts(order = mockedOrder, name = expectedTitle)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo(expectedTitle)
    }

    @Test
    fun `when product item does not have name, then empty title used`() {
        initProducts(order = mockedOrder, name = null)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo("")
    }

    @Test
    fun `when product item has quantity, then added as quantity`() {
        val expectedQuantity = 10f
        initProducts(order = mockedOrder, quantity = expectedQuantity)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].quantity).isEqualTo(expectedQuantity)
    }

    @Test
    fun `when product item does not have quantity, then 1 is used`() {
        initProducts(order = mockedOrder, quantity = null)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].quantity).isEqualTo(1f)
    }

    @Test
    fun `when product item total is not big decimal, then item not added to list`() {
        initProducts(order = mockedOrder, total = "not a big decimal")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts.isEmpty()).isTrue
    }

    @Test
    fun `when product item total has 3 decimal places, then gets rounded to 2 decimal places`() {
        initProducts(order = mockedOrder, total = "1.123")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].itemsTotalAmount.toString()).isEqualTo("1.12")
    }

    @Test
    fun `when fee line item list not empty, then added to receipt`() {
        initFeeLineItems(order = mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts).isNotEmpty
    }

    @Test
    fun `when fee line item has name, then added as title`() {
        val expectedTitle = "expected title"
        initFeeLineItems(order = mockedOrder, name = expectedTitle)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo(expectedTitle)
    }

    @Test
    fun `when fee line item does not have name, then empty title used`() {
        initFeeLineItems(order = mockedOrder, name = null)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo("")
    }

    @Test
    fun `when mapping fee line item, then 1 is used`() {
        initFeeLineItems(order = mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].quantity).isEqualTo(1f)
    }

    @Test
    fun `when fee line item total is not big decimal, then item not added to list`() {
        initFeeLineItems(order = mockedOrder, total = "not a big decimal")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts.isEmpty()).isTrue
    }

    @Test
    fun `when fee line item total has 3 decimal places, then gets rounded to 2 decimal places`() {
        initFeeLineItems(order = mockedOrder, total = "1.123")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].itemsTotalAmount.toString()).isEqualTo("1.12")
    }

    @Test
    fun `when shipping line item list not empty, then added to receipt`() {
        initShippingLineItems(order = mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts).isNotEmpty
    }

    @Test
    fun `when shipping line item has name, then added as title`() {
        val expectedTitle = "expected title"
        initShippingLineItems(order = mockedOrder, name = expectedTitle)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo(expectedTitle)
    }

    @Test
    fun `when shipping line item does not have name, then empty title used`() {
        initShippingLineItems(order = mockedOrder, name = null)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo("")
    }

    @Test
    fun `when mapping shipping line item, then quantity 1 used`() {
        initShippingLineItems(order = mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].quantity).isEqualTo(1f)
    }

    @Test
    fun `when shipping line item total is not big decimal, then item not added to list`() {
        initShippingLineItems(order = mockedOrder, total = "not a big decimal")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts.isEmpty()).isTrue
    }

    @Test
    fun `when shipping line item total has 3 decimal places, then gets rounded to 2 decimal places`() {
        initShippingLineItems(order = mockedOrder, total = "1.123")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].itemsTotalAmount.toString()).isEqualTo("1.12")
    }

    @Test
    fun `when total discount not empty, then added to receipt`() {
        whenever(mockedOrder.discountTotal).thenReturn("123")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts).isNotEmpty
    }

    @Test
    fun `when total discount not empty, then correct title used`() {
        val expectedTitle = "expected title"
        initDiscount(mockedOrder, name = expectedTitle)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo(expectedTitle)
    }

    @Test
    fun `when mapping total discount, then quantity 1 used`() {
        initDiscount(mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].quantity).isEqualTo(1f)
    }

    @Test
    fun `when total discount item total is not big decimal, then item not added to list`() {
        initDiscount(mockedOrder, total = "not a big decimal")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts.isEmpty()).isTrue
    }

    @Test
    fun `when total discount item has 3 decimal places, then gets rounded to 2 decimal places`() {
        initDiscount(mockedOrder, total = "1.123")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].itemsTotalAmount.toString()).isEqualTo("1.12")
    }

    @Test
    fun `when taxes not empty, then added to receipt`() {
        initTax(mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts).isNotEmpty
    }

    @Test
    fun `when taxes not empty, then correct title used`() {
        val expectedTitle = "expected title"
        initTax(mockedOrder, name = expectedTitle)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo(expectedTitle)
    }

    @Test
    fun `when mapping taxes, then quantity 1 used`() {
        initTax(mockedOrder)

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].quantity).isEqualTo(1f)
    }

    @Test
    fun `when taxes item total is not big decimal, then item not added to list`() {
        initTax(mockedOrder, total = "not a big decimal")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts.isEmpty()).isTrue
    }

    @Test
    fun `when taxes item has 3 decimal places, then gets rounded to 2 decimal places`() {
        initTax(mockedOrder, total = "1.123")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].itemsTotalAmount.toString()).isEqualTo("1.12")
    }

    @Test
    fun `when mapping line items, then items in correct order`() {
        initProducts(mockedOrder, name = "product")
        initFeeLineItems(mockedOrder, name = "fee")
        initShippingLineItems(mockedOrder, name = "shipping")
        initDiscount(mockedOrder, name = "discount")
        initTax(mockedOrder, name = "tax")

        val result = receiptDataMapper.mapToReceiptData(mockedOrder, mock())

        assertThat(result.purchasedProducts[0].title).isEqualTo("product")
        assertThat(result.purchasedProducts[1].title).isEqualTo("discount")
        assertThat(result.purchasedProducts[2].title).isEqualTo("fee")
        assertThat(result.purchasedProducts[3].title).isEqualTo("shipping")
        assertThat(result.purchasedProducts[4].title).isEqualTo("tax")
    }

    private fun initProducts(
        order: WCOrderModel,
        name: String? = "",
        quantity: Float? = 0f,
        total: String = "0"
    ): WCOrderModel {
        val lineItem = mock<LineItem>()
        whenever(lineItem.name).thenReturn(name)
        whenever(lineItem.quantity).thenReturn(quantity)
        whenever(lineItem.total).thenReturn(total)
        whenever(order.getLineItemList()).thenReturn(listOf(lineItem))
        return order
    }

    private fun initFeeLineItems(
        order: WCOrderModel,
        name: String? = "",
        total: String = "0"
    ): WCOrderModel {
        val lineItem = mock<FeeLine>()
        whenever(lineItem.name).thenReturn(name)
        whenever(lineItem.total).thenReturn(total)
        whenever(order.getFeeLineList()).thenReturn(listOf(lineItem))
        return order
    }

    private fun initShippingLineItems(
        order: WCOrderModel,
        name: String? = "",
        total: String = "0"
    ): WCOrderModel {
        val lineItem = mock<ShippingLine>()
        whenever(lineItem.methodTitle).thenReturn(name)
        whenever(lineItem.total).thenReturn(total)
        whenever(order.getShippingLineList()).thenReturn(listOf(lineItem))
        return order
    }

    private fun initDiscount(
        order: WCOrderModel,
        name: String = "",
        total: String = "0"
    ) {
        whenever(order.discountTotal).thenReturn(total)
        whenever(resourceProvider.getString(R.string.discount)).thenReturn(name)
    }

    private fun initTax(
        order: WCOrderModel,
        name: String = "",
        total: String = "0"
    ) {
        whenever(order.totalTax).thenReturn(total)
        whenever(resourceProvider.getString(R.string.taxes)).thenReturn(name)
    }
}
