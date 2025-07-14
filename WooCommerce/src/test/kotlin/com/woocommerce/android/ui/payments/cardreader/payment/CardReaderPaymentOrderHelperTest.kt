package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import java.math.BigDecimal

class CardReaderPaymentOrderHelperTest {
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSite: SelectedSite = mock()
    private val currencyFormatter: CurrencyFormatter = mock()

    private val helper = CardReaderPaymentOrderHelper(
        resourceProvider,
        selectedSite,
        currencyFormatter,
    )

    @Test
    fun `given order and site, when getPaymentDescription, then return correct description`() {
        // GIVEN
        val order: Order = mock {
            on { number }.thenReturn("1")
        }
        val remoteId = mock<RemoteId> {
            on { value }.thenReturn(2L)
        }
        val site = mock<SiteModel> {
            on { name }.thenReturn("test")
            on { remoteId() }.thenReturn(remoteId)
        }
        whenever(selectedSite.get()).thenReturn(site)

        whenever(
            resourceProvider.getString(
                R.string.card_reader_payment_description_v2,
                order.number,
                selectedSite.get().name.orEmpty(),
                selectedSite.get().remoteId().value
            )
        ).thenReturn(
            "In-Person Payment for Order #1 for test blog_id 2."
        )

        // WHEN
        val result = helper.getPaymentDescription(order)

        // THEN
        assertThat(result).isEqualTo("In-Person Payment for Order #1 for test blog_id 2.")
    }

    @Test
    fun `given usd order, when getAmountLabel, then return correct label`() {
        // GIVEN
        val order: Order = mock {
            on { total }.thenReturn(BigDecimal.valueOf(1.00))
            on { currency }.thenReturn("USD")
        }
        whenever(currencyFormatter.formatAmountWithCurrency(order.total.toDouble(), order.currency))
            .thenReturn("$1.00")

        // WHEN
        val result = helper.getAmountLabel(order)

        // THEN
        assertThat(result).isEqualTo("$1.00")
    }

    @Test
    fun `given order with id, when getReceiptDocumentName, then return correct name`() {
        // GIVEN
        val order: Order = mock {
            on { id }.thenReturn(1L)
        }

        // WHEN
        val result = helper.getReceiptDocumentName(order.id)

        // THEN
        assertThat(result).isEqualTo("receipt-order-1")
    }
}
