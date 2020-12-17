package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Data
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressChangeSuggested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressEditCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidationFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class CreateShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val stateMachine: ShippingLabelsStateMachine,
    private val addressValidator: ShippingLabelAddressValidator
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val STATE_KEY = "state"
    }
    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        initializeStateMachine()
    }

    private fun initializeStateMachine() {
        launch {
            stateMachine.transitions.collect { transition ->
                transition.sideEffect?.let { sideEffect ->
                    when (sideEffect) {
                        SideEffect.NoOp -> {
                        }
                        is SideEffect.ShowError -> showError(sideEffect.error)
                        is SideEffect.UpdateViewState -> updateViewState(sideEffect.data)
                        is SideEffect.LoadData -> handleResult { loadData(sideEffect.orderId) }
                        is SideEffect.ValidateAddress -> handleResult {
                            validateAddress(sideEffect.address, sideEffect.type)
                        }
                        is SideEffect.OpenAddressEditor -> triggerEvent(
                            ShowAddressEditor(
                                sideEffect.address,
                                sideEffect.type,
                                sideEffect.validationResult
                            )
                        )
                        is SideEffect.ShowAddressSuggestion -> handleResult {
                            Event.SuggestedAddressSelected(sideEffect.suggested)
                        }
                        is SideEffect.ShowPackageOptions -> Event.PackagesSelected
                        is SideEffect.ShowCustomsForm -> Event.CustomsFormFilledOut
                        is SideEffect.ShowCarrierOptions -> Event.ShippingCarrierSelected
                        is SideEffect.ShowPaymentDetails -> Event.PaymentSelected
                    }
                }
                // save the current state
                savedState[STATE_KEY] = transition.state
            }
        }

        val state = savedState.get<State>(STATE_KEY)
        if (state != null) {
            stateMachine.initialize(state)
        } else {
            stateMachine.start(arguments.orderIdentifier)
        }
    }

    private suspend fun handleResult(action: suspend () -> Event) {
        viewState = viewState.copy(isProgressDialogVisible = true)
        stateMachine.handleEvent(action())
        viewState = viewState.copy(isProgressDialogVisible = false)
    }

    private fun updateViewState(data: Data) {
        viewState = when (data.flowSteps.maxBy { it.ordinal } ?: FlowStep.ORIGIN_ADDRESS) {
            FlowStep.ORIGIN_ADDRESS -> {
                viewState.copy(
                    originAddressStep = Step.current(data.originAddress.toString()),
                    shippingAddressStep = Step.notDone(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.notDone(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            FlowStep.SHIPPING_ADDRESS -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.current(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.notDone(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            FlowStep.PACKAGING -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.current(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            FlowStep.CUSTOMS -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.current(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            FlowStep.CARRIER -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.done(),
                    carrierStep = Step.current(),
                    paymentStep = Step.notDone()
                )
            }
            FlowStep.PAYMENT -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.done(),
                    carrierStep = Step.done(),
                    paymentStep = Step.current()
                )
            }
            FlowStep.DONE -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.done(),
                    carrierStep = Step.done(),
                    paymentStep = Step.done()
                )
            }
        }
    }

    private fun showError(error: Error) {
        when (error) {
            DataLoadingError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            AddressValidationError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
        }
    }

    private fun loadData(orderId: String): Event {
        val order = requireNotNull(orderDetailRepository.getOrder(orderId))
        return Event.DataLoaded(order.billingAddress, order.shippingAddress)
    }

    private suspend fun validateAddress(address: Address, type: AddressType): Event {
        return when (val result = addressValidator.validateAddress(address, type)) {
            ValidationResult.Valid -> AddressValidated(address)
            is ValidationResult.SuggestedChanges -> AddressChangeSuggested(result.suggested)
            is ValidationResult.NotFound, is ValidationResult.Invalid -> AddressInvalid(address, result)
            is ValidationResult.Error -> AddressValidationFailed
        }
    }

    fun onAddressEditConfirmed(address: Address) {
        stateMachine.handleEvent(AddressValidated(address))
    }

    fun onAddressEditCanceled() {
        stateMachine.handleEvent(AddressEditCanceled)
    }

    fun onEditButtonTapped(step: FlowStep) {
        when (step) {
            FlowStep.ORIGIN_ADDRESS -> Event.EditOriginAddressRequested
            FlowStep.SHIPPING_ADDRESS -> Event.EditShippingAddressRequested
            FlowStep.PACKAGING -> Event.EditPackagingRequested
            FlowStep.CUSTOMS -> Event.EditCustomsRequested
            FlowStep.CARRIER -> Event.EditShippingCarrierRequested
            FlowStep.PAYMENT -> Event.EditPaymentRequested
            FlowStep.DONE -> null
        }.also { event ->
            event?.let {
                stateMachine.handleEvent(it)
            }
        }
    }

    fun onContinueButtonTapped(step: FlowStep) {
        when (step) {
            FlowStep.ORIGIN_ADDRESS -> Event.OriginAddressValidationStarted
            FlowStep.SHIPPING_ADDRESS -> Event.ShippingAddressValidationStarted
            FlowStep.PACKAGING -> Event.PackageSelectionStarted
            FlowStep.CUSTOMS -> Event.CustomsDeclarationStarted
            FlowStep.CARRIER -> Event.ShippingCarrierSelectionStarted
            FlowStep.PAYMENT -> Event.PaymentSelectionStarted
            FlowStep.DONE -> null
        }.also { event ->
            event?.let { stateMachine.handleEvent(it) }
        }
    }

    @Parcelize
    data class ViewState(
        val originAddressStep: Step? = null,
        val shippingAddressStep: Step? = null,
        val packagingDetailsStep: Step? = null,
        val customsStep: Step? = null,
        val carrierStep: Step? = null,
        val paymentStep: Step? = null,
        val isProgressDialogVisible: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class Step(
        val details: String? = null,
        val isEnabled: Boolean? = null,
        val isContinueButtonVisible: Boolean? = null,
        val isEditButtonVisible: Boolean? = null,
        val isHighlighted: Boolean? = null
    ) : Parcelable {
        companion object {
            fun notDone(newDetails: String? = null) = Step(
                details = newDetails,
                isEnabled = false,
                isContinueButtonVisible = false,
                isEditButtonVisible = false,
                isHighlighted = false
            )

            fun current(newDetails: String? = null) = Step(
                details = newDetails,
                isEnabled = true,
                isContinueButtonVisible = true,
                isEditButtonVisible = false,
                isHighlighted = true
            )

            fun done(newDetails: String? = null) = Step(
                details = newDetails,
                isEnabled = true,
                isContinueButtonVisible = false,
                isEditButtonVisible = true,
                isHighlighted = false
            )
        }
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<CreateShippingLabelViewModel>
}
