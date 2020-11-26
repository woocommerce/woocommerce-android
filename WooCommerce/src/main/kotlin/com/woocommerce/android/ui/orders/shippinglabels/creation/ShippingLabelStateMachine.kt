package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.util.Log
import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error.*
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
                val data = Data(event.originAddress, event.shippingAddress)
                transitionTo(WaitingForUser(data), UpdateViewState(data))
            }
            on<DataLoadingFailed> {
                transitionTo(DataLoadingFailure, ShowError(DataLoadingError))
            }
        }

        state<WaitingForUser> {
            on<OriginAddressValidationStarted> {
                transitionTo(OriginAddressValidation(data), ValidateOriginAddress(data.originAddress))
            }
            on<ShippingAddressValidationStarted> {
                transitionTo(ShippingAddressValidation(data), ValidateShippingAddress(data.shippingAddress))
            }
            on<PackageSelectionStarted> {
                transitionTo(PackageSelection(data), ShowPackagingDetails)
            }
            on<EditOriginAddressTapped> {
                transitionTo(OriginAddressEditing(data), OpenOriginAddressEditor(data.originAddress))
            }
            on<EditShippingAddressTapped> {
                transitionTo(ShippingAddressEditing(data), OpenShippingAddressEditor(data.shippingAddress))
            }
        }

        state<OriginAddressValidation> {
            on<OriginAddressValidated> { event ->
                val newData = data.copy(originAddress = event.address, isOriginAddressValidated = true)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<OriginAddressInvalid> {
                transitionTo(OriginAddressSuggestions(data), ShowOriginAddressSuggestions)
            }
            on<OriginAddressNotRecognized> {
                transitionTo(OriginAddressEditing(data), OpenOriginAddressEditor(data.originAddress))
            }
        }

        state<OriginAddressSuggestions> {
            on<SuggestedOriginAddressSelected> { event ->
                val newData = data.copy(originAddress = event.address, isOriginAddressValidated = true)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<SuggestedOriginAddressRejected> {
                transitionTo(OriginAddressEditing(data), OpenOriginAddressEditor(data.originAddress))
            }
        }

        state<OriginAddressEditing> {
            on<OriginAddressUpdated> { event ->
                transitionTo(OriginAddressValidation(data), ValidateOriginAddress(event.address))
            }
            on<OriginAddressUsedAsIs> { event ->
                val newData = data.copy(originAddress = event.address, isOriginAddressValidated = true)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<ShippingAddressValidation> {
            on<ShippingAddressValidated> { event ->
                val newData = data.copy(shippingAddress = event.address, isShippingAddressValidated = true)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<ShippingAddressInvalid> {
                transitionTo(ShippingAddressSuggestions(data), ShowShippingAddressSuggestions)
            }
            on<ShippingAddressNotRecognized> {
                transitionTo(ShippingAddressEditing(data), OpenShippingAddressEditor(data.shippingAddress))
            }
        }

        state<OriginAddressSuggestions> {
            on<SuggestedShippingAddressSelected> { event ->
                val newData = data.copy(shippingAddress = event.address, isShippingAddressValidated = true)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
            on<SuggestedShippingAddressRejected> {
                transitionTo(ShippingAddressEditing(data), OpenShippingAddressEditor(data.shippingAddress))
            }
        }

        state<ShippingAddressEditing> {
            on<ShippingAddressUpdated> { event ->
                transitionTo(ShippingAddressValidation(data), ValidateShippingAddress(event.address))
            }
            on<ShippingAddressUsedAsIs> { event ->
                val newData = data.copy(shippingAddress = event.address, isShippingAddressValidated = true)
                transitionTo(WaitingForUser(newData), UpdateViewState(newData))
            }
        }

        state<PackageSelection> {
        }

        onTransition { transition ->
            if (transition is StateMachine.Transition.Valid) {
                Log.d("onko: State", transition.toState.toString())
                transition.sideEffect?.let { sideEffect ->
                    _effects.value = sideEffect
                }
            } else {
                Log.e(TAG,"Invalid event ${transition.event} passed from ${transition.fromState}")
            }
        }
    }

    fun start(orderId: String) {
        stateMachine.transition(FlowStarted(orderId))
    }

    fun handleEvent(event: Event) {
        stateMachine.transition(event)
    }

    data class Data(
        val originAddress: Address,
        val shippingAddress: Address,
        val isOriginAddressValidated: Boolean = false,
        val isShippingAddressValidated: Boolean = false
    )

    sealed class Error {
        object DataLoadingError : Error()
    }

    sealed class State {
        object Idle : State()
        object DataLoadingFailure : State()
        object DataLoading : State()
        data class WaitingForUser(val data: Data) : State()

        data class OriginAddressValidation(val data: Data) : State()
        data class OriginAddressSuggestions(val data: Data) : State()
        data class OriginAddressEditing(val data: Data) : State()

        data class ShippingAddressValidation(val data: Data) : State()
        data class ShippingAddressSuggestions(val data: Data) : State()
        data class ShippingAddressEditing(val data: Data) : State()

        data class PackageSelection(val data: Data) : State()
    }

    sealed class Event {
        data class FlowStarted(val orderId: String) : Event()
        data class DataLoaded(val originAddress: Address, val shippingAddress: Address) : Event()
        object DataLoadingFailed : Event()

        object OriginAddressValidationStarted : Event()
        data class OriginAddressValidated(val address: Address) : Event()
        object OriginAddressNotRecognized : Event()
        object OriginAddressInvalid : Event()
        data class OriginAddressUsedAsIs(val address: Address) : Event()
        data class OriginAddressUpdated(val address: Address) : Event()
        object EditOriginAddressTapped : Event()
        data class SuggestedOriginAddressSelected(val address: Address) : Event()
        object SuggestedOriginAddressRejected : Event()

        object ShippingAddressValidationStarted : Event()
        data class ShippingAddressValidated(val address: Address) : Event()
        object ShippingAddressNotRecognized : Event()
        object ShippingAddressInvalid : Event()
        data class ShippingAddressUsedAsIs(val address: Address) : Event()
        data class ShippingAddressUpdated(val address: Address) : Event()
        object EditShippingAddressTapped : Event()
        data class SuggestedShippingAddressSelected(val address: Address) : Event()
        object SuggestedShippingAddressRejected : Event()

        object PackageSelectionStarted : Event()
        object EditPackagingAddressTapped : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        data class LoadData(val orderId: String) : SideEffect()
        data class ShowError(val error: Error) : SideEffect()
        data class UpdateViewState(val data: Data) : SideEffect()

        data class ValidateOriginAddress(val address: Address) : SideEffect()
        object ShowOriginAddressSuggestions : SideEffect()
        data class OpenOriginAddressEditor(val address: Address) : SideEffect()

        data class ValidateShippingAddress(val address: Address) : SideEffect()
        object ShowShippingAddressSuggestions : SideEffect()
        data class OpenShippingAddressEditor(val address: Address) : SideEffect()

        object ShowPackagingDetails : SideEffect()
    }
}
