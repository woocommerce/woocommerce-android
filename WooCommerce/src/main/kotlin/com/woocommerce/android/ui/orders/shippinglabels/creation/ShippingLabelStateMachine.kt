package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.util.Log
import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressEditFinished
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressNotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressUsedAsIs
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.CustomsDeclarationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.CustomsFormFilledOut
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.DataLoadingFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditCustomsRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditOriginAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditPackagingRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditPaymentRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditShippingAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditShippingCarrierRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.FlowStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.OriginAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PackageSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PackagesSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PaymentSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PaymentSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingCarrierSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingCarrierSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.SuggestedAddressSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.CARRIER
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.CUSTOMS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.DONE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.ORIGIN_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.PACKAGING
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.PAYMENT
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep.SHIPPING_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.LoadData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.NoOp
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.OpenAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowCarrierOptions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowCustomsForm
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowPackageOptions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowPaymentDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.UpdateViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ValidateAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.CustomsDeclaration
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.DataLoading
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.DataLoadingFailure
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.Idle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.OriginAddressEditing
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.OriginAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.OriginAddressValidation
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.PackageSelection
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.PaymentSelection
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.ShippingAddressEditing
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.ShippingAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.ShippingAddressValidation
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.ShippingCarrierSelection
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.WaitingForUser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ShippingLabelStateMachine @Inject constructor() {
    companion object {
        private val TAG = ShippingLabelStateMachine::class.simpleName
    }

    private val _effects = MutableStateFlow<SideEffect>(NoOp)
    val effects: StateFlow<SideEffect> = _effects

    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(Idle)

        state<Idle> {
            on<FlowStarted> { event ->
                transitionTo(DataLoading, LoadData(event.orderId))
            }
        }

        state<DataLoading> {
            on<DataLoaded> { event ->
                val data = Data(event.originAddress, event.shippingAddress, setOf(ORIGIN_ADDRESS))
                transitionTo(WaitingForUser(data), UpdateViewState(data))
            }
            on<DataLoadingFailed> {
                transitionTo(DataLoadingFailure, ShowError(DataLoadingError))
            }
        }

        state<WaitingForUser> {
            on<OriginAddressValidationStarted> {
                transitionTo(OriginAddressValidation(data), ValidateAddress(data.originAddress))
            }
            on<ShippingAddressValidationStarted> {
                transitionTo(ShippingAddressValidation(data), ValidateAddress(data.shippingAddress))
            }
            on<PackageSelectionStarted> {
                transitionTo(PackageSelection(data), ShowPackageOptions)
            }
            on<CustomsDeclarationStarted> {
                transitionTo(CustomsDeclaration(data), ShowCustomsForm)
            }
            on<ShippingCarrierSelectionStarted> {
                transitionTo(ShippingCarrierSelection(data), ShowCarrierOptions)
            }
            on<PaymentSelectionStarted> {
                transitionTo(PaymentSelection(data), ShowPaymentDetails)
            }
            on<EditOriginAddressRequested> {
                transitionTo(OriginAddressEditing(data), OpenAddressEditor(data.originAddress))
            }
            on<EditShippingAddressRequested> {
                transitionTo(ShippingAddressEditing(data), OpenAddressEditor(data.shippingAddress))
            }
            on<EditPackagingRequested> {
                transitionTo(PackageSelection(data), ShowPackageOptions)
            }
            on<EditCustomsRequested> {
                transitionTo(CustomsDeclaration(data), ShowCustomsForm)
            }
            on<EditShippingCarrierRequested> {
                transitionTo(ShippingCarrierSelection(data), ShowCarrierOptions)
            }
            on<EditPaymentRequested> {
                transitionTo(PaymentSelection(data), ShowPaymentDetails)
            }
        }

        state<OriginAddressValidation> {
            on<AddressValidated> { event ->
                val newData = data.copy(originAddress = event.address, stepsDone = data.stepsDone + SHIPPING_ADDRESS)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<AddressInvalid> { event ->
                transitionTo(
                    OriginAddressSuggestion(data),
                    ShowAddressSuggestion(data.originAddress, event.suggested)
                )
            }
            on<AddressNotRecognized> {
                transitionTo(OriginAddressEditing(data), OpenAddressEditor(data.originAddress))
            }
        }

        state<OriginAddressSuggestion> {
            on<SuggestedAddressSelected> { event ->
                val newData = data.copy(originAddress = event.address, stepsDone = data.stepsDone + SHIPPING_ADDRESS)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<EditOriginAddressRequested> {
                transitionTo(OriginAddressEditing(data), OpenAddressEditor(data.originAddress))
            }
        }

        state<OriginAddressEditing> {
            on<AddressEditFinished> { event ->
                transitionTo(OriginAddressValidation(data), ValidateAddress(event.address))
            }
            on<AddressUsedAsIs> { event ->
                val newData = data.copy(originAddress = event.address, stepsDone = data.stepsDone + SHIPPING_ADDRESS)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<ShippingAddressValidation> {
            on<AddressValidated> { event ->
                val newData = data.copy(shippingAddress = event.address, stepsDone = data.stepsDone + PACKAGING)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<AddressInvalid> { event ->
                transitionTo(
                    ShippingAddressSuggestion(data),
                    ShowAddressSuggestion(data.originAddress, event.suggested)
                )
            }
            on<AddressNotRecognized> {
                transitionTo(ShippingAddressEditing(data), OpenAddressEditor(data.shippingAddress))
            }
        }

        state<OriginAddressSuggestion> {
            on<SuggestedAddressSelected> { event ->
                val newData = data.copy(shippingAddress = event.address, stepsDone = data.stepsDone + PACKAGING)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<EditShippingAddressRequested> {
                transitionTo(ShippingAddressEditing(data), OpenAddressEditor(data.shippingAddress))
            }
        }

        state<ShippingAddressEditing> {
            on<AddressEditFinished> { event ->
                transitionTo(ShippingAddressValidation(data), ValidateAddress(event.address))
            }
            on<AddressUsedAsIs> { event ->
                val newData = data.copy(shippingAddress = event.address, stepsDone = data.stepsDone + PACKAGING)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<PackageSelection> {
            on<PackagesSelected> {
                val newData = data.copy(stepsDone = data.stepsDone + CUSTOMS)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<CustomsDeclaration> {
            on<CustomsFormFilledOut> {
                val newData = data.copy(stepsDone = data.stepsDone + CARRIER)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<ShippingCarrierSelection> {
            on<ShippingCarrierSelected> {
                val newData = data.copy(stepsDone = data.stepsDone + PAYMENT)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<PaymentSelection> {
            on<PaymentSelected> {
                val newData = data.copy(stepsDone = data.stepsDone + DONE)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        onTransition { transition ->
            if (transition is StateMachine.Transition.Valid) {
                Log.d(TAG, transition.toState.toString())
                transition.sideEffect?.let { sideEffect ->
                    _effects.value = sideEffect
                }
            } else {
                throw InvalidStateException("Unexpected event ${transition.event} passed from ${transition.fromState}")
            }
        }
    }

    fun start(orderId: String) {
        stateMachine.transition(FlowStarted(orderId))
    }

    fun handleEvent(event: Event) {
        Log.d(TAG, event.toString())
        stateMachine.transition(event)
    }

    data class Data(
        val originAddress: Address,
        val shippingAddress: Address,
        val stepsDone: Set<FlowStep>
    )

    enum class FlowStep {
        ORIGIN_ADDRESS, SHIPPING_ADDRESS, PACKAGING, CUSTOMS, CARRIER, PAYMENT, DONE
    }

    sealed class Error {
        object DataLoadingError : Error()
    }

    sealed class State {
        object Idle : State()
        object DataLoadingFailure : State()
        object DataLoading : State()
        data class WaitingForUser(val data: Data) : State()

        data class OriginAddressValidation(val data: Data) : State()
        data class OriginAddressSuggestion(val data: Data) : State()
        data class OriginAddressEditing(val data: Data) : State()

        data class ShippingAddressValidation(val data: Data) : State()
        data class ShippingAddressSuggestion(val data: Data) : State()
        data class ShippingAddressEditing(val data: Data) : State()

        data class PackageSelection(val data: Data) : State()
        data class CustomsDeclaration(val data: Data) : State()
        data class ShippingCarrierSelection(val data: Data) : State()
        data class PaymentSelection(val data: Data) : State()
    }

    sealed class Event {
        data class FlowStarted(val orderId: String) : Event()
        data class DataLoaded(val originAddress: Address, val shippingAddress: Address) : Event()
        object DataLoadingFailed : Event()

        object AddressNotRecognized : Event()
        data class AddressValidated(val address: Address) : Event()
        data class AddressInvalid(val suggested: Address) : Event()
        data class AddressUsedAsIs(val address: Address) : Event()
        data class AddressEditFinished(val address: Address) : Event()
        data class SuggestedAddressSelected(val address: Address) : Event()

        object OriginAddressValidationStarted : Event()
        object EditOriginAddressRequested : Event()

        object ShippingAddressValidationStarted : Event()
        object EditShippingAddressRequested : Event()

        object PackageSelectionStarted : Event()
        object EditPackagingRequested : Event()
        object PackagesSelected : Event()

        object CustomsDeclarationStarted : Event()
        object EditCustomsRequested : Event()
        object CustomsFormFilledOut : Event()

        object ShippingCarrierSelectionStarted : Event()
        object EditShippingCarrierRequested : Event()
        object ShippingCarrierSelected : Event()

        object PaymentSelectionStarted : Event()
        object EditPaymentRequested : Event()
        object PaymentSelected : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        data class LoadData(val orderId: String) : SideEffect()
        data class ShowError(val error: Error) : SideEffect()
        data class UpdateViewState(val data: Data) : SideEffect()

        data class ValidateAddress(val address: Address) : SideEffect()
        data class ShowAddressSuggestion(val entered: Address, val suggested: Address) : SideEffect()
        data class OpenAddressEditor(val address: Address) : SideEffect()

        object ShowPackageOptions : SideEffect()
        object ShowCustomsForm : SideEffect()
        object ShowCarrierOptions : SideEffect()
        object ShowPaymentDetails : SideEffect()
    }

    class InvalidStateException(message: String) : Exception(message)
}
