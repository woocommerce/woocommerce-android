package com.woocommerce.android.ui.orders.wooshippinglabels.refund

import com.woocommerce.android.R
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.RefundLabelResponseDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooShippingLabelRefundViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: WooShippingLabelRefundViewModel
    private val mockOrderId = 456L
    private val mockLabelId = 789L
    val mockSite = SiteModel().apply { siteId = 123 }

    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn mockSite }
    private val repository: WooShippingLabelRepository = mock()
    private val networkStatus: NetworkStatus = mock { on { isConnected() } doReturn true }

    @Before
    fun setup() {
        viewModel = WooShippingLabelRefundViewModel(
            WooShippingLabelRefundFragmentArgs(
                orderId = mockOrderId,
                shipment = ShipmentUIModel(localId = "0", items = emptyList(), labelId = mockLabelId)
            ).toSavedStateHandle(),
            selectedSite = selectedSite,
            repository = repository,
            networkStatus = networkStatus,
            currencyFormatter = mock()
        )
    }

    @Test
    fun `when refund is successful, show success message and exit`() = testBlocking {
        // Given
        var capturedEvents = mutableListOf<Event>()
        viewModel.event.observeForever { capturedEvents.add(it) }
        whenever(
            repository.refundLabel(site = mockSite, orderId = mockOrderId, labelId = mockLabelId)
        ) doReturn WooResult(RefundLabelResponseDTO(true))

        // When
        viewModel.onRefundShippingLabelButtonClicked()

        // Then
        assertThat(capturedEvents.first()).isInstanceOf(Event.ShowSnackbar::class.java)
        assertThat((capturedEvents.first() as Event.ShowSnackbar).message)
            .isEqualTo(R.string.shipping_label_refund_success)
        assertThat(capturedEvents.last()).isEqualTo(Event.Exit)
    }

    @Test
    fun `when refund fails, show error message`() = testBlocking {
        // Given
        var capturedEvent: Event.Exit? = null
        viewModel.event.observeForever { capturedEvent = it as? Event.Exit }
        whenever(
            repository.refundLabel(site = mockSite, orderId = mockOrderId, labelId = mockLabelId)
        ) doReturn WooResult(RefundLabelResponseDTO(false))

        // When
        viewModel.onRefundShippingLabelButtonClicked()

        // Then
        assertThat(capturedEvent).isNotNull()
    }
}
