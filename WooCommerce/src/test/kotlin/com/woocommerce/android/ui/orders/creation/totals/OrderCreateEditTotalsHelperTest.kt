package com.woocommerce.android.ui.orders.creation.totals

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class OrderCreateEditTotalsHelperTest {
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) }.thenReturn("")
    }
    private val bigDecimalFormatter = mock<(BigDecimal) -> String> {
        on { invoke(anyOrNull()) }.thenReturn("10.00$")
    }
    private val currencyFormatter: CurrencyFormatter = mock {
        on { buildBigDecimalFormatter("USD") }.thenReturn(bigDecimalFormatter)
    }
    private val helper = OrderCreateEditTotalsHelper(
        resourceProvider = resourceProvider,
        currencyFormatter
    )

    private val order = Order.getEmptyOrder(mock(), mock()).copy(
        currency = "USD",
    )

    @Test
    @Suppress("LongMethod")
    fun `given items not empty, when mapToPaymentTotalsState, then full returned`() {
        // GIVEN
        val item = mock<Order.Item> {
            on { total }.thenReturn(BigDecimal(11))
        }
        val feeLine = mock<Order.FeeLine> {
            on { total }.thenReturn(BigDecimal(12))
        }
        val shippingLine = mock<Order.ShippingLine> {
            on { methodId }.thenReturn("methodId")
            on { total }.thenReturn(BigDecimal(13))
        }
        val taxLines = listOf(
            mock<Order.TaxLine> {
                on { label }.thenReturn("tax 1")
                on { ratePercent }.thenReturn(5F)
                on { taxTotal }.thenReturn("10")
            },
            mock<Order.TaxLine> {
                on { label }.thenReturn("tax 2")
                on { ratePercent }.thenReturn(6F)
                on { taxTotal }.thenReturn("11")
            }
        )
        val localOrder = order.copy(
            items = listOf(item),
            feesLines = listOf(feeLine),
            productsTotal = BigDecimal(11),
            discountTotal = BigDecimal(14),
            discountCodes = "20OFF",
            shippingLines = listOf(shippingLine),
            selectedGiftCard = "21OFF",
            giftCardDiscountedAmount = BigDecimal(15),
            taxLines = taxLines,
            totalTax = BigDecimal(16)
        )

        whenever(resourceProvider.getString(R.string.order_creation_collect_payment_button)).thenReturn(
            "Collect Payment"
        )
        whenever(resourceProvider.getString(R.string.order_creation_payment_products)).thenReturn(
            "Products"
        )
        whenever(resourceProvider.getString(R.string.order_creation_payment_order_total)).thenReturn(
            "Order Total"
        )
        whenever(resourceProvider.getString(R.string.custom_amounts)).thenReturn(
            "Custom Amounts"
        )
        whenever(resourceProvider.getString(R.string.shipping)).thenReturn(
            "Shipping"
        )
        whenever(resourceProvider.getString(R.string.order_creation_coupon_button)).thenReturn(
            "Coupons"
        )
        whenever(resourceProvider.getString(R.string.order_gift_card)).thenReturn(
            "Gift Cards"
        )
        whenever(resourceProvider.getString(R.string.order_creation_payment_tax_label)).thenReturn(
            "Taxes"
        )
        whenever(resourceProvider.getString(R.string.order_creation_tax_based_on_billing_address)).thenReturn(
            "tax based on billing address"
        )
        whenever(resourceProvider.getString(R.string.learn_more)).thenReturn(
            "learn More"
        )

        whenever(
            resourceProvider.getString(R.string.order_creation_discounts_total_value, "14.00$")
        ).thenReturn("-14.00$")

        whenever(
            resourceProvider.getString(R.string.order_creation_coupon_discount_value, "14.00$")
        ).thenReturn("-14.00$")

        whenever(bigDecimalFormatter.invoke(BigDecimal(10))).thenReturn("10.00$")
        whenever(bigDecimalFormatter.invoke(BigDecimal(11))).thenReturn("11.00$")
        whenever(bigDecimalFormatter.invoke(BigDecimal(12))).thenReturn("12.00$")
        whenever(bigDecimalFormatter.invoke(BigDecimal(13))).thenReturn("13.00$")
        whenever(bigDecimalFormatter.invoke(BigDecimal(14))).thenReturn("14.00$")
        whenever(bigDecimalFormatter.invoke(BigDecimal(15))).thenReturn("15.00$")
        whenever(bigDecimalFormatter.invoke(BigDecimal(16))).thenReturn("16.00$")

        val onCouponsClicked = mock<() -> Unit>()
        val onGiftClicked = mock<() -> Unit>()
        val onTaxesLearnMore = mock<() -> Unit>()
        val onMainButtonClicked = mock<() -> Unit>()
        val onRecalculateButtonClicked = mock<() -> Unit>()
        val onExpandCollapseClicked = mock<() -> Unit>()
        val onHeightChanged = mock<(Int) -> Unit>()

        val taxBasedOnSettingLabel = "tax based on billing address"

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            localOrder,
            OrderCreateEditViewModel.Mode.Creation(),
            OrderCreateEditViewModel.ViewState(
                taxBasedOnSettingLabel = taxBasedOnSettingLabel
            ),
            onCouponsClicked,
            onGiftClicked,
            onTaxesLearnMore,
            onMainButtonClicked,
            onRecalculateButtonClicked,
            onExpandCollapseClicked,
            onHeightChanged,
        )

        // THEN
        assertThat((actual as TotalsSectionsState.Full).mainButton.text).isEqualTo("Collect Payment")
        assertThat(actual.mainButton.enabled).isTrue()
        assertThat(actual.mainButton.onClick == onMainButtonClicked).isTrue()

        assertThat(actual.onExpandCollapseClicked == onExpandCollapseClicked).isTrue()

        assertThat(actual.orderTotal.value).isEqualTo("10.00$")
        assertThat(actual.orderTotal.label).isEqualTo("Order Total")

        assertThat((actual.lines[0] as TotalsSectionsState.Line.Simple).label).isEqualTo("Products")
        assertThat((actual.lines[0] as TotalsSectionsState.Line.Simple).value).isEqualTo("11.00$")

        assertThat((actual.lines[1] as TotalsSectionsState.Line.Simple).label).isEqualTo("Custom Amounts")
        assertThat((actual.lines[1] as TotalsSectionsState.Line.Simple).value).isEqualTo("12.00$")

        assertThat((actual.lines[2] as TotalsSectionsState.Line.Simple).label).isEqualTo("Shipping")
        assertThat((actual.lines[2] as TotalsSectionsState.Line.Simple).value).isEqualTo("13.00$")

        assertThat((actual.lines[3] as TotalsSectionsState.Line.Button).text).isEqualTo("Coupons")
        assertThat((actual.lines[3] as TotalsSectionsState.Line.Button).value).isEqualTo("-14.00$")
        assertThat((actual.lines[3] as TotalsSectionsState.Line.Button).enabled).isFalse()
        assertThat((actual.lines[3] as TotalsSectionsState.Line.Button).extraValue).isEqualTo("20OFF")
        assertThat((actual.lines[3] as TotalsSectionsState.Line.Button).onClick == onCouponsClicked).isTrue()

        assertThat((actual.lines[4] as TotalsSectionsState.Line.Button).text).isEqualTo("Gift Cards")
        assertThat((actual.lines[4] as TotalsSectionsState.Line.Button).value).isEqualTo("-15.00$")
        assertThat((actual.lines[4] as TotalsSectionsState.Line.Button).enabled).isFalse()
        assertThat((actual.lines[4] as TotalsSectionsState.Line.Button).extraValue).isEqualTo("21OFF")
        assertThat((actual.lines[4] as TotalsSectionsState.Line.Button).onClick == onGiftClicked).isTrue()

        val taxesLines = (actual.lines[5] as TotalsSectionsState.Line.Block).lines
        assertThat((taxesLines[0] as TotalsSectionsState.Line.Simple).label).isEqualTo("Taxes")
        assertThat((taxesLines[0] as TotalsSectionsState.Line.Simple).value).isEqualTo("16.00$")
        assertThat((taxesLines[1] as TotalsSectionsState.Line.SimpleSmall).label).isEqualTo("tax 1 · 5.0%")
        assertThat((taxesLines[1] as TotalsSectionsState.Line.SimpleSmall).value).isEqualTo("10.00$")
        assertThat((taxesLines[2] as TotalsSectionsState.Line.SimpleSmall).label).isEqualTo("tax 2 · 6.0%")
        assertThat((taxesLines[2] as TotalsSectionsState.Line.SimpleSmall).value).isEqualTo("11.00$")
        assertThat((taxesLines[3] as TotalsSectionsState.Line.LearnMore).text).isEqualTo(taxBasedOnSettingLabel)
        assertThat((taxesLines[3] as TotalsSectionsState.Line.LearnMore).buttonText).isEqualTo("learn More")
        assertThat((taxesLines[3] as TotalsSectionsState.Line.LearnMore).onClick == onTaxesLearnMore).isTrue()
    }

    @Test
    fun `given fee lines not empty, when mapToPaymentTotalsState, then full returned`() {
        // GIVEN
        whenever(resourceProvider.getString(R.string.order_creation_collect_payment_button)).thenReturn(
            "Collect Payment"
        )
        val localOrder = order.copy(
            items = emptyList(),
            feesLines = listOf(
                mock {
                    on { total }.thenReturn(BigDecimal.TEN)
                }
            )
        )

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            localOrder,
            OrderCreateEditViewModel.Mode.Creation(),
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )

        // THEN
        assertThat((actual as TotalsSectionsState.Full).mainButton.text).isEqualTo("Collect Payment")
    }

    @Test
    fun `given items and fee lines empty, when mapToPaymentTotalsState, then minimised returned`() {
        // GIVEN
        val localOrder = order.copy(
            items = emptyList(),
            feesLines = emptyList(),
            total = BigDecimal.TEN
        )

        whenever(resourceProvider.getString(R.string.order_creation_payment_order_total)).thenReturn(
            "Order Total"
        )

        whenever(bigDecimalFormatter.invoke(BigDecimal.TEN)).thenReturn("10.00$")

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            localOrder,
            OrderCreateEditViewModel.Mode.Creation(),
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )

        // THEN
        assertThat((actual as TotalsSectionsState.Minimised).orderTotal.label).isEqualTo("Order Total")
        assertThat(actual.orderTotal.value).isEqualTo("10.00$")
    }

    @Test
    fun `given items not empty and gift amount is null, when mapToPaymentTotalsState, then gift value is empty`() {
        // GIVEN
        val item = mock<Order.Item> {
            on { total }.thenReturn(BigDecimal(11))
        }
        val localOrder = order.copy(
            items = listOf(item),
            selectedGiftCard = "21OFF",
            giftCardDiscountedAmount = null,
        )

        whenever(resourceProvider.getString(R.string.order_gift_card)).thenReturn(
            "Gift Cards"
        )

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            localOrder,
            OrderCreateEditViewModel.Mode.Creation(),
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {},
            {},
            {},
        )

        // THEN
        actual as TotalsSectionsState.Full

        assertThat((actual.lines[1] as TotalsSectionsState.Line.Button).text).isEqualTo("Gift Cards")
        assertThat((actual.lines[1] as TotalsSectionsState.Line.Button).value).isEqualTo("")
        assertThat((actual.lines[1] as TotalsSectionsState.Line.Button).enabled).isFalse()
        assertThat((actual.lines[1] as TotalsSectionsState.Line.Button).extraValue).isEqualTo("21OFF")
    }
}
