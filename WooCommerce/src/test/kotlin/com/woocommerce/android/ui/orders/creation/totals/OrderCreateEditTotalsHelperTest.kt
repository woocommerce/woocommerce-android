package com.woocommerce.android.ui.orders.creation.totals

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.TabletOrdersFeatureFlagWrapper
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
    private val isTabletOrdersM1Enabled: TabletOrdersFeatureFlagWrapper = mock()
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
        isTabletOrdersM1Enabled = isTabletOrdersM1Enabled,
        resourceProvider = resourceProvider,
        currencyFormatter
    )

    private val order = Order.EMPTY.copy(
        currency = "USD",
    )

    @Test
    fun `given ff disabled, when mapToPaymentTotalsState, then disabled returned`() {
        // GIVEN
        whenever(isTabletOrdersM1Enabled()).thenReturn(false)

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            order,
            mock(),
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {}
        )

        // THEN
        assertThat(actual).isEqualTo(TotalsSectionsState.Disabled)
    }

    @Test
    fun `given ff enabled and items not empty, when mapToPaymentTotalsState, then shown returned`() {
        // GIVEN
        whenever(isTabletOrdersM1Enabled()).thenReturn(true)
        whenever(resourceProvider.getString(R.string.order_creation_collect_payment_button)).thenReturn(
            "Collect Payment"
        )
        val localOrder = order.copy(
            items = listOf(mock()),
            feesLines = emptyList()
        )

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            localOrder,
            OrderCreateEditViewModel.Mode.Creation,
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {}
        )

        // THEN
        assertThat((actual as TotalsSectionsState.Full).mainButton.text).isEqualTo("Collect Payment")
    }

    @Test
    fun `given ff enabled and fee lines not empty, when mapToPaymentTotalsState, then shown returned`() {
        // GIVEN
        whenever(isTabletOrdersM1Enabled()).thenReturn(true)
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
            OrderCreateEditViewModel.Mode.Creation,
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {}
        )

        // THEN
        assertThat((actual as TotalsSectionsState.Full).mainButton.text).isEqualTo("Collect Payment")
    }

    @Test
    fun `given ff enabled and items and fee lines empty, when mapToPaymentTotalsState, then minimised returned`() {
        // GIVEN
        whenever(isTabletOrdersM1Enabled()).thenReturn(true)

        val localOrder = order.copy(
            items = emptyList(),
            feesLines = emptyList()
        )

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            localOrder,
            OrderCreateEditViewModel.Mode.Creation,
            OrderCreateEditViewModel.ViewState(),
            {},
            {},
            {},
            {},
            {}
        )

        // THEN
        assertThat(actual).isInstanceOf(TotalsSectionsState.Minimised::class.java)
    }
}
