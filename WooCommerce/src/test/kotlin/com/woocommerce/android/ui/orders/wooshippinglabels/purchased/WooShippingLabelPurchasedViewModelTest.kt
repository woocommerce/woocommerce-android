package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPaperSize.LABEL
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPaperSize.LETTER
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPurchasedViewModel.OpenLearnMoreScreen
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPurchasedViewModel.OpenShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPurchasedViewModel.OpenUrl
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPurchasedViewModel.StartRefundRequest
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.WooShippingLabelPurchasedViewModel.ViewState
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
class WooShippingLabelPurchasedViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: WooShippingLabelPurchasedViewModel
    private val fetchShippingLabelFile: FetchShippingLabelFile = mock()
    private val file: File = mock()

    private val mockPurchaseData = PurchasedShippingLabelData(
        labelId = 4158L,
        carrierId = "usps",
        totalWeight = "1.5",
        formattedTotalPrice = "10.00",
        weightUnit = "kg",
        trackingNumber = "123456",
        items = emptyList()
    )

    private val navArgs = WooShippingLabelPurchasedFragmentArgs(
        purchaseData = mockPurchaseData
    )

    @Before
    fun setup() {
        viewModel = WooShippingLabelPurchasedViewModel(
            savedState = navArgs.toSavedStateHandle(),
            fetchShippingLabelFile = fetchShippingLabelFile
        )
    }

    @Test
    fun `onPrintShippingLabelClicked triggers OpenShippingLabelFile event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }
        whenever(fetchShippingLabelFile(listOf(navArgs.purchaseData.labelId), "label")).thenReturn(file)

        viewModel.onPrintShippingLabelClicked()

        verify(fetchShippingLabelFile).invoke(listOf(navArgs.purchaseData.labelId), "label")
        assertThat(latestEvent).isEqualTo(OpenShippingLabelFile(file))
    }

    @Test
    fun `onPrintShippingLabelClicked triggers Loading state correctly`() = testBlocking {
        val viewStateUpdates: MutableList<ViewState> = mutableListOf()
        viewModel.viewState.observeForever { viewStateUpdates.add(it) }
        whenever(fetchShippingLabelFile(listOf(navArgs.purchaseData.labelId), "label")).thenReturn(file)

        viewModel.onPrintShippingLabelClicked()

        assertThat(viewStateUpdates).hasSize(3)
        assertThat(viewStateUpdates[0].isPrintingInProgress).isFalse()
        assertThat(viewStateUpdates[1].isPrintingInProgress).isTrue()
        assertThat(viewStateUpdates[2].isPrintingInProgress).isFalse()
    }

    @Test
    fun `ViewState starts with LABEL paper size as default`() {
        var latestViewState: ViewState? = null
        viewModel.viewState.observeForever { latestViewState = it }

        assertThat(latestViewState).isNotNull
        assertThat(latestViewState?.paperSizeOption).isEqualTo(LABEL)
    }

    @Test
    fun `onLabelPaperSizeOptionSelected updates the ViewState as expected`() {
        var latestViewState: ViewState? = null
        viewModel.viewState.observeForever { latestViewState = it }

        viewModel.onLabelPaperSizeOptionSelected(LETTER)

        assertThat(latestViewState).isNotNull
        assertThat(latestViewState?.paperSizeOption).isEqualTo(LETTER)
    }

    @Test
    fun `onTrackShipmentClicked triggers OpenUrl event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onTrackShipmentClicked()

        assertThat(latestEvent).isEqualTo(OpenUrl("https://tools.usps.com/go/TrackConfirmAction.action?tLabels=123456"))
    }

    @Test
    fun `onSchedulePickUpClicked triggers OpenUrl event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onSchedulePickUpClicked()

        assertThat(latestEvent).isEqualTo(OpenUrl("https://tools.usps.com/schedule-pickup-steps.htm"))
    }

    @Test
    fun `onRefundClicked triggers StartRefundRequest event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onRefundClicked()

        assertThat(latestEvent).isEqualTo(StartRefundRequest)
    }

    @Test
    fun `onLearnMoreClicked triggers OpenLearnMoreScreen event`() = testBlocking {
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onLearnMoreClicked()

        assertThat(latestEvent).isEqualTo(OpenLearnMoreScreen)
    }
}
