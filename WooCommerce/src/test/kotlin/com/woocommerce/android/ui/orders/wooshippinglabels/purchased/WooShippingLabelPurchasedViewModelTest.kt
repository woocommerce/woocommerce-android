package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PurchaseInProgress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.Purchased
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.Unknown
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
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.io.File

@ExperimentalCoroutinesApi
class WooShippingLabelPurchasedViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: WooShippingLabelPurchasedViewModel
    private val fetchShippingLabelFile: FetchShippingLabelFile = mock()
    private val observeShippingLabelStatus: ObserveShippingLabelStatus = mock()
    private val file: File = mock()

    private val mockPurchaseData = PurchasedShippingLabelData(
        labelId = 4158L,
        orderId = 1234L,
        carrierId = "usps",
        trackingNumber = "123456",
        addresses = mock(),
        rateSummary = mock(),
        shippingLines = emptyList(),
        items = mock()
    )

    private val navArgs = WooShippingLabelPurchasedFragmentArgs(
        purchaseData = mockPurchaseData
    )

    @Test
    fun `onPrintShippingLabelClicked triggers OpenShippingLabelFile event`() = testBlocking {
        createSut()
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }
        whenever(fetchShippingLabelFile(listOf(navArgs.purchaseData.labelId), "label")).thenReturn(file)

        viewModel.onPrintShippingLabelClicked()

        verify(fetchShippingLabelFile).invoke(listOf(navArgs.purchaseData.labelId), "label")
        assertThat(latestEvent).isEqualTo(OpenShippingLabelFile(file))
    }

    @Test
    fun `onPrintShippingLabelClicked triggers Loading state correctly`() = testBlocking {
        createSut()
        val viewStateUpdates: MutableList<ViewState> = mutableListOf()
        viewModel.viewState.observeForever { viewStateUpdates.add(it) }
        whenever(fetchShippingLabelFile(listOf(navArgs.purchaseData.labelId), "label")).thenReturn(file)

        viewModel.onPrintShippingLabelClicked()

        assertThat(viewStateUpdates).hasSize(3)
        assertThat(viewStateUpdates[0].isLoadingData).isFalse()
        assertThat(viewStateUpdates[1].isLoadingData).isTrue()
        assertThat(viewStateUpdates[2].isLoadingData).isFalse()
    }

    @Test
    fun `ViewState starts with LABEL paper size as default`() {
        createSut()
        var latestViewState: ViewState? = null
        viewModel.viewState.observeForever { latestViewState = it }

        assertThat(latestViewState).isNotNull
        assertThat(latestViewState?.paperSizeOption).isEqualTo(LABEL)
    }

    @Test
    fun `onLabelPaperSizeOptionSelected updates the ViewState as expected`() {
        createSut()
        var latestViewState: ViewState? = null
        viewModel.viewState.observeForever { latestViewState = it }

        viewModel.onLabelPaperSizeOptionSelected(LETTER)

        assertThat(latestViewState).isNotNull
        assertThat(latestViewState?.paperSizeOption).isEqualTo(LETTER)
    }

    @Test
    fun `onTrackShipmentClicked triggers OpenUrl event`() = testBlocking {
        createSut()
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onTrackShipmentClicked()

        assertThat(latestEvent).isEqualTo(OpenUrl("https://tools.usps.com/go/TrackConfirmAction.action?tLabels=123456"))
    }

    @Test
    fun `onSchedulePickUpClicked triggers OpenUrl event`() = testBlocking {
        createSut()
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onSchedulePickUpClicked()

        assertThat(latestEvent).isEqualTo(OpenUrl("https://tools.usps.com/schedule-pickup-steps.htm"))
    }

    @Test
    fun `onRefundClicked triggers StartRefundRequest event`() = testBlocking {
        createSut()
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onRefundClicked()

        assertThat(latestEvent).isEqualTo(StartRefundRequest)
    }

    @Test
    fun `onLearnMoreClicked triggers OpenLearnMoreScreen event`() = testBlocking {
        createSut()
        var latestEvent: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { latestEvent = it }

        viewModel.onLearnMoreClicked()

        assertThat(latestEvent).isEqualTo(OpenLearnMoreScreen)
    }

    @Test
    fun `on Shipping Label Status is Unknown, ViewState isLoadingData is true`() = testBlocking {
        var latestState: ViewState? = null
        whenever(observeShippingLabelStatus(labelId = 4158L, orderId = 1234L)).thenReturn(flowOf(Unknown))

        createSut()
        viewModel.viewState.observeForever { latestState = it }

        assertThat(latestState?.isPurchaseFinished).isNull()
    }

    @Test
    fun `on Shipping Label Status is PurchaseInProgress, ViewState isLoadingData is true`() = testBlocking {
        var latestState: ViewState? = null
        whenever(
            observeShippingLabelStatus.invoke(labelId = 4158L, orderId = 1234L)
        ).thenReturn(flowOf(PurchaseInProgress))

        createSut()
        viewModel.viewState.observeForever { latestState = it }

        assertThat(latestState?.isPurchaseFinished).isFalse()
    }

    @Test
    fun `on Shipping Label Status is Purchased, ViewState isLoadingData is false`() = testBlocking {
        var latestState: ViewState? = null
        whenever(observeShippingLabelStatus(labelId = 4158L, orderId = 1234L)).thenReturn(flowOf(Unknown, Purchased))

        createSut()
        viewModel.viewState.observeForever { latestState = it }

        assertThat(latestState?.isPurchaseFinished).isTrue()
    }

    private fun createSut() {
        viewModel = WooShippingLabelPurchasedViewModel(
            savedState = navArgs.toSavedStateHandle(),
            fetchShippingLabelFile = fetchShippingLabelFile,
            observeShippingLabelStatus = observeShippingLabelStatus
        )
    }
}
