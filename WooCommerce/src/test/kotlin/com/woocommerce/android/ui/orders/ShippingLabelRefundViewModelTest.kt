package com.woocommerce.android.ui.orders

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R.string
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundFragmentArgs
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundViewModel
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRefundViewModel.ShippingLabelRefundViewState
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
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

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val shippingLabel = OrderTestUtils.generateShippingLabel(
        remoteOrderId = REMOTE_ORDER_ID, shippingLabelId = REMOTE_SHIPPING_LABEL_ID
    )

    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            ShippingLabelRefundFragmentArgs(orderId = REMOTE_ORDER_ID, shippingLabelId = REMOTE_SHIPPING_LABEL_ID)
        )
    )

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
                networkStatus,
                coroutinesTestRule.testDispatchers
            ))

        clearInvocations(
            viewModel,
            savedState,
            repository,
            networkStatus
        )
    }

    @Test
    fun `Displays the refund shipping label view correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var shippingLabelData: ShippingLabelRefundViewState? = null
        viewModel.shippingLabelRefundViewStateData.observeForever { _, new -> shippingLabelData = new }

        viewModel.start()
        assertThat(shippingLabelData).isEqualTo(shippingLabelViewStateTestData)
    }

    @Test
    fun `Refunds the shipping label correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
    fun `Refunds the shipping label results in an error`() = coroutinesTestRule.testDispatcher.runBlockingTest {
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
}
