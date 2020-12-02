package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

/*

The finite-state machine that manages the shipping label creation workflow. The following diagram represents the initial
data-loading and origin address verification and it illustrates the relationships between different states, events and
side effects (in parentheses).

                  +
                  |
                  |
                  v
           +------+-------+                             +--------------------------+
           |              |                             |                          |    SuggestedAddressSelected
           |     Idle     |                             |         Waiting          |       (UpdateViewState)
           |              |                             |           For            +<-------------------------------+
           +------+-------+    +----------------------->+          Input           |                                |
                  |            |      DataLoaded        |                          +<---------------+               |
      FlowStarted |            |  (UpdateViewState)     +---+----+-----------------+                |               |
      (LoadData)  |            |                            |    |                                  |               |
                  |            |                            |    | OriginAddressValidationStarted   |               |
                  v            |                            |    |       (ValidateAddress)          |               |
           +------+-------+    |                            |    |                                  |               |
           |              |    |                            |    |                                  |               |
           | Data Loading +----+                            |    |                                  |               |
           |              |                                 |    |                                  |               |
           +------+-------+                                 |    |                                  |               |
                  |              EditOriginAddressRequested |    |                                  |               |
DataLoadingFailed |                  (OpenAddressEditor)    |    |                                  |               |
   (ShowError)    |            +----------------------------+    |                                  |               |
                  v            |                                 v                                  |               |
           +------+-------+    |                      +----------+----------+   AddressValidated    |               |
           |              |    |   AddressEditFinished|                     |   (UpdateViewState)   |               |
           | Data Loading |    |    (ValidateAddress) |    Origin Address   +-----------------------+               |
           |   Failure    |    |  +------------------>+      Validation     |                                       |
           |              |    |  |                   |                     +---------+                             |
           +--------------+    |  |                   +----+---+------------+         |      AddressInvalid         |
                               |  |                        |   ^                      | (ShowAddressSuggestions)    |
                               |  |                        |   |                      |                             |
                               |  |                        |   |                      |                             |
                               |  |   AddressNotRecognized |   |                      |                             |
                               |  |   (OpenAddressEditor)  |   | AddressEditFinished  |                             |
                               |  |                        |   |  (ValidateAddress)   |                             |
                               |  |                        |   |                      |                             |
                               |  |                        |   |                      |                             |
                               |  |                        v   |                      v                             |
                               |  |     +------------------+-+ |                +-----+--------------+              |
                               |  +-----+                    | |                |                    |              |
                               |        |   Origin Address   | |                |   Origin Address   +--------------+
                               +------->+       Editing      +-+                |    Suggestions     |
                                        |                    |                  |                    |
                                        +--------+-----------+                  +---------+----------+
                                                 ^                                        |
                                                 |                                        |
                                                 |                                        |
                                                 |                                        |
                                                 +----------------------------------------+
                                                          EditOriginAddressRequested
                                                             (OpenAddressEditor)
 */
@ExperimentalCoroutinesApi
class ShippingLabelsStateMachine @Inject constructor() {
    // the flow can be observed by a ViewModel (similar to LiveData) and it can react by perform actions and update
    // the view states based on the triggered side-effects
    private val _effects = MutableStateFlow<SideEffect>(SideEffect.NoOp)
    val effects: StateFlow<SideEffect> = _effects

    // the actual state machine behavior is defined by a DSL using the following format:
    //
    // state<STATE> {
    //     on<EVENT> {
    //         transitionTo(NEXT_STATE, SIDE_EFFECT)
    //     }
    // }
    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.Idle)

        state<State.Idle> {
            on<Event.FlowStarted> { event ->
                transitionTo(State.DataLoading, SideEffect.LoadData(event.orderId))
            }
        }

        state<State.DataLoading> {
            on<Event.DataLoaded> { event ->
                val data = Data(event.originAddress, event.shippingAddress, setOf(FlowStep.ORIGIN_ADDRESS))
                transitionTo(State.WaitingForInput(data), SideEffect.UpdateViewState(data))
            }
            on<Event.DataLoadingFailed> {
                transitionTo(State.DataLoadingFailure, SideEffect.ShowError(Error.DataLoadingError))
            }
        }

        state<State.WaitingForInput> {
            on<Event.OriginAddressValidationStarted> {
                transitionTo(State.OriginAddressValidation(data), SideEffect.ValidateAddress(data.originAddress))
            }
            on<Event.ShippingAddressValidationStarted> {
                transitionTo(State.ShippingAddressValidation(data), SideEffect.ValidateAddress(data.shippingAddress))
            }
            on<Event.PackageSelectionStarted> {
                transitionTo(State.PackageSelection(data), SideEffect.ShowPackageOptions)
            }
            on<Event.CustomsDeclarationStarted> {
                transitionTo(State.CustomsDeclaration(data), SideEffect.ShowCustomsForm)
            }
            on<Event.ShippingCarrierSelectionStarted> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions)
            }
            on<Event.PaymentSelectionStarted> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentDetails)
            }
            on<Event.EditOriginAddressRequested> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress))
            }
            on<Event.EditShippingAddressRequested> {
                transitionTo(State.ShippingAddressEditing(data), SideEffect.OpenAddressEditor(data.shippingAddress))
            }
            on<Event.EditPackagingRequested> {
                transitionTo(State.PackageSelection(data), SideEffect.ShowPackageOptions)
            }
            on<Event.EditCustomsRequested> {
                transitionTo(State.CustomsDeclaration(data), SideEffect.ShowCustomsForm)
            }
            on<Event.EditShippingCarrierRequested> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions)
            }
            on<Event.EditPaymentRequested> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentDetails)
            }
        }

        state<State.OriginAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.OriginAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(data.originAddress, event.suggested)
                )
            }
            on<Event.AddressNotRecognized> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress))
            }
        }

        state<State.OriginAddressSuggestion> {
            on<Event.SuggestedAddressSelected> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.EditOriginAddressRequested> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress))
            }
        }

        state<State.OriginAddressEditing> {
            on<Event.AddressEditFinished> { event ->
                transitionTo(State.OriginAddressValidation(data), SideEffect.ValidateAddress(event.address))
            }
            on<Event.AddressUsedAsIs> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.ShippingAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.ShippingAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(data.originAddress, event.suggested)
                )
            }
            on<Event.AddressNotRecognized> {
                transitionTo(State.ShippingAddressEditing(data), SideEffect.OpenAddressEditor(data.shippingAddress))
            }
        }

        state<State.OriginAddressSuggestion> {
            on<Event.SuggestedAddressSelected> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.EditShippingAddressRequested> {
                transitionTo(State.ShippingAddressEditing(data), SideEffect.OpenAddressEditor(data.shippingAddress))
            }
        }

        state<State.ShippingAddressEditing> {
            on<Event.AddressEditFinished> { event ->
                transitionTo(State.ShippingAddressValidation(data), SideEffect.ValidateAddress(event.address))
            }
            on<Event.AddressUsedAsIs> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.PackageSelection> {
            on<Event.PackagesSelected> {
                val newData = data.copy(flowSteps = data.flowSteps + FlowStep.CUSTOMS)
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.CustomsDeclaration> {
            on<Event.CustomsFormFilledOut> {
                val newData = data.copy(flowSteps = data.flowSteps + FlowStep.CARRIER)
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.ShippingCarrierSelection> {
            on<Event.ShippingCarrierSelected> {
                val newData = data.copy(flowSteps = data.flowSteps + FlowStep.PAYMENT)
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.PaymentSelection> {
            on<Event.PaymentSelected> {
                val newData = data.copy(flowSteps = data.flowSteps + FlowStep.DONE)
                transitionTo(State.WaitingForInput(newData), SideEffect.UpdateViewState(newData))
            }
        }

        // transition listener passes the side effects to the flow
        onTransition { transition ->
            if (transition is StateMachine.Transition.Valid) {
                WooLog.d(T.ORDERS, transition.toState.toString())
                transition.sideEffect?.let { sideEffect ->
                    _effects.value = sideEffect
                }
            } else {
                throw InvalidStateException("Unexpected event ${transition.event} passed from ${transition.fromState}")
            }
        }
    }

    /**
     * Starts the initial event sequence (see the diagram)
     */
    fun start(orderId: String) {
        stateMachine.transition(Event.FlowStarted(orderId))
    }

    /**
     * Incoming external event that triggers a transition (such as user input)
     */
    fun handleEvent(event: Event) {
        WooLog.d(T.ORDERS, event.toString())
        stateMachine.transition(event)
    }

    /**
     * Data passed around between states
     */
    data class Data(
        val originAddress: Address,
        val shippingAddress: Address,
        val flowSteps: Set<FlowStep>
    )

    /**
     * The main shipping label creation steps
     */
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
        data class WaitingForInput(val data: Data) : State()

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
