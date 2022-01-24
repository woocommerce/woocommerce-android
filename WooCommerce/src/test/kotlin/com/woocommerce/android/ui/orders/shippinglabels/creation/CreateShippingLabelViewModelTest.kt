package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPrintShippingLabels
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.NoOp
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.*
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult
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
    private val orderMapper = OrderMapper(
        getLocations = mock {
            on { invoke(any(), any()) } doReturn (Location.EMPTY to AmbiguousLocation.EMPTY)
        }
    )

    private val data = StateMachineData(
        order = orderMapper.toAppModel(order),
        stepsState = StepsState(
            originAddressStep = OriginAddressStep(READY, originAddress),
            shippingAddressStep = ShippingAddressStep(NOT_READY, shippingAddress),
            packagingStep = PackagingStep(NOT_READY, emptyList()),
            customsStep = CustomsStep(
                NOT_READY,
                isVisible = originAddress.country != shippingAddress.country,
                data = null
            ),
            carrierStep = CarrierStep(NOT_READY, emptyList()),
            paymentsStep = PaymentsStep(NOT_READY, null)
        )
    )

    private val doneData = StateMachineData(
        order = orderMapper.toAppModel(order),
        stepsState = StepsState(
            originAddressStep = OriginAddressStep(READY, originAddress),
            shippingAddressStep = ShippingAddressStep(READY, shippingAddress),
            packagingStep = PackagingStep(
                READY,
                listOf(CreateShippingLabelTestUtils.generateShippingLabelPackage())
            ),
            customsStep = CustomsStep(
                NOT_READY,
                isVisible = originAddress.country != shippingAddress.country,
                data = null
            ),
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

    private val otherInvisible = StepUiState(
        isVisible = false
    )

    private val otherCurrent = StepUiState(
        isEnabled = true,
        isContinueButtonVisible = true,
        isEditButtonVisible = false,
        isHighlighted = true
    )

    private val savedState = CreateShippingLabelFragmentArgs(order.remoteOrderId.value).initSavedStateHandle()

    private lateinit var viewModel: CreateShippingLabelViewModel

    @Before
    fun setup() {
        stateFlow = MutableStateFlow(Transition(Idle, NoOp))
        whenever(stateMachine.transitions).thenReturn(stateFlow)

        viewModel = spy(
            CreateShippingLabelViewModel(
                savedState,
                parameterRepository,
                orderDetailRepository,
                shippingLabelRepository,
                stateMachine,
                addressValidator,
                site,
                wooStore,
                accountStore,
                resourceProvider,
                currencyFormatter,
                mock()
            )
        )

        clearInvocations(
            viewModel,
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
            customsStep = otherInvisible,
            carrierStep = otherNotDone,
            paymentStep = otherNotDone
        )

        stateFlow.value = Transition(WaitingForInput(data), null)

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
            customsStep = otherInvisible,
            carrierStep = otherNotDone,
            paymentStep = otherNotDone
        )

        val newStepsState = data.stepsState.copy(
            originAddressStep = data.stepsState.originAddressStep.copy(status = DONE, data = originAddressValidated),
            shippingAddressStep = data.stepsState.shippingAddressStep.copy(status = READY)
        )
        val newData = data.copy(stepsState = newStepsState)
        stateFlow.value = Transition(WaitingForInput(newData), null)

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
            customsStep = otherInvisible,
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
        stateFlow.value = Transition(WaitingForInput(newData), null)

        assertThat(viewState).isEqualTo(expectedViewState)
    }

    @Test
    fun `Continue click in origin address triggers validation`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        stateFlow.value = Transition(WaitingForInput(data), null)

        viewModel.onContinueButtonTapped(FlowStep.ORIGIN_ADDRESS)

        verify(stateMachine).handleEvent(OriginAddressValidationStarted)

        stateFlow.value = Transition(State.OriginAddressValidation(data), null)

        verify(addressValidator).validateAddress(originAddress, ORIGIN, requiresPhoneNumber = false)
    }

    @Test
    fun `when the address validation has a non trivial suggestion, then show address suggestions screen`() =
        testBlocking {
            val suggestedAddress = originAddress.copy(address1 = "Suggested street")
            whenever(addressValidator.validateAddress(any(), any(), any()))
                .thenReturn(ValidationResult.SuggestedChanges(suggestedAddress, isTrivial = false))

            stateFlow.value = Transition(State.OriginAddressValidation(data), null)

            verify(stateMachine).handleEvent(Event.AddressChangeSuggested(suggestedAddress))
        }

    @Test
    fun `when the address validation has trivial suggestion, then use the suggested address`() = testBlocking {
        val suggestedAddress = originAddress.copy(address1 = "Suggested street")
        whenever(addressValidator.validateAddress(any(), any(), any()))
            .thenReturn(ValidationResult.SuggestedChanges(suggestedAddress, isTrivial = true))

        stateFlow.value = Transition(State.OriginAddressValidation(data), null)

        verify(stateMachine).handleEvent(AddressValidated(suggestedAddress))
    }

    @Test
    fun `Purchase a label successfully`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val purchasedLabels = listOf(
            OrderTestUtils.generateShippingLabel(shippingLabelId = 1)
        )
        whenever(shippingLabelRepository.purchaseLabels(any(), any(), any(), any(), any(), anyOrNull()))
            .thenReturn(WooResult(purchasedLabels))

        viewModel.onPurchaseButtonClicked(fulfillOrder = false)
        stateFlow.value = Transition(PurchaseLabels(doneData, fulfillOrder = false), null)

        verify(stateMachine).handleEvent(PurchaseSuccess(purchasedLabels))
    }

    @Test
    fun `Show print screen after purchase`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val purchasedLabels = listOf(
            OrderTestUtils.generateShippingLabel(shippingLabelId = 1)
        )

        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever {
            event = it
        }

        stateFlow.value = Transition(Idle, SideEffect.ShowLabelsPrint(doneData.order.id, purchasedLabels))

        assertThat(event).isEqualTo(ShowPrintShippingLabels(doneData.order.id, purchasedLabels))
    }

    @Test
    fun `fulfill order after successful purchase`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val purchasedLabels = listOf(
            OrderTestUtils.generateShippingLabel(shippingLabelId = 1)
        )
        whenever(shippingLabelRepository.purchaseLabels(any(), any(), any(), any(), any(), anyOrNull()))
            .thenReturn(WooResult(purchasedLabels))
        whenever(orderDetailRepository.updateOrderStatus(any(), any())).thenReturn(
            flow {
                UpdateOrderResult.OptimisticUpdateResult(mock())
                UpdateOrderResult.RemoteUpdateResult(mock())
            }
        )

        viewModel.onPurchaseButtonClicked(fulfillOrder = true)
        stateFlow.value = Transition(PurchaseLabels(doneData, fulfillOrder = true), null)

        verify(orderDetailRepository).updateOrderStatus(
            doneData.order.id, CoreOrderStatus.COMPLETED.value
        )
    }

    @Test
    fun `notify user if fulfilment fail after successful purchase`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val purchasedLabels = listOf(
                OrderTestUtils.generateShippingLabel(shippingLabelId = 1)
            )
            whenever(shippingLabelRepository.purchaseLabels(any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(WooResult(purchasedLabels))
            whenever(orderDetailRepository.updateOrderStatus(any(), any())).thenReturn(
                flow {
                    emit(UpdateOrderResult.OptimisticUpdateResult(mock()))
                    val onOrderChangedWithError = mock<OnOrderChanged>()
                        .apply { whenever(this.isError).thenReturn(true) }
                    emit(UpdateOrderResult.RemoteUpdateResult(onOrderChangedWithError))
                }
            )

            viewModel.onPurchaseButtonClicked(fulfillOrder = true)
            stateFlow.value = Transition(PurchaseLabels(doneData, fulfillOrder = true), null)
            advanceUntilIdle()

            assertThat(viewModel.event.value)
                .isEqualTo(ShowSnackbar(R.string.shipping_label_create_purchase_fulfill_error))
        }

    @Test
    fun `given there are no changes, when the user tries to exit, then don't warn them`() = testBlocking {
        stateFlow.value = Transition(WaitingForInput(data), null)

        viewModel.onBackButtonClicked()

        assertThat(viewModel.event.value).isEqualTo(Exit)
    }

    @Test
    fun `given there are some changes changes, when the user tries to exit, then display a discard changes dialog`() =
        testBlocking {
            val stateMachineData = data.copy(
                stepsState = data.stepsState.copy(
                    originAddressStep = data.stepsState.originAddressStep.copy(status = DONE)
                )
            )
            stateFlow.value = Transition(WaitingForInput(stateMachineData), null)

            viewModel.onBackButtonClicked()

            assertThat(viewModel.event.value).isInstanceOf(ShowDialog::class.java)
        }
}
