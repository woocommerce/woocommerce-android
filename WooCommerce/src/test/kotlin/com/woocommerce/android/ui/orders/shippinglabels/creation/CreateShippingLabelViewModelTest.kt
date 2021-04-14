package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.clearInvocations
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPrintShippingLabels
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.FlowStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.StepUiState
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.OriginAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PurchaseSuccess
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.NoOp
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.Idle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.PurchaseLabels
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CarrierStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CustomsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.OriginAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PackagingStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PaymentsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.ShippingAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.DONE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.NOT_READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepsState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Transition
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.model.order.toIdSet
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CreateShippingLabelViewModelTest : BaseUnitTest() {
    private val orderDetailRepository: OrderDetailRepository = mock()
    private val shippingLabelRepository: ShippingLabelRepository = mock()
    private val stateMachine: ShippingLabelsStateMachine = mock()
    private val addressValidator: ShippingLabelAddressValidator = mock()
    private val site: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val accountStore: AccountStore = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val parameterRepository: ParameterRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private lateinit var stateFlow: MutableStateFlow<Transition>

    private val originAddress = CreateShippingLabelTestUtils.generateAddress()
    private val originAddressValidated = originAddress.copy(city = "DONE")
    private val shippingAddress = originAddress.copy(company = "McDonald's")
    private val shippingAddressValidated = shippingAddress.copy(city = "DONE")
    private val order = OrderTestUtils.generateOrder()

    private val data = StateMachineData(
        order = order.toAppModel(),
        stepsState = StepsState(
            originAddressStep = OriginAddressStep(READY, originAddress),
            shippingAddressStep = ShippingAddressStep(NOT_READY, shippingAddress),
            packagingStep = PackagingStep(NOT_READY, emptyList()),
            customsStep = CustomsStep(NOT_READY, isVisible = true),
            carrierStep = CarrierStep(NOT_READY, emptyList()),
            paymentsStep = PaymentsStep(NOT_READY, null)
        )
    )

    private val doneData = StateMachineData(
        order = order.toAppModel(),
        stepsState = StepsState(
            originAddressStep = OriginAddressStep(READY, originAddress),
            shippingAddressStep = ShippingAddressStep(READY, shippingAddress),
            packagingStep = PackagingStep(
                READY,
                listOf(CreateShippingLabelTestUtils.generateShippingLabelPackage())
            ),
            customsStep = CustomsStep(NOT_READY, isVisible = false),
            carrierStep = CarrierStep(READY, listOf(CreateShippingLabelTestUtils.generateRate())),
            paymentsStep = PaymentsStep(READY, CreateShippingLabelTestUtils.generatePaymentMethod())
        )
    )

    private val originAddressCurrent = StepUiState(
        details = originAddress.toString(),
        isEnabled = true,
        isContinueButtonVisible = true,
        isEditButtonVisible = false,
        isHighlighted = true
    )

    private val originAddressDone = originAddressCurrent.copy(
        details = originAddressValidated.toString(),
        isContinueButtonVisible = false,
        isEditButtonVisible = true,
        isHighlighted = false
    )

    private val shippingAddressNotDone = StepUiState(
        details = shippingAddress.toString(),
        isEnabled = false,
        isContinueButtonVisible = false,
        isEditButtonVisible = false,
        isHighlighted = false
    )

    private val shippingAddressCurrent = shippingAddressNotDone.copy(
        isEnabled = true,
        isContinueButtonVisible = true,
        isEditButtonVisible = false,
        isHighlighted = true
    )

    private val shippingAddressDone = shippingAddressCurrent.copy(
        details = shippingAddressValidated.toString(),
        isContinueButtonVisible = false,
        isEditButtonVisible = true,
        isHighlighted = false
    )

    private val otherNotDone = StepUiState(
        isEnabled = false,
        isContinueButtonVisible = false,
        isEditButtonVisible = false,
        isHighlighted = false
    )

    private val otherCurrent = StepUiState(
        isEnabled = true,
        isContinueButtonVisible = true,
        isEditButtonVisible = false,
        isHighlighted = true
    )

    @get:Rule
    var coroutinesTestRule = CoroutineTestRule()
    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            CreateShippingLabelFragmentArgs(order.getIdentifier())
        )
    )

    private lateinit var viewModel: CreateShippingLabelViewModel

    @Before
    fun setup() {
        stateFlow = MutableStateFlow(Transition(Idle, NoOp))
        whenever(stateMachine.transitions).thenReturn(stateFlow)

        viewModel = spy(
            CreateShippingLabelViewModel(
                savedState,
                coroutinesTestRule.testDispatchers,
                parameterRepository,
                orderDetailRepository,
                shippingLabelRepository,
                stateMachine,
                addressValidator,
                site,
                wooStore,
                accountStore,
                resourceProvider,
                currencyFormatter
            )
        )

        clearInvocations(
            viewModel,
            savedState,
            orderDetailRepository,
            stateMachine
        )
    }

    @Test
    fun `Displays create shipping label view correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        assertThat(viewState).isEqualTo(ViewState())
    }

    @Test
    fun `Displays data-loaded state correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val expectedViewState = ViewState(
            originAddressStep = originAddressCurrent,
            shippingAddressStep = shippingAddressNotDone,
            packagingDetailsStep = otherNotDone,
            customsStep = otherNotDone,
            carrierStep = otherNotDone,
            paymentStep = otherNotDone
        )

        stateFlow.value = Transition(State.WaitingForInput(data), null)

        assertThat(viewState).isEqualTo(expectedViewState)
    }

    @Test
    fun `Displays origin-address validated state correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val expectedViewState = ViewState(
            originAddressStep = originAddressDone,
            shippingAddressStep = shippingAddressCurrent,
            packagingDetailsStep = otherNotDone,
            customsStep = otherNotDone,
            carrierStep = otherNotDone,
            paymentStep = otherNotDone
        )

        val newStepsState = data.stepsState.copy(
            originAddressStep = data.stepsState.originAddressStep.copy(status = DONE, data = originAddressValidated),
            shippingAddressStep = data.stepsState.shippingAddressStep.copy(status = READY)
        )
        val newData = data.copy(stepsState = newStepsState)
        stateFlow.value = Transition(State.WaitingForInput(newData), null)

        assertThat(viewState).isEqualTo(expectedViewState)
    }

    @Test
    fun `Displays shipping-address validated state correctly`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        var viewState: ViewState? = null
        viewModel.viewStateData.observeForever { _, new -> viewState = new }

        val expectedViewState = ViewState(
            originAddressStep = originAddressDone,
            shippingAddressStep = shippingAddressDone,
            packagingDetailsStep = otherCurrent,
            customsStep = otherNotDone,
            carrierStep = otherNotDone,
            paymentStep = otherNotDone
        )

        val newStepsState = data.stepsState.copy(
            originAddressStep = data.stepsState.originAddressStep.copy(status = DONE, data = originAddressValidated),
            shippingAddressStep = data.stepsState.shippingAddressStep.copy(
                status = DONE,
                data = shippingAddressValidated
            ),
            packagingStep = data.stepsState.packagingStep.copy(status = READY)
        )

        val newData = data.copy(stepsState = newStepsState)
        stateFlow.value = Transition(State.WaitingForInput(newData), null)

        assertThat(viewState).isEqualTo(expectedViewState)
    }

    @Test
    fun `Continue click in origin address triggers validation`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        stateFlow.value = Transition(State.WaitingForInput(data), null)

        viewModel.onContinueButtonTapped(FlowStep.ORIGIN_ADDRESS)

        verify(stateMachine).handleEvent(OriginAddressValidationStarted)

        stateFlow.value = Transition(State.OriginAddressValidation(data), null)

        verify(addressValidator).validateAddress(originAddress, ORIGIN)
    }

    @Test
    fun `Purchase a label successfully`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val purchasedLabels = listOf(
            OrderTestUtils.generateShippingLabel(
                remoteOrderId = order.remoteOrderId, shippingLabelId = 1
            )
        )
        whenever(shippingLabelRepository.purchaseLabels(any(), any(), any(), any(), any()))
            .thenReturn(WooResult(purchasedLabels))

        viewModel.onPurchaseButtonClicked(fulfillOrder = false)
        stateFlow.value = Transition(PurchaseLabels(doneData, fulfillOrder = false), null)

        verify(stateMachine).handleEvent(PurchaseSuccess(purchasedLabels))
    }

    @Test
    fun `Show print screen after purchase`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val purchasedLabels = listOf(
            OrderTestUtils.generateShippingLabel(
                remoteOrderId = order.remoteOrderId, shippingLabelId = 1
            )
        )

        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            event = it
        }

        stateFlow.value = Transition(State.Idle, SideEffect.ShowLabelsPrint(doneData.order.remoteId, purchasedLabels))

        assertThat(event).isEqualTo(ShowPrintShippingLabels(doneData.order.remoteId, purchasedLabels))
    }

    @Test
    fun `fulfill order after successful purchase`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val purchasedLabels = listOf(
            OrderTestUtils.generateShippingLabel(
                remoteOrderId = order.remoteOrderId, shippingLabelId = 1
            )
        )
        whenever(shippingLabelRepository.purchaseLabels(any(), any(), any(), any(), any()))
            .thenReturn(WooResult(purchasedLabels))
        whenever(orderDetailRepository.updateOrderStatus(any(), any(), any()))
            .thenReturn(true)

        viewModel.onPurchaseButtonClicked(fulfillOrder = true)
        stateFlow.value = Transition(PurchaseLabels(doneData, fulfillOrder = true), null)

        verify(orderDetailRepository).updateOrderStatus(
            doneData.order.identifier.toIdSet().id, doneData.order.remoteId, CoreOrderStatus.COMPLETED.value
        )
    }

    @Test
    fun `notify user if fulfilment fail after successful purchase`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val purchasedLabels = listOf(
                OrderTestUtils.generateShippingLabel(
                    remoteOrderId = order.remoteOrderId, shippingLabelId = 1
                )
            )
            whenever(shippingLabelRepository.purchaseLabels(any(), any(), any(), any(), any()))
                .thenReturn(WooResult(purchasedLabels))
            whenever(orderDetailRepository.updateOrderStatus(any(), any(), any()))
                .thenReturn(false)

            var event: MultiLiveEvent.Event? = null
            viewModel.event.observeForever {
                event = it
            }

            viewModel.onPurchaseButtonClicked(fulfillOrder = true)
            stateFlow.value = Transition(PurchaseLabels(doneData, fulfillOrder = true), null)

            assertThat(event).isEqualTo(ShowSnackbar(R.string.shipping_label_create_purchase_fulfill_error))
        }
}
