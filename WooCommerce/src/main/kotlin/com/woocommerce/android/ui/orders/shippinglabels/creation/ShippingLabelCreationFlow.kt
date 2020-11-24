package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.State.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class ShippingLabelCreationFlow @Inject constructor() {
    private val _effects = MutableStateFlow<SideEffect>(NoOp)
    val effects: StateFlow<SideEffect> = _effects

    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(Idle)

        state<Idle> {
            on<FlowStarted> {
                transitionTo(DataLoading, LoadData)
            }
        }

        state<DataLoading> {
            on<DataLoaded> {
                transitionTo(OriginAddressValidation, ValidateOriginAddress)
            }
            on<DataLoadingFailed> {
                transitionTo(DataLoadingError, ShowDataLoadingError)
            }
        }

        state<OriginAddressValidation> {
            on<OriginAddressValidated> {
                transitionTo(ShippingAddressValidation, ValidateShippingAddress)
            }
            on<OriginAddressInvalid> {
                transitionTo(OriginAddressSuggestions, ShowOriginAddressSuggestions)
            }
            on<OriginAddressNotRecognized> {
                transitionTo(OriginAddressEditing, OpenOriginAddressEditor)
            }
        }

        state<OriginAddressSuggestions> {
            on<SuggestedOriginAddressSelected> {
                transitionTo(OriginAddressValidation, ValidateOriginAddress)
            }
            on<SuggestedOriginAddressRejected> {
                transitionTo(OriginAddressEditing, OpenOriginAddressEditor)
            }
        }

        state<OriginAddressEditing> {
            on<OriginAddressUpdated> {
                transitionTo(OriginAddressValidation, ValidateOriginAddress)
            }
            on<OriginAddressUsedAsIs> {
                transitionTo(ShippingAddressValidation, ValidateShippingAddress)
            }
        }

        state<ShippingAddressValidation> {
            on<ShippingAddressValidated> {
                transitionTo(PackageSelection, ShowPackagingDetails)
            }
            on<ShippingAddressInvalid> {
                transitionTo(ShippingAddressSuggestions, ShowShippingAddressSuggestions)
            }
            on<ShippingAddressNotRecognized> {
                transitionTo(ShippingAddressEditing, OpenShippingAddressEditor)
            }
        }

        state<OriginAddressSuggestions> {
            on<SuggestedShippingAddressSelected> {
                transitionTo(ShippingAddressValidation, ValidateShippingAddress)
            }
            on<SuggestedShippingAddressRejected> {
                transitionTo(ShippingAddressEditing, OpenShippingAddressEditor)
            }
        }

        state<ShippingAddressEditing> {
            on<ShippingAddressUpdated> {
                transitionTo(ShippingAddressValidation, ValidateShippingAddress)
            }
            on<ShippingAddressUsedAsIs> {
                transitionTo(ShippingAddressValidation, ValidateShippingAddress)
            }
        }

        onTransition {
            val validTransition = it as? StateMachine.Transition.Valid ?: return@onTransition
            validTransition.sideEffect?.let { sideEffect ->
                _effects.value = sideEffect
            }
        }
    }

    fun start() {
        stateMachine.transition(FlowStarted)
    }

    fun handleEvent(event: Event) {
        stateMachine.transition(event)
    }

    sealed class Data {
        data class Origin(val address: Address) : Data()
        data class Destination(val address: Address) : Data()
    }

    sealed class State {
        object Idle : State()
        object DataLoading : State()
        object DataLoadingError : State()

        object OriginAddressValidation : State()
        object OriginAddressSuggestions : State()
        object OriginAddressEditing : State()

        object ShippingAddressValidation : State()
        object ShippingAddressSuggestions : State()
        object ShippingAddressEditing : State()

        object PackageSelection : State()
    }

    sealed class Event {
        object FlowStarted : Event()
        object DataLoaded : Event()
        object DataLoadingFailed : Event()

        object OriginAddressValidated : Event()
        object OriginAddressNotRecognized : Event()
        object OriginAddressInvalid : Event()
        object OriginAddressUsedAsIs : Event()
        object OriginAddressUpdated : Event()
        object SuggestedOriginAddressSelected : Event()
        object SuggestedOriginAddressRejected : Event()

        object ShippingAddressValidated : Event()
        object ShippingAddressNotRecognized : Event()
        object ShippingAddressInvalid : Event()
        object ShippingAddressUsedAsIs : Event()
        object ShippingAddressUpdated : Event()
        object SuggestedShippingAddressSelected : Event()
        object SuggestedShippingAddressRejected : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        object LoadData : SideEffect()
        object ShowDataLoadingError : SideEffect()

        object ValidateOriginAddress : SideEffect()
        object ShowOriginAddressSuggestions : SideEffect()
        object OpenOriginAddressEditor : SideEffect()

        object ValidateShippingAddress : SideEffect()
        object ShowShippingAddressSuggestions : SideEffect()
        object OpenShippingAddressEditor : SideEffect()

        object ShowPackagingDetails : SideEffect()
    }
}
