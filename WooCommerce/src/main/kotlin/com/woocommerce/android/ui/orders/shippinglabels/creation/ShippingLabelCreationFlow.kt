package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.util.Log
import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.Event.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.SideEffect.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreationFlow.State.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ShippingLabelCreationFlow @Inject constructor() {
    companion object {
        private val TAG = ShippingLabelCreationFlow::class.simpleName
    }
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

        state<PackageSelection> {
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
