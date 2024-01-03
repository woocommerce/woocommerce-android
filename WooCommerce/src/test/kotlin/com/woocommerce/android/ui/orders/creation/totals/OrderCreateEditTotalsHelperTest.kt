package com.woocommerce.android.ui.orders.creation.totals

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.TabletOrdersFeatureFlagWrapper
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class OrderCreateEditTotalsHelperTest {
    private val isTabletOrdersM1Enabled: TabletOrdersFeatureFlagWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) }.thenReturn("")
    }
    private val helper = OrderCreateEditTotalsHelper(
        isTabletOrdersM1Enabled = isTabletOrdersM1Enabled,
        resourceProvider = resourceProvider
    )

    @Test
    fun `given ff disabled, when mapToPaymentTotalsState, then disabled returned`() {
        // GIVEN
        whenever(isTabletOrdersM1Enabled()).thenReturn(false)

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            mock(),
            mock(),
            mock()
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
        val order = mock<Order> {
            whenever(it.items).thenReturn(listOf(mock()))
        }

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            OrderCreateEditViewModel.Mode.Creation,
            order,
            mock()
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
        val order = mock<Order> {
            whenever(it.feesLines).thenReturn(listOf(mock()))
        }

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            OrderCreateEditViewModel.Mode.Creation,
            order,
            mock()
        )

        // THEN
        assertThat((actual as TotalsSectionsState.Full).mainButton.text).isEqualTo("Collect Payment")
    }

    @Test
    fun `given ff enabled and items and fee lines empty, when mapToPaymentTotalsState, then minised returned`() {
        // GIVEN
        whenever(isTabletOrdersM1Enabled()).thenReturn(true)
        val order = mock<Order> {
            whenever(it.items).thenReturn(emptyList())
            whenever(it.feesLines).thenReturn(emptyList())
        }

        // WHEN
        val actual = helper.mapToPaymentTotalsState(
            OrderCreateEditViewModel.Mode.Creation,
            order,
            mock()
        )

        // THEN
        assertThat(actual).isInstanceOf(TotalsSectionsState.Minimised::class.java)
    }
}
