package com.woocommerce.android.ui.woopos.receiptprinting

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsRepository
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Labels
import com.woocommerce.android.ui.woopos.receiptprinting.WooPosThermalReceipt.Row
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosReceiptDataResult
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsReceiptRepository
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreRepository
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreState.ReceiptInfo
import com.woocommerce.android.ui.woopos.settings.details.store.WooPosSettingsStoreState.StoreInfo
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosBuildThermalReceiptTest {
    @get:Rule
    val coroutinesTestRule = WooPosCoroutineTestRule()

    private val selectedSite: SelectedSite = mock {
        on { getSelectedSiteId() } doReturn SITE_ID
    }
    private val totalsRepository: WooPosTotalsRepository = mock {
        on { getOrderById(ORDER_ID) } doReturn ORDER
    }
    private val receiptRepository: WooPosSettingsReceiptRepository = mock {
        on { getReceiptInfo() } doReturn WooPosReceiptDataResult.Success(RECEIPT_INFO)
    }
    private val storeRepository: WooPosSettingsStoreRepository = mock {
        on { getStoreInfo() } doReturn STORE_INFO
    }
    private val formatPrice: WooPosFormatPrice = mock {
        on { invoke(any<BigDecimal>(), eq(CURRENCY)) } doAnswer {
            val amount = it.getArgument<BigDecimal>(0)
            (if (amount.signum() < 0) "-" else "") + "$" + amount.abs().toPlainString()
        }
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { STRINGS.getValue(it.getArgument(0)) }
        on { getString(any(), anyVararg()) } doAnswer {
            STRINGS.getValue(it.getArgument(0)).format(*it.arguments.drop(1).toTypedArray())
        }
    }

    private val sut = WooPosBuildThermalReceipt(
        selectedSite = selectedSite,
        totalsRepository = totalsRepository,
        receiptRepository = receiptRepository,
        storeRepository = storeRepository,
        formatPrice = formatPrice,
        resourceProvider = resourceProvider,
    )

    private val defaultLocale = Locale.getDefault()
    private val defaultTimeZone = TimeZone.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(defaultLocale)
        TimeZone.setDefault(defaultTimeZone)
    }

    @Test
    fun `given an order with every line, when building the receipt, then every field is mapped in order`() = runTest {
        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt?.copy(datePaid = "")).isEqualTo(
            WooPosThermalReceipt(
                storeName = "Everyday Pay",
                storeAddress = "12345 Example Street S, Springfield Heights, CA 90000",
                storePhone = "05555 465 5556",
                storeEmail = "shop@everyday-pay.example",
                datePaid = "",
                orderNumber = "Order #1042",
                paymentMethod = null,
                lineItems = listOf(
                    Row(label = "T-Shirt. Color Blue, Size M x2", amount = "$50.00"),
                    Row(label = "Coffee Mug x1", amount = "$12.00"),
                ),
                totals = listOf(
                    Row(label = "Subtotal", amount = "$62.00"),
                    Row(label = "Discount (SAVE10, VIP)", amount = "-$6.20"),
                    Row(label = "Fees", amount = "$1.00"),
                    Row(label = "Shipping", amount = "$5.00"),
                    Row(label = "Taxes", amount = "$4.94"),
                    Row(label = "Amount Paid", amount = "$66.74"),
                ),
                orderNote = "Thanks for shopping with us!",
                refundPolicy = "Returns accepted within 30 days with the original receipt.",
            )
        )
    }

    @Test
    fun `given a card or a cash order, when building the receipt, then there is no payment method`() = runTest {
        // GIVEN
        val orders = listOf(
            ORDER.copy(paymentMethod = "woocommerce_payments", isCashPayment = false),
            ORDER.copy(paymentMethod = "cod", isCashPayment = true),
        )

        // WHEN
        val receipts = orders.map { order ->
            whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(order)
            sut(ORDER_ID)
        }

        // THEN
        assertThat(receipts).extracting("paymentMethod").containsOnlyNulls()
    }

    @Test
    fun `given a paid order, when building the receipt, then the date is the paid date in medium and short style`() =
        runTest {
            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.datePaid).matches("Nov 12, 2026,? 2:30\\hPM")
        }

    @Test
    fun `given an order without a paid date, when building the receipt, then the date is the creation date`() =
        runTest {
            // GIVEN
            whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(ORDER.copy(datePaid = null))

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.datePaid).matches("Nov 10, 2026,? 9:05\\hAM")
        }

    @Test
    fun `given a discount without coupons, when building the receipt, then the discount label has no codes`() =
        runTest {
            // GIVEN
            whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(ORDER.copy(discountCodes = ""))

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.totals).contains(Row(label = "Discount", amount = "-$6.20"))
        }

    @Test
    fun `given a coupon that discounts nothing, when building the receipt, then the discount line is kept`() =
        runTest {
            // GIVEN
            whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(
                ORDER.copy(discountTotal = BigDecimal("0.00"), discountCodes = "FREESHIP")
            )

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.totals).contains(Row(label = "Discount (FREESHIP)", amount = "$0.00"))
        }

    @Test
    fun `given no discount, fees, shipping or taxes, when building the receipt, then those lines are absent`() =
        runTest {
            // GIVEN
            whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(
                ORDER.copy(
                    discountTotal = BigDecimal("0.00"),
                    discountCodes = "",
                    feesLines = emptyList(),
                    shippingTotal = BigDecimal("0.00"),
                    totalTax = BigDecimal("0.00"),
                    total = BigDecimal("62.00"),
                )
            )

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.totals).containsExactly(
                Row(label = "Subtotal", amount = "$62.00"),
                Row(label = "Amount Paid", amount = "$62.00"),
            )
        }

    @Test
    fun `given attributes with HTML or missing parts, when building the receipt, then only complete ones print`() =
        runTest {
            // GIVEN
            val item = ITEM.copy(
                name = "Hoodie",
                quantity = 1.5f,
                attributesList = listOf(
                    Order.Item.Attribute(key = "<b>Color</b>", value = "blue"),
                    Order.Item.Attribute(key = "Size", value = ""),
                    Order.Item.Attribute(key = "", value = "Cotton"),
                ),
            )
            whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(ORDER.copy(items = listOf(item)))

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.lineItems).containsExactly(Row(label = "Hoodie. Color Blue x1.5", amount = "$50.00"))
        }

    @Test
    fun `given an order note with HTML, when building the receipt, then the tags are stripped`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(
            ORDER.copy(customerNote = "<p>Leave at the <strong>front desk</strong> &amp; call</p>")
        )

        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt?.orderNote).isEqualTo("Leave at the front desk & call")
    }

    @Test
    fun `given an order note that is blank once stripped, when building the receipt, then the note is absent`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(ORDER.copy(customerNote = "<p> </p>"))

        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt?.orderNote).isNull()
    }

    @Test
    fun `given an order note made only of tags, when building the receipt, then the note is absent`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(ORDER_ID))
            .thenReturn(ORDER.copy(customerNote = "<p></p><img src=\"a.png\" />"))

        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt?.orderNote).isNull()
    }

    @Test
    fun `given an order note with several paragraphs, when building the receipt, then it starts with the text`() =
        runTest {
            // GIVEN
            whenever(totalsRepository.getOrderById(ORDER_ID))
                .thenReturn(ORDER.copy(customerNote = "<p>Gift wrap</p><p>Ring the bell</p>\n"))

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.orderNote).isEqualTo("Gift wrap\nRing the bell")
        }

    @Test
    fun `given the settings fetch fails, when building the receipt, then the cached settings are used`() = runTest {
        // GIVEN
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.Error)
        whenever(receiptRepository.getCachedReceiptInfo()).thenReturn(RECEIPT_INFO.copy(storeName = "Cached Pay"))

        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt?.storeName).isEqualTo("Cached Pay")
        assertThat(receipt?.storePhone).isEqualTo("05555 465 5556")
        assertThat(receipt?.refundPolicy).isEqualTo("Returns accepted within 30 days with the original receipt.")
    }

    @Test
    fun `given the settings fetch fails and nothing is cached, when building the receipt, then the store info is used`() =
        runTest {
            // GIVEN
            whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.Error)

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.storeName).isEqualTo("Site Title")
            assertThat(receipt?.storeAddress).isEqualTo("1 Site Road, Springfield, CA 90000, US")
            assertThat(receipt?.storePhone).isNull()
            assertThat(receipt?.storeEmail).isNull()
            assertThat(receipt?.refundPolicy).isNull()
        }

    @Test
    fun `given receipt settings are not available, when building the receipt, then the store info is used`() =
        runTest {
            // GIVEN
            whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.storeName).isEqualTo("Site Title")
            assertThat(receipt?.storeAddress).isEqualTo("1 Site Road, Springfield, CA 90000, US")
        }

    @Test
    fun `given blank receipt settings, when building the receipt, then name and address fall back and the rest is absent`() =
        runTest {
            // GIVEN
            whenever(receiptRepository.getReceiptInfo()).thenReturn(
                WooPosReceiptDataResult.Success(
                    ReceiptInfo(storeName = "", address = " ", phone = "", email = " ", refundPolicy = "\n")
                )
            )

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt?.storeName).isEqualTo("Site Title")
            assertThat(receipt?.storeAddress).isEqualTo("1 Site Road, Springfield, CA 90000, US")
            assertThat(receipt?.storePhone).isNull()
            assertThat(receipt?.storeEmail).isNull()
            assertThat(receipt?.refundPolicy).isNull()
        }

    @Test
    fun `given no address anywhere, when building the receipt, then the address is absent`() = runTest {
        // GIVEN
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)
        whenever(storeRepository.getStoreInfo()).thenReturn(StoreInfo(storeName = "Site Title", address = ""))

        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt?.storeAddress).isNull()
    }

    @Test
    fun `given the order is not found, when building the receipt, then there is no receipt`() = runTest {
        // GIVEN
        whenever(totalsRepository.getOrderById(ORDER_ID)).thenReturn(null)

        // WHEN
        val receipt = sut(ORDER_ID)

        // THEN
        assertThat(receipt).isNull()
    }

    @Test
    fun `given the selected site changes during the last read, when building the receipt, then there is no receipt`() =
        runTest {
            // GIVEN
            var siteId = SITE_ID
            whenever(selectedSite.getSelectedSiteId()).thenAnswer { siteId }
            whenever(storeRepository.getStoreInfo()).doAnswer {
                siteId = SITE_ID + 1
                STORE_INFO
            }

            // WHEN
            val receipt = sut(ORDER_ID)

            // THEN
            assertThat(receipt).isNull()
        }

    @Test
    fun `when reading the labels, then they come from the receipt strings`() {
        // WHEN
        val labels = sut.labels()

        // THEN
        assertThat(labels).isEqualTo(Labels(datePaid = "Date Paid", paymentMethod = "Payment Method", notes = "Notes"))
    }

    private companion object {
        const val ORDER_ID = 1042L
        const val SITE_ID = 1
        const val CURRENCY = "USD"

        val STRINGS = mapOf(
            R.string.woopos_receipt_date_paid to "Date Paid",
            R.string.woopos_receipt_order_number to "Order #%1\$s",
            R.string.woopos_receipt_payment_method to "Payment Method",
            R.string.woopos_receipt_item_with_attributes to "%1\$s. %2\$s",
            R.string.woopos_receipt_item_and_quantity to "%1\$s x%2\$s",
            R.string.woopos_receipt_subtotal to "Subtotal",
            R.string.woopos_receipt_discount to "Discount",
            R.string.woopos_receipt_discount_with_coupons to "Discount (%1\$s)",
            R.string.woopos_receipt_fees to "Fees",
            R.string.woopos_receipt_shipping to "Shipping",
            R.string.woopos_receipt_taxes to "Taxes",
            R.string.woopos_receipt_amount_paid to "Amount Paid",
            R.string.woopos_receipt_notes to "Notes",
        )

        val RECEIPT_INFO = ReceiptInfo(
            storeName = "Everyday Pay",
            address = "12345 Example Street S, Springfield Heights, CA 90000",
            phone = "05555 465 5556",
            email = "shop@everyday-pay.example",
            refundPolicy = "Returns accepted within 30 days with the original receipt.",
        )

        val STORE_INFO = StoreInfo(storeName = "Site Title", address = "1 Site Road, Springfield, CA 90000, US")

        val ITEM = Order.Item.EMPTY.copy(
            name = "T-Shirt",
            quantity = 2f,
            subtotal = BigDecimal("50.00"),
            total = BigDecimal("45.00"),
            attributesList = listOf(
                Order.Item.Attribute(key = "Color", value = "Blue"),
                Order.Item.Attribute(key = "Size", value = "M"),
            ),
        )

        val ORDER = Order.getEmptyOrder(Date(0), Date(0)).copy(
            id = ORDER_ID,
            number = "1042",
            currency = CURRENCY,
            dateCreated = DateTimeUtils.dateUTCFromIso8601("2026-11-10T09:05:00Z"),
            datePaid = DateTimeUtils.dateUTCFromIso8601("2026-11-12T14:30:00Z"),
            items = listOf(
                ITEM,
                Order.Item.EMPTY.copy(
                    name = "Coffee Mug",
                    quantity = 1f,
                    subtotal = BigDecimal("12.00"),
                    total = BigDecimal("10.80"),
                ),
            ),
            productsTotal = BigDecimal("62.00"),
            discountTotal = BigDecimal("6.20"),
            discountCodes = "SAVE10, VIP",
            feesLines = listOf(
                Order.FeeLine.EMPTY.copy(total = BigDecimal("0.60")),
                Order.FeeLine.EMPTY.copy(total = BigDecimal("0.40")),
            ),
            shippingTotal = BigDecimal("5.00"),
            totalTax = BigDecimal("4.94"),
            total = BigDecimal("66.74"),
            customerNote = "Thanks for shopping with us!",
        )
    }
}
