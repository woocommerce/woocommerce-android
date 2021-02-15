package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.android.parcel.Parcelize
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
                  |                                     +--------------------------+
                  v                                     |                          +<------------------------------+
           +------+-------+                             |                          |                               |
           |              |                             |         Waiting          |  SuggestedAddressSelected     |
           |     Idle     |                             |           For            |     (UpdateViewState)         |
           |              |                             |          Input           +<--------------------------+   |
           +------+-------+    +----------------------->+                          |                           |   |
                  |            |      DataLoaded        |                          +<---------------+          |   |
      FlowStarted |            |  (UpdateViewState)     +---+----+-----------------+                |          |   |
      (LoadData)  |            |                            |    |                                  |          |   |
                  |            |                            |    | OriginAddressValidationStarted   |          |   |
                  v            |                            |    |       (ValidateAddress)          |          |   |
           +------+-------+    |                            |    |                                  |          |   |
           |              |    |                            |    |                                  |          |   |
           | Data Loading +----+                            |    |                                  |          |   |
           |              |                                 |    |                                  |          |   |
           +------+-------+                                 |    |                                  |          |   |
                  |              EditOriginAddressRequested |    |                                  |          |   |
DataLoadingFailed |                  (OpenAddressEditor)    |    |                                  |          |   |
   (ShowError)    |            +----------------------------+    |                                  |          |   |
                  v            |                                 v                                  |          |   |
           +------+-------+    |                      +----------+----------+   AddressValidated    |          |   |
           |              |    |                      |                     |   (UpdateViewState)   |          |   |
           | Data Loading |    |                      |    Origin Address   +-----------------------+          |   |
           |   Failure    |    |                      |      Validation     |                                  |   |
           |              |    |                      |                     |                                  |   |
           +--------------+    |                      +----+----------+-----+                                  |   |
                               |                           |          |                                        |   |
                               |                           |          |                                        |   |
                               |                           |          |                                        |   |
                               |         AddressInvalid    |          |  AddressChangeSuggested                |   |
                               |      (OpenAddressEditor)  |          | (ShowAddressSuggestions)               |   |
                               |                           |          |                                        |   |
                               |                           |          |                                        |   |
                               |                           |          |                                        |   |
                               |                           v          v                                        |   |
                               |        +------------------+-+      +-+-------------------+                    |   |
                               |        |                    |      |                     |                    |   |
                               |        |   Origin Address   |      |    Origin Address   +--------------------+   |
                               +------->+       Editing      |      |     Suggestions     |                        |
                                        |                    |      |                     |                        |
                                        +--+-----+-----------+      +-----------+---------+                        |
                                           |     ^                              |                                  |
                                           |     |                              |                                  |
                                           |     |                              |                                  |
                                           |     |                              |                                  |
                                           |     +------------------------------+                                  |
                                           |        EditOriginAddressRequested                                     |
                                           |           (OpenAddressEditor)                                         |
                                           |                                                                       |
                                           |                                                                       |
                                           +-----------------------------------------------------------------------+
                                                                     AddressEditFinished
                                                                      (UpdateViewState)

This diagram was created using ASCIIFlow (asciiflow.com). If you want to modify it, you can just copy the text and
import it.

 */
@ExperimentalCoroutinesApi
class ShippingLabelsStateMachine @Inject constructor() {
    // the flow can be observed by a ViewModel (similar to LiveData) and it can react by perform actions and update
    // the view states based on the triggered states and side-effects
    private val _transitions = MutableStateFlow(Transition(State.Idle, SideEffect.NoOp))
    val transitions: StateFlow<Transition> = _transitions

    // the actual state machine behavior is defined by a DSL using the following format:
    //
    // state<State.STATE> {
    //     on<Event.EVENT> {
    //         transitionTo(State.NEXT_STATE, SIDE_EFFECT)
    //     }
    // }
    private var stateMachine = createStateMachine()

    private fun createStateMachine(initialState: State = State.Idle) = StateMachine.create<State, Event, SideEffect> {
        initialState(initialState)

        state<State.Idle> {
            on<Event.FlowStarted> { event ->
                transitionTo(State.DataLoading, SideEffect.LoadData(event.orderId))
            }
        }

        state<State.DataLoading> {
            on<Event.DataLoaded> { event ->
                val data = Data(
                    originAddress = event.originAddress,
                    shippingAddress = event.shippingAddress,
                    currentPaymentMethod = event.currentPaymentMethod,
                    shippingPackages = emptyList(),
                    flowSteps = setOf(FlowStep.ORIGIN_ADDRESS)
                )
                transitionTo(State.WaitingForInput(data))
            }
            on<Event.DataLoadingFailed> {
                transitionTo(State.DataLoadingFailure, SideEffect.ShowError(DataLoadingError))
            }
        }

        state<State.DataLoadingFailure> {
            on<Event.FlowStarted> { event ->
                transitionTo(State.DataLoading, SideEffect.LoadData(event.orderId))
            }
        }

        state<State.WaitingForInput> {
            on<Event.OriginAddressValidationStarted> {
                transitionTo(
                    State.OriginAddressValidation(data),
                    SideEffect.ValidateAddress(data.originAddress, ORIGIN)
                )
            }
            on<Event.ShippingAddressValidationStarted> {
                transitionTo(
                    State.ShippingAddressValidation(data),
                    SideEffect.ValidateAddress(data.shippingAddress, DESTINATION)
                )
            }
            on<Event.PackageSelectionStarted> {
                transitionTo(State.PackageSelection(data), SideEffect.ShowPackageOptions(data.shippingPackages))
            }
            on<Event.CustomsDeclarationStarted> {
                transitionTo(State.CustomsDeclaration(data), SideEffect.ShowCustomsForm)
            }
            on<Event.ShippingCarrierSelectionStarted> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions)
            }
            on<Event.PaymentSelectionStarted> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentOptions)
            }
            on<Event.EditOriginAddressRequested> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress, ORIGIN))
            }
            on<Event.EditShippingAddressRequested> {
                transitionTo(
                    State.ShippingAddressEditing(data),
                    SideEffect.OpenAddressEditor(data.shippingAddress, DESTINATION)
                )
            }
            on<Event.EditPackagingRequested> {
                transitionTo(State.PackageSelection(data), SideEffect.ShowPackageOptions(data.shippingPackages))
            }
            on<Event.EditCustomsRequested> {
                transitionTo(State.CustomsDeclaration(data), SideEffect.ShowCustomsForm)
            }
            on<Event.EditShippingCarrierRequested> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions)
            }
            on<Event.EditPaymentRequested> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentOptions)
            }
        }

        state<State.OriginAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForInput(newData))
            }
            on<Event.AddressChangeSuggested> { event ->
                transitionTo(
                    State.OriginAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(data.originAddress, event.suggested, ORIGIN)
                )
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.OriginAddressEditing(data),
                    SideEffect.OpenAddressEditor(data.originAddress, ORIGIN, event.validationResult)
                )
            }
            on<Event.AddressValidationFailed> {
                transitionTo(State.OriginAddressValidationFailure, SideEffect.ShowError(AddressValidationError))
            }
        }

        state<State.OriginAddressSuggestion> {
            on<Event.SuggestedAddressAccepted> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForInput(newData))
            }
            on<Event.EditAddressRequested> { event ->
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(event.address, ORIGIN))
            }
            on<Event.SuggestedAddressDiscarded> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.OriginAddressEditing> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForInput(newData))
            }
            on<Event.AddressEditCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.ShippingAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForInput(newData))
            }
            on<Event.AddressChangeSuggested> { event ->
                transitionTo(
                    State.ShippingAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(data.originAddress, event.suggested, DESTINATION)
                )
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.ShippingAddressEditing(data),
                    SideEffect.OpenAddressEditor(data.shippingAddress, DESTINATION, event.validationResult)
                )
            }
            on<Event.AddressValidationFailed> {
                transitionTo(State.ShippingAddressValidationFailure, SideEffect.ShowError(AddressValidationError))
            }
        }

        state<State.ShippingAddressSuggestion> {
            on<Event.SuggestedAddressAccepted> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForInput(newData))
            }
            on<Event.EditAddressRequested> { event ->
                transitionTo(
                    State.ShippingAddressEditing(data),
                    SideEffect.OpenAddressEditor(event.address, DESTINATION)
                )
            }
            on<Event.SuggestedAddressDiscarded> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.ShippingAddressEditing> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForInput(newData))
            }
            on<Event.AddressEditCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.PackageSelection> {
            on<Event.PackagesSelected> { event ->
                val newData = data.copy(
                    shippingPackages = event.shippingPackages,
                    flowSteps = data.flowSteps + FlowStep.CUSTOMS
                )
                transitionTo(State.WaitingForInput(newData))
            }

            on<Event.EditPackagingCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.CustomsDeclaration> {
            on<Event.CustomsFormFilledOut> {
                val newData = data.copy(flowSteps = data.flowSteps + FlowStep.CARRIER)
                transitionTo(State.WaitingForInput(newData))
            }
        }

        state<State.ShippingCarrierSelection> {
            on<Event.ShippingCarrierSelected> {
                val stepsToAdd = if (data.currentPaymentMethod != null) {
                    listOf(FlowStep.PAYMENT, FlowStep.DONE)
                } else {
                    listOf(FlowStep.PAYMENT)
                }
                val newData = data.copy(flowSteps = data.flowSteps + stepsToAdd)
                transitionTo(State.WaitingForInput(newData))
            }
        }

        state<State.PaymentSelection> {
            on<Event.PaymentSelected> {
                val newData = data.copy(
                    currentPaymentMethod = it.paymentMethod,
                    flowSteps = data.flowSteps + FlowStep.DONE
                )
                transitionTo(State.WaitingForInput(newData))
            }

            on<Event.EditPaymentCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        // transition listener passes the side effects to the flow
        onTransition { transition ->
            if (transition is StateMachine.Transition.Valid) {
                WooLog.d(T.ORDERS, transition.toState.toString())
                _transitions.value = Transition(transition.toState, transition.sideEffect)
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

    fun initialize(state: State) {
        stateMachine = createStateMachine(state)
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
    @Parcelize
    data class Data(
        val originAddress: Address,
        val shippingAddress: Address,
        val currentPaymentMethod: PaymentMethod?,
        val shippingPackages: List<ShippingLabelPackage>,
        val flowSteps: Set<FlowStep>
    ) : Parcelable

    /**
     * The main shipping label creation steps
     */
    enum class FlowStep {
        ORIGIN_ADDRESS, SHIPPING_ADDRESS, PACKAGING, CUSTOMS, CARRIER, PAYMENT, DONE
    }

    sealed class Error {
        object DataLoadingError : Error()
        object AddressValidationError : Error()
        object PackagesLoadingError : Error()
    }

    sealed class State : Parcelable {
        @Parcelize
        object Idle : State()

        @Parcelize
        object DataLoadingFailure : State()

        @Parcelize
        object DataLoading : State()

        @Parcelize
        data class WaitingForInput(val data: Data) : State()

        @Parcelize
        data class OriginAddressValidation(val data: Data) : State()

        @Parcelize
        data class OriginAddressSuggestion(val data: Data) : State()

        @Parcelize
        data class OriginAddressEditing(val data: Data) : State()

        @Parcelize
        object OriginAddressValidationFailure : State()

        @Parcelize
        data class ShippingAddressValidation(val data: Data) : State()

        @Parcelize
        data class ShippingAddressSuggestion(val data: Data) : State()

        @Parcelize
        data class ShippingAddressEditing(val data: Data) : State()

        @Parcelize
        object ShippingAddressValidationFailure : State()

        @Parcelize
        data class PackageSelection(val data: Data) : State()

        @Parcelize
        data class CustomsDeclaration(val data: Data) : State()

        @Parcelize
        data class ShippingCarrierSelection(val data: Data) : State()

        @Parcelize
        data class PaymentSelection(val data: Data) : State()
    }

    sealed class Event {
        data class FlowStarted(val orderId: String) : Event()
        data class DataLoaded(
            val originAddress: Address,
            val shippingAddress: Address,
            val currentPaymentMethod: PaymentMethod?
        ) : Event()

        object DataLoadingFailed : Event()

        data class AddressInvalid(val address: Address, val validationResult: ValidationResult) : Event()
        data class AddressValidated(val address: Address) : Event()
        data class AddressChangeSuggested(val suggested: Address) : Event()
        data class SuggestedAddressAccepted(val address: Address) : Event()
        data class EditAddressRequested(val address: Address) : Event()
        object AddressValidationFailed : Event()
        object AddressEditCanceled : Event()
        object SuggestedAddressDiscarded : Event()

        object OriginAddressValidationStarted : Event()
        object EditOriginAddressRequested : Event()

        object ShippingAddressValidationStarted : Event()
        object EditShippingAddressRequested : Event()

        object PackageSelectionStarted : Event()
        object EditPackagingRequested : Event()
        object EditPackagingCanceled : Event()
        data class PackagesSelected(val shippingPackages: List<ShippingLabelPackage>) : Event()

        object CustomsDeclarationStarted : Event()
        object EditCustomsRequested : Event()
        object CustomsFormFilledOut : Event()

        object ShippingCarrierSelectionStarted : Event()
        object EditShippingCarrierRequested : Event()
        object ShippingCarrierSelected : Event()

        object PaymentSelectionStarted : Event()
        object EditPaymentRequested : Event()
        object EditPaymentCanceled : Event()
        data class PaymentSelected(val paymentMethod: PaymentMethod) : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        data class LoadData(val orderId: String) : SideEffect()
        data class ShowError(val error: Error) : SideEffect()

        data class ValidateAddress(val address: Address, val type: AddressType) : SideEffect()
        data class ShowAddressSuggestion(
            val entered: Address,
            val suggested: Address,
            val type: AddressType
        ) : SideEffect()

        data class OpenAddressEditor(
            val address: Address,
            val type: AddressType,
            val validationResult: ValidationResult? = null
        ) : SideEffect()

        data class ShowPackageOptions(
            val shippingPackages: List<ShippingLabelPackage>
        ) : SideEffect()

        object ShowCustomsForm : SideEffect()
        object ShowCarrierOptions : SideEffect()
        object ShowPaymentOptions : SideEffect()
    }

    class InvalidStateException(message: String) : Exception(message)

    data class Transition(val state: State, val sideEffect: SideEffect?)
}
