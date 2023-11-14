package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.R.string
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundViewModel.ShippingLabelRefundViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class ShippingLabelRefundViewModelTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_ORDER_ID = 1L
        private const val REMOTE_SHIPPING_LABEL_ID = 1L
    }

    private val repository: ShippingLabelRepository = mock()
    private val networkStatus: NetworkStatus = mock()

    private val shippingLabel = OrderTestUtils.generateShippingLabel(shippingLabelId = REMOTE_SHIPPING_LABEL_ID)

    private val savedState = ShippingLabelRefundFragmentArgs(
        orderId = REMOTE_ORDER_ID,
        shippingLabelId = REMOTE_SHIPPING_LABEL_ID
    ).initSavedStateHandle()

    private val shippingLabelViewStateTestData = ShippingLabelRefundViewState(shippingLabel = shippingLabel)
    private lateinit var viewModel: ShippingLabelRefundViewModel

    @Before
    fun setup() {
        doReturn(shippingLabel).whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()

        viewModel = spy(
            ShippingLabelRefundViewModel(
                savedState,
                repository,
                networkStatus
            )
        )

        clearInvocations(
            viewModel,
            repository,
            networkStatus
        )
    }

    @Test
    fun `Displays the refund shipping label view correctly`() = testBlocking {
        var shippingLabelData: ShippingLabelRefundViewState? = null
        viewModel.shippingLabelRefundViewStateData.observeForever { _, new -> shippingLabelData = new }

        viewModel.start()
        assertThat(shippingLabelData).isEqualTo(shippingLabelViewStateTestData)
    }

    @Test
    fun `Refunds the shipping label correctly`() = testBlocking {
        doReturn(WooResult(true)).whenever(repository).refundShippingLabel(any(), any())

        var snackBar: ShowSnackbar? = null
        var exitEvent: Exit? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackBar = it
            if (it is Exit) exitEvent = it
        }

        viewModel.start()
        viewModel.onRefundShippingLabelButtonClicked()

        verify(repository, times(1)).getShippingLabelByOrderIdAndLabelId(any(), any())
        assertThat(snackBar).isEqualTo(ShowSnackbar(string.shipping_label_refund_success))
        assertNotNull(exitEvent)
    }

    @Test
    fun `Refunds the shipping label results in an error`() = testBlocking {
        doReturn(
            WooResult<Boolean>(WooError(API_ERROR, NETWORK_ERROR, ""))
        ).whenever(repository).refundShippingLabel(any(), any())

        var snackBar: ShowSnackbar? = null
        var exitEvent: Exit? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackBar = it
            if (it is Exit) exitEvent = it
        }

        viewModel.start()
        viewModel.onRefundShippingLabelButtonClicked()

        verify(repository, times(1)).getShippingLabelByOrderIdAndLabelId(any(), any())
        assertThat(snackBar).isEqualTo(ShowSnackbar(string.order_refunds_amount_refund_error))
        assertNull(exitEvent)
    }

    @Test
    fun `disable refund if label is anonymized`() = testBlocking {
        doReturn(shippingLabel.copy(status = "ANONYMIZED"))
            .whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())

        var viewState: ShippingLabelRefundViewState? = null
        viewModel.shippingLabelRefundViewStateData.observeForever { _, new -> viewState = new }

        viewModel.start()

        assertThat(viewState?.isRefundExpired).isTrue()
    }

    @Test
    fun `disable refund if label is older than 30 days`() = testBlocking {
        doReturn(shippingLabel.copy(createdDate = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(31))))
            .whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())

        var viewState: ShippingLabelRefundViewState? = null
        viewModel.shippingLabelRefundViewStateData.observeForever { _, new -> viewState = new }

        viewModel.start()

        assertThat(viewState?.isRefundExpired).isTrue()
    }

    @Test
    fun `enable refund if label is recent than 30 days`() = testBlocking {
        doReturn(shippingLabel.copy(createdDate = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(29))))
            .whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())

        var viewState: ShippingLabelRefundViewState? = null
        viewModel.shippingLabelRefundViewStateData.observeForever { _, new -> viewState = new }

        viewModel.start()

        assertThat(viewState?.isRefundExpired).isFalse()
    }
}
