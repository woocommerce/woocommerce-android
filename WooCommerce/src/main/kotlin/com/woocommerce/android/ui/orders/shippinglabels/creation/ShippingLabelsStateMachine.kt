package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.tinder.StateMachine
import com.tinder.StateMachine.Transition.Valid
import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressChangeSuggested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressEditCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidationFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.CustomsDeclarationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.CustomsFormFilledOut
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.DataLoadingFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditCustomsRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditOriginAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPackagingRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPaymentRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditShippingAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditShippingCarrierRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.FlowStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.OriginAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PackageSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PackagesSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PaymentSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PaymentSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.CARRIER
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.CUSTOMS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.DONE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.ORIGIN_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.PACKAGING
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.PAYMENT
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep.SHIPPING_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.LoadData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.OpenAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ShowAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ShowCarrierOptions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ShowCustomsForm
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ShowError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ShowPackageOptions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ShowPaymentDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.UpdateViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.ValidateAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.CustomsDeclaration
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.DataLoading
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.DataLoadingFailure
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.Idle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.OriginAddressEditing
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.OriginAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.OriginAddressValidation
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.OriginAddressValidationFailure
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.PackageSelection
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.PaymentSelection
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.ShippingAddressEditing
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.ShippingAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.ShippingAddressValidation
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.ShippingAddressValidationFailure
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.ShippingCarrierSelection
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State.WaitingForInput
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.WooLog.T.ORDERS
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
class ShippingLabelsStateMachine @Inject constructor(

) {
    // the flow can be observed by a ViewModel (similar to LiveData) and it can react by perform actions and update
    // the view states based on the triggered states and side-effects
    private val _transitions = MutableStateFlow(Transition(Idle, SideEffect.NoOp))
    val transitions: StateFlow<Transition> = _transitions

    // the actual state machine behavior is defined by a DSL using the following format:
    //
    // state<STATE> {
    //     on<EVENT> {
    //         transitionTo(NEXT_STATE, SIDE_EFFECT)
    //     }
    // }
    private var stateMachine = createStateMachine()

    private fun createStateMachine(initialState: State = Idle) = StateMachine.create<State, Event, SideEffect> {
        initialState(initialState)

        state<Idle> {
            on<FlowStarted> { event ->
                transitionTo(DataLoading, LoadData(event.orderId))
            }
        }

        state<DataLoading> {
            on<DataLoaded> { event ->
                val data = Data(event.originAddress, event.shippingAddress, setOf(ORIGIN_ADDRESS))
                transitionTo(WaitingForInput(data), UpdateViewState(data))
            }
            on<DataLoadingFailed> {
                transitionTo(DataLoadingFailure, ShowError(DataLoadingError))
            }
        }

        state<WaitingForInput> {
            on<OriginAddressValidationStarted> {
                transitionTo(
                    OriginAddressValidation(data),
                    ValidateAddress(data.originAddress, ORIGIN)
                )
            }
            on<ShippingAddressValidationStarted> {
                transitionTo(
                    ShippingAddressValidation(data),
                    ValidateAddress(data.shippingAddress, DESTINATION)
                )
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
                transitionTo(OriginAddressEditing(data), OpenAddressEditor(data.originAddress, ORIGIN))
            }
            on<EditShippingAddressRequested> {
                transitionTo(
                    ShippingAddressEditing(data),
                    OpenAddressEditor(data.shippingAddress, DESTINATION)
                )
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
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + SHIPPING_ADDRESS
                )
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
            on<AddressChangeSuggested> { event ->
                transitionTo(
                    OriginAddressSuggestion(data),
                    ShowAddressSuggestion(data.originAddress, event.suggested, ORIGIN)
                )
            }
            on<AddressInvalid> { event ->
                transitionTo(
                    OriginAddressEditing(data),
                    OpenAddressEditor(data.originAddress, ORIGIN, event.validationResult)
                )
            }
            on<AddressValidationFailed> {
                transitionTo(OriginAddressValidationFailure, ShowError(AddressValidationError))
            }
        }

        state<OriginAddressSuggestion> {
            on<SuggestedAddressSelected> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + SHIPPING_ADDRESS
                )
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
            on<EditOriginAddressRequested> {
                transitionTo(
                    OriginAddressEditing(data),
                    OpenAddressEditor(data.originAddress, ORIGIN)
                )
            }
        }

        state<OriginAddressEditing> {
            on<AddressValidated> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    flowSteps = data.flowSteps + SHIPPING_ADDRESS
                )
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
            on<AddressEditCanceled> {
                transitionTo(WaitingForInput(data), UpdateViewState(data))
            }
        }

        state<ShippingAddressValidation> {
            on<AddressValidated> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + PACKAGING
                )
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
            on<AddressChangeSuggested> { event ->
                transitionTo(
                    ShippingAddressSuggestion(data),
                    ShowAddressSuggestion(data.originAddress, event.suggested, DESTINATION)
                )
            }
            on<AddressInvalid> { event ->
                transitionTo(
                    ShippingAddressEditing(data),
                    OpenAddressEditor(data.shippingAddress, DESTINATION, event.validationResult)
                )
            }
            on<AddressValidationFailed> {
                transitionTo(ShippingAddressValidationFailure, ShowError(AddressValidationError))
            }
        }

        state<ShippingAddressSuggestion> {
            on<SuggestedAddressSelected> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + PACKAGING
                )
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
            on<EditShippingAddressRequested> {
                transitionTo(
                    ShippingAddressEditing(data),
                    OpenAddressEditor(data.shippingAddress, DESTINATION)
                )
            }
        }

        state<ShippingAddressEditing> {
            on<AddressValidated> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    flowSteps = data.flowSteps + PACKAGING
                )
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
            on<AddressEditCanceled> {
                transitionTo(WaitingForInput(data), UpdateViewState(data))
            }
        }

        state<PackageSelection> {
            on<PackagesSelected> {
                val newData = data.copy(flowSteps = data.flowSteps + CUSTOMS)
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
        }

        state<CustomsDeclaration> {
            on<CustomsFormFilledOut> {
                val newData = data.copy(flowSteps = data.flowSteps + CARRIER)
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
        }

        state<ShippingCarrierSelection> {
            on<ShippingCarrierSelected> {
                val newData = data.copy(flowSteps = data.flowSteps + PAYMENT)
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
        }

        state<PaymentSelection> {
            on<PaymentSelected> {
                val newData = data.copy(flowSteps = data.flowSteps + DONE)
                transitionTo(WaitingForInput(newData), UpdateViewState(newData))
            }
        }

        // transition listener passes the side effects to the flow
        onTransition { transition ->
            if (transition is Valid) {
                WooLog.d(ORDERS, transition.toState.toString())
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
        stateMachine.transition(FlowStarted(orderId))
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
    }

    sealed class State : Parcelable {
        @Parcelize object Idle : State()
        @Parcelize object DataLoadingFailure : State()
        @Parcelize object DataLoading : State()
        @Parcelize data class WaitingForInput(val data: Data) : State()

        @Parcelize data class OriginAddressValidation(val data: Data) : State()
        @Parcelize data class OriginAddressSuggestion(val data: Data) : State()
        @Parcelize data class OriginAddressEditing(val data: Data) : State()
        @Parcelize object OriginAddressValidationFailure : State()

        @Parcelize data class ShippingAddressValidation(val data: Data) : State()
        @Parcelize data class ShippingAddressSuggestion(val data: Data) : State()
        @Parcelize data class ShippingAddressEditing(val data: Data) : State()
        @Parcelize object ShippingAddressValidationFailure : State()

        @Parcelize data class PackageSelection(val data: Data) : State()
        @Parcelize data class CustomsDeclaration(val data: Data) : State()
        @Parcelize data class ShippingCarrierSelection(val data: Data) : State()
        @Parcelize data class PaymentSelection(val data: Data) : State()
    }

    sealed class Event {
        data class FlowStarted(val orderId: String) : Event()
        data class DataLoaded(val originAddress: Address, val shippingAddress: Address) : Event()
        object DataLoadingFailed : Event()

        data class AddressInvalid(val address: Address, val validationResult: ValidationResult) : Event()
        data class AddressValidated(val address: Address) : Event()
        data class AddressChangeSuggested(val suggested: Address) : Event()
        data class SuggestedAddressSelected(val address: Address) : Event()
        object AddressValidationFailed : Event()
        object AddressEditCanceled : Event()

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

        object ShowPackageOptions : SideEffect()
        object ShowCustomsForm : SideEffect()
        object ShowCarrierOptions : SideEffect()
        object ShowPaymentDetails : SideEffect()
    }

    class InvalidStateException(message: String) : Exception(message)

    data class Transition(val state: State, val sideEffect: SideEffect?)
}
