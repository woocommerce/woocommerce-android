package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.util.Log
import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.State.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Data.*
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
                transitionTo(OriginAddressValidation, ValidateOriginAddress(event.originAddress))
            }
            on<DataLoadingFailed> { event ->
                transitionTo(DataLoadingFailure, ShowError(DataLoadingError))
            }
        }

        state<OriginAddressValidation> {
            on<OriginAddressValidated> { event ->
                transitionTo(ShippingAddressValidation, ValidateShippingAddress(event.address))
            }
            on<OriginAddressInvalid> {
                transitionTo(OriginAddressSuggestions, ShowOriginAddressSuggestions)
            }
            on<OriginAddressNotRecognized> {
                transitionTo(OriginAddressEditing, OpenOriginAddressEditor)
            }
        }

        state<OriginAddressSuggestions> {
            on<SuggestedOriginAddressSelected> { event ->
                transitionTo(OriginAddressValidation, ValidateOriginAddress(event.address))
            }
            on<SuggestedOriginAddressRejected> {
                transitionTo(OriginAddressEditing, OpenOriginAddressEditor)
            }
        }

        state<OriginAddressEditing> {
            on<OriginAddressUpdated> { event ->
                transitionTo(OriginAddressValidation, ValidateOriginAddress(event.address))
            }
            on<OriginAddressUsedAsIs> { event ->
                transitionTo(ShippingAddressValidation, ValidateShippingAddress(event.address))
            }
        }

        state<ShippingAddressValidation> {
            on<ShippingAddressValidated> { event ->
                transitionTo(PackageSelectionStep, UpdateViewState(event.address))
            }
            on<ShippingAddressInvalid> {
                transitionTo(ShippingAddressSuggestions, ShowShippingAddressSuggestions)
            }
            on<ShippingAddressNotRecognized> {
                transitionTo(ShippingAddressEditing, OpenShippingAddressEditor)
            }
        }

        state<OriginAddressSuggestions> {
            on<SuggestedShippingAddressSelected> { event ->
                transitionTo(ShippingAddressValidation, ValidateShippingAddress(event.address))
            }
            on<SuggestedShippingAddressRejected> {
                transitionTo(ShippingAddressEditing, OpenShippingAddressEditor)
            }
        }

        state<ShippingAddressEditing> {
            on<ShippingAddressUpdated> { event ->
                transitionTo(ShippingAddressValidation, ValidateShippingAddress(event.address))
            }
            on<ShippingAddressUsedAsIs> { event ->
                transitionTo(ShippingAddressValidation, ValidateShippingAddress(event.address))
            }
        }

        state<PackageSelectionStep> {
            on<ContinueToPackagingDetailsTapped> {
                transitionTo(PackageDetailsSelection, ShowPackagingDetails)
            }
            on<EditOriginAddressTapped> {
                transitionTo(OriginAddressEditing, OpenOriginAddressEditor)
            }
            on<EditShippingAddressTapped> {
                transitionTo(ShippingAddressEditing, OpenShippingAddressEditor)
            }
        }

        onTransition { transition ->
            if (transition is StateMachine.Transition.Valid) {
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

    class Data {
        private val dataMap = mutableMapOf<DataType.Key<*>, DataType>()

        @Suppress("UNCHECKED_CAST")
        operator fun <E : DataType> get(key: DataType.Key<E>): E? =
            dataMap[key] as? E

        operator fun <E : DataType> set(key: DataType.Key<E>, value: E) {
            dataMap[key] = value
        }

        sealed class DataType {
            interface Key<E : DataType>

            data class Origin(val address: Address) : DataType() {
                companion object Key : DataType.Key<Origin>
            }

            data class Destination(val address: Address) : DataType() {
                companion object Key : DataType.Key<Destination>
            }
        }
    }

    sealed class Error {
        object DataLoadingError : Error()
    }

    sealed class State {
        object Idle : State()
        object DataLoading : State()
        object DataLoadingFailure : State()

        object OriginAddressValidation : State()
        object OriginAddressSuggestions : State()
        object OriginAddressEditing : State()

        object ShippingAddressValidation : State()
        object ShippingAddressSuggestions : State()
        object ShippingAddressEditing : State()

        object PackageSelectionStep : State()
        object PackageDetailsSelection : State()
    }

    sealed class Event {
        data class FlowStarted(val orderId: String) : Event()
        data class DataLoaded(val originAddress: Address, val shippingAddress: Address) : Event()
        object DataLoadingFailed : Event()

        data class OriginAddressValidated(val address: Address) : Event()
        object OriginAddressNotRecognized : Event()
        object OriginAddressInvalid : Event()
        data class OriginAddressUsedAsIs(val address: Address) : Event()
        data class OriginAddressUpdated(val address: Address) : Event()
        object EditOriginAddressTapped : Event()
        data class SuggestedOriginAddressSelected(val address: Address) : Event()
        object SuggestedOriginAddressRejected : Event()

        data class ShippingAddressValidated(val address: Address) : Event()
        object ShippingAddressNotRecognized : Event()
        object ShippingAddressInvalid : Event()
        data class ShippingAddressUsedAsIs(val address: Address) : Event()
        data class ShippingAddressUpdated(val address: Address) : Event()
        object EditShippingAddressTapped : Event()
        data class SuggestedShippingAddressSelected(val address: Address) : Event()
        object SuggestedShippingAddressRejected : Event()

        object ContinueToPackagingDetailsTapped : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        data class LoadData(val orderId: String) : SideEffect()
        data class ShowError(val error: Error) : SideEffect()

        data class ValidateOriginAddress(val address: Address) : SideEffect()
        object ShowOriginAddressSuggestions : SideEffect()
        object OpenOriginAddressEditor : SideEffect()

        data class ValidateShippingAddress(val address: Address) : SideEffect()
        object ShowShippingAddressSuggestions : SideEffect()
        object OpenShippingAddressEditor : SideEffect()

        object ShowPackagingDetails : SideEffect()
        data class UpdateViewState(val address: Address) : SideEffect()
    }
}
