package com.woocommerce.android.ui.payments.simplepayments

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam.PaymentOrRefund.Payment.PaymentType
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCOrderStore

@ExperimentalCoroutinesApi
class SimplePaymentsViewModelTests : BaseUnitTest() {
    private val simplePaymentsRepository: SimplePaymentsRepository = mock()
    private val networkStatus: NetworkStatus = mock {
        on { isConnected() }.thenReturn(true)
    }

    private val testOrder: Order
        get() {
            val taxLines = ArrayList<Order.TaxLine>().also {
                it.add(
                    Order.TaxLine(
                        id = TAX_LINE_ID,
                        label = "Test Tax",
                        compound = false,
                        taxTotal = "10.00",
                        ratePercent = TAX_LINE_TAX_RATE,
                        rateCode = TAX_LINE_TAX_RATE_CODE,
                    )
                )
            }
            return OrderTestUtils.generateTestOrder(ORDER_ID).copy(
                number = REMOTE_ORDER_NUMBER,
                taxLines = taxLines
            )
        }

    private lateinit var viewModel: SimplePaymentsViewModel

    private val savedState: SavedStateHandle =
        SimplePaymentsFragmentArgs(
            order = testOrder,
            paymentType = PaymentType.SIMPLE
        ).toSavedStateHandle()

    private fun initViewModel() {
        viewModel = SimplePaymentsViewModel(savedState, simplePaymentsRepository, networkStatus, mock())
    }

    @Test
    fun `when charging taxes is enabled, then taxes are applied to the total amount of the order`() =
        testBlocking {
            initViewModel()
            viewModel.onChargeTaxesChanged(chargeTaxes = true)
            assertThat(viewModel.orderDraft.total).isGreaterThan(viewModel.orderDraft.feesTotal)
        }

    @Test
    fun `when charging taxes is NOT enabled, then total amount is equal to the total fee`() =
        testBlocking {
            initViewModel()
            viewModel.onChargeTaxesChanged(chargeTaxes = false)
            assertThat(viewModel.orderDraft.total).isEqualTo(viewModel.orderDraft.feesTotal)
        }

    @Test
    fun `given error result from update simple payments, when onDoneButtonClicked, then show error message`() =
        testBlocking {
            // GIVEN
            whenever(
                simplePaymentsRepository.updateSimplePayment(
                    orderId = any(),
                    amount = any(),
                    customerNote = any(),
                    billingEmail = any(),
                    isTaxable = any(),
                )
            ).thenReturn(
                flowOf(
                    WCOrderStore.UpdateOrderResult.RemoteUpdateResult(
                        WCOrderStore.OnOrderChanged(
                            orderError = mock()
                        )
                    )
                )
            )
            initViewModel()

            val states = viewModel.viewStateLiveData.liveData.captureValues()

            // WHEN
            viewModel.onDoneButtonClicked()

            // THEN
            assertThat(states[1].isLoading).isFalse
            assertThat(states[2].isLoading).isTrue()
            assertThat(states[3].isLoading).isFalse

            assertThat(viewModel.event.value).isEqualTo(
                MultiLiveEvent.Event.ShowSnackbar(R.string.simple_payments_update_error)
            )
        }

    @Test
    fun `given successful result from update simple payments, when onDoneButtonClicked, then ShowPaymentMethodSelectionScreen`() =
        testBlocking {
            // GIVEN
            whenever(
                simplePaymentsRepository.updateSimplePayment(
                    orderId = any(),
                    amount = any(),
                    customerNote = any(),
                    billingEmail = any(),
                    isTaxable = any(),
                )
            ).thenReturn(
                flowOf(
                    WCOrderStore.UpdateOrderResult.RemoteUpdateResult(
                        WCOrderStore.OnOrderChanged()
                    )
                )
            )
            initViewModel()

            val states = viewModel.viewStateLiveData.liveData.captureValues()

            // WHEN
            viewModel.onDoneButtonClicked()

            // THEN
            assertThat(states[1].isLoading).isFalse
            assertThat(states[2].isLoading).isTrue()
            assertThat(states[3].isLoading).isFalse

            assertThat(viewModel.event.value).isEqualTo(SimplePaymentsViewModel.ShowPaymentMethodSelectionScreen)
        }

    companion object {
        private const val ORDER_ID = 1L
        private const val REMOTE_ORDER_NUMBER = "100"
        private const val TAX_LINE_ID = 1L
        private const val TAX_LINE_TAX_RATE = 0.15f
        private const val TAX_LINE_TAX_RATE_CODE = "US CA 92679"
    }
}
