package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.tinder.StateMachine
import com.woocommerce.android.model.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.UserInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.TrackCompletedStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect.TrackFlowStart
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.*
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.*
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
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
class ShippingLabelsStateMachine @Inject constructor() {
    // the flow can be observed by a ViewModel (similar to LiveData) and it can react by perform actions and update
    // the view states based on the triggered states and side-effects
    @ExperimentalCoroutinesApi
    private val _transitions = MutableStateFlow(Transition(State.Idle, SideEffect.NoOp))

    @ExperimentalCoroutinesApi
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
                transitionTo(State.DataLoading(event.orderId), TrackFlowStart)
            }
        }

        state<State.DataLoading> {
            on<Event.DataLoaded> { event ->
                val steps = StepsState(
                    originAddressStep = OriginAddressStep(READY, event.originAddress),
                    shippingAddressStep = ShippingAddressStep(NOT_READY, event.shippingAddress),
                    packagingStep = PackagingStep(NOT_READY, emptyList()),
                    customsStep = CustomsStep(NOT_READY, data = null),
                    carrierStep = CarrierStep(NOT_READY, emptyList()),
                    paymentsStep = PaymentsStep(NOT_READY, event.currentPaymentMethod)
                ).checkRequirementsAndUpdateState()

                val data = StateMachineData(
                    order = event.order,
                    stepsState = steps
                )
                transitionTo(State.WaitingForInput(data))
            }
            on<Event.DataLoadingFailed> {
                transitionTo(State.DataLoadingFailure, SideEffect.ShowError(DataLoadingError))
            }
        }

        state<State.DataLoadingFailure> {
            on<Event.FlowStarted> { event ->
                transitionTo(State.DataLoading(event.orderId))
            }
        }

        state<State.WaitingForInput> {
            on<Event.OriginAddressValidationStarted> {
                transitionTo(State.OriginAddressValidation(data))
            }
            on<Event.ShippingAddressValidationStarted> {
                transitionTo(State.ShippingAddressValidation(data))
            }
            on<Event.PackageSelectionStarted> {
                transitionTo(
                    State.PackageSelection(data),
                    SideEffect.ShowPackageOptions(data.stepsState.packagingStep.data)
                )
            }
            on<Event.CustomsDeclarationStarted> {
                transitionTo(
                    State.CustomsDeclaration(data),
                    SideEffect.ShowCustomsForm(
                        originCountryCode = data.stepsState.originAddressStep.data.country.code,
                        destinationCountryCode = data.stepsState.shippingAddressStep.data.country.code,
                        shippingPackages = data.stepsState.packagingStep.data,
                        customsPackages = data.stepsState.customsStep.data ?: emptyList()
                    )
                )
            }
            on<Event.ShippingCarrierSelectionStarted> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions(data))
            }
            on<Event.PaymentSelectionStarted> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentOptions)
            }
            on<Event.EditOriginAddressRequested> {
                transitionTo(
                    State.OriginAddressEditing(data),
                    SideEffect.OpenAddressEditor(
                        address = data.stepsState.originAddressStep.data,
                        type = ORIGIN,
                        requiresPhoneNumber = data.isCustomsFormRequired
                    )
                )
            }
            on<Event.EditShippingAddressRequested> {
                transitionTo(
                    State.ShippingAddressEditing(data),
                    SideEffect.OpenAddressEditor(
                        address = data.stepsState.shippingAddressStep.data,
                        type = DESTINATION,
                        requiresPhoneNumber = data.isCustomsFormRequired
                    )
                )
            }
            on<Event.EditPackagingRequested> {
                transitionTo(
                    State.PackageSelection(data),
                    SideEffect.ShowPackageOptions(data.stepsState.packagingStep.data)
                )
            }
            on<Event.EditCustomsRequested> {
                transitionTo(
                    State.CustomsDeclaration(data),
                    SideEffect.ShowCustomsForm(
                        originCountryCode = data.stepsState.originAddressStep.data.country.code,
                        destinationCountryCode = data.stepsState.shippingAddressStep.data.country.code,
                        shippingPackages = data.stepsState.packagingStep.data,
                        customsPackages = data.stepsState.customsStep.data ?: emptyList()
                    )
                )
            }
            on<Event.EditShippingCarrierRequested> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions(data))
            }
            on<Event.EditPaymentRequested> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentOptions)
            }
            on<Event.PurchaseStarted> {
                transitionTo(State.PurchaseLabels(data, it.fulfillOrder))
            }
        }

        state<State.OriginAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.originAddressStep, event.address)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.originAddressStep))
            }
            on<Event.AddressChangeSuggested> { event ->
                transitionTo(
                    State.OriginAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(
                        data.stepsState.originAddressStep.data,
                        event.suggested,
                        ORIGIN
                    )
                )
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.OriginAddressEditing(data),
                    SideEffect.OpenAddressEditor(
                        address = data.stepsState.originAddressStep.data,
                        type = ORIGIN,
                        validationResult = event.validationResult,
                        requiresPhoneNumber = data.isCustomsFormRequired
                    )
                )
            }
            on<Event.AddressValidationFailed> {
                transitionTo(State.WaitingForInput(data), SideEffect.ShowError(AddressValidationError))
            }
        }

        state<State.OriginAddressSuggestion> {
            on<Event.SuggestedAddressAccepted> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.originAddressStep, event.address)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.originAddressStep))
            }
            on<Event.EditAddressRequested> { event ->
                transitionTo(
                    State.OriginAddressEditing(data),
                    SideEffect.OpenAddressEditor(
                        address = event.address,
                        type = ORIGIN,
                        requiresPhoneNumber = data.isCustomsFormRequired
                    )
                )
            }
            on<Event.SuggestedAddressDiscarded> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.OriginAddressEditing> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.originAddressStep, event.address)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.originAddressStep))
            }
            on<Event.AddressEditCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.ShippingAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.shippingAddressStep, event.address)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.shippingAddressStep))
            }
            on<Event.AddressChangeSuggested> { event ->
                transitionTo(
                    State.ShippingAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(
                        data.stepsState.shippingAddressStep.data,
                        event.suggested,
                        DESTINATION
                    )
                )
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.ShippingAddressEditing(data),
                    SideEffect.OpenAddressEditor(
                        address = data.stepsState.shippingAddressStep.data,
                        type = DESTINATION,
                        validationResult = event.validationResult,
                        requiresPhoneNumber = data.isCustomsFormRequired
                    )
                )
            }
            on<Event.AddressValidationFailed> {
                transitionTo(State.WaitingForInput(data), SideEffect.ShowError(AddressValidationError))
            }
        }

        state<State.ShippingAddressSuggestion> {
            on<Event.SuggestedAddressAccepted> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.shippingAddressStep, event.address)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.shippingAddressStep))
            }
            on<Event.EditAddressRequested> { event ->
                transitionTo(
                    State.ShippingAddressEditing(data),
                    SideEffect.OpenAddressEditor(
                        address = event.address,
                        type = DESTINATION,
                        requiresPhoneNumber = data.isCustomsFormRequired
                    )
                )
            }
            on<Event.SuggestedAddressDiscarded> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.ShippingAddressEditing> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.shippingAddressStep, event.address)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.shippingAddressStep))
            }
            on<Event.AddressEditCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.PackageSelection> {
            on<Event.PackagesSelected> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.packagingStep, event.shippingPackages)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.packagingStep))
            }

            on<Event.EditPackagingCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.CustomsDeclaration> {
            on<Event.CustomsFormFilledOut> { event ->
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.customsStep, event.customsPackages)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.customsStep))
            }
            on<Event.EditCustomsCanceled> {
                transitionTo((State.WaitingForInput(data)))
            }
        }

        state<State.ShippingCarrierSelection> {
            on<Event.ShippingCarrierSelected> {
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(
                        data.stepsState.carrierStep,
                        it.rates
                    )
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.carrierStep))
            }
            on<Event.ShippingCarrierSelectionCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.PaymentSelection> {
            on<Event.PaymentSelected> {
                val newData = data.copy(
                    stepsState = data.stepsState.updateStep(data.stepsState.paymentsStep, it.paymentMethod)
                )
                transitionTo(State.WaitingForInput(newData), getTracksSideEffect(data.stepsState.paymentsStep))
            }

            on<Event.EditPaymentCanceled> {
                transitionTo(State.WaitingForInput(data))
            }
        }

        state<State.PurchaseLabels> {
            on<Event.PurchaseFailed> {
                transitionTo(State.WaitingForInput(data), SideEffect.ShowError(Error.PurchaseError))
            }

            on<Event.PurchaseSuccess> {
                transitionTo(State.Idle, SideEffect.ShowLabelsPrint(data.order.id, it.labels))
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
    fun start(orderId: Long) {
        stateMachine.transition(Event.FlowStarted(orderId))
    }

    fun initialize(state: State) {
        stateMachine = createStateMachine(state)
        _transitions.value = Transition(state, SideEffect.NoOp)
    }

    /**
     * Incoming external event that triggers a transition (such as user input)
     */
    fun handleEvent(event: Event) {
        WooLog.d(T.ORDERS, event.toString())

        // we can ignore invalid state transitions caused by user input (most likely caused by duplicate clicks)
        if (event !is UserInput || stateMachine.state is State.WaitingForInput) {
            stateMachine.transition(event)
        }
    }

    private fun getTracksSideEffect(step: Step<*>): SideEffect? {
        return if (step.status == DONE) {
            null
        } else {
            TrackCompletedStep(step)
        }
    }

    /**
     * Data passed around between states
     */
    @Parcelize
    data class StateMachineData(
        val order: Order,
        val stepsState: StepsState
    ) : Parcelable {
        @IgnoredOnParcel
        val isCustomsFormRequired
            get() = stepsState.isCustomsFormRequired
    }

    /**
     * The main shipping label creation steps
     */
    sealed class Step<T> : Parcelable {
        abstract val status: StepStatus
        abstract val data: T
        open val isVisible: Boolean = true

        @Parcelize
        data class OriginAddressStep(override val status: StepStatus, override val data: Address) : Step<Address>()

        @Parcelize
        data class ShippingAddressStep(
            override val status: StepStatus,
            override val data: Address
        ) : Step<Address>()

        @Parcelize
        data class PackagingStep(
            override val status: StepStatus,
            override val data: List<ShippingLabelPackage>
        ) : Step<List<ShippingLabelPackage>>()

        @Parcelize
        data class CustomsStep(
            override val status: StepStatus,
            override val isVisible: Boolean = false,
            override val data: List<CustomsPackage>?
        ) : Step<List<CustomsPackage>?>()

        @Parcelize
        data class CarrierStep(
            override val status: StepStatus,
            override val data: List<ShippingRate>
        ) : Step<List<ShippingRate>>()

        @Parcelize
        data class PaymentsStep(
            override val status: StepStatus,
            override val data: PaymentMethod?
        ) : Step<PaymentMethod?>()
    }

    @Parcelize
    data class StepsState(
        val originAddressStep: OriginAddressStep,
        val shippingAddressStep: ShippingAddressStep,
        val packagingStep: PackagingStep,
        val customsStep: CustomsStep,
        val carrierStep: CarrierStep,
        val paymentsStep: PaymentsStep
    ) : Parcelable, Iterable<Step<out Any?>> {
        companion object {
            // These US states are a special case because they represent military bases. They're considered "domestic",
            // but they require a Customs form to ship from/to them.
            private val US_MILITARY_STATES = arrayOf("AA", "AE", "AP")
        }

        @IgnoredOnParcel
        val isCustomsFormRequired: Boolean by lazy {
            val originAddress = originAddressStep.data
            val shippingAddress = shippingAddressStep.data

            fun Address.isUSMilitaryState() =
                country.code == "US" && US_MILITARY_STATES.contains(state.asLocation().code)

            return@lazy if (originAddress.isUSMilitaryState() || shippingAddress.isUSMilitaryState()) {
                // Special case: Any shipment from/to military addresses must have Customs
                true
            } else {
                originAddress.country != shippingAddress.country
            }
        }

        @IgnoredOnParcel
        private val backingList = listOf(
            originAddressStep,
            shippingAddressStep,
            packagingStep,
            customsStep,
            carrierStep,
            paymentsStep
        ).filter { it.isVisible }

        @Suppress("UNCHECKED_CAST")
        fun <T> updateStep(currentStep: Step<T>, newData: T): StepsState {
            return when (currentStep) {
                is OriginAddressStep -> {
                    val newAddress = newData as Address
                    val isSamePhysicalAddress = newAddress.isSamePhysicalAddress(originAddressStep.data)
                    val newState = copy(
                        originAddressStep = originAddressStep.copy(status = DONE, data = newData as Address)
                    )
                    if (isSamePhysicalAddress) newState else newState.invalidateCarrierStep()
                }
                is ShippingAddressStep -> {
                    val newAddress = newData as Address
                    val isSamePhysicalAddress = newAddress.isSamePhysicalAddress(shippingAddressStep.data)
                    val newState = copy(
                        shippingAddressStep = shippingAddressStep.copy(status = DONE, data = newData as Address)
                    )
                    if (isSamePhysicalAddress) newState else newState.invalidateCarrierStep()
                }
                is PackagingStep -> {
                    copy(
                        packagingStep = packagingStep.copy(status = DONE, data = newData as List<ShippingLabelPackage>)
                    )
                        .invalidateCustomsStep()
                        .invalidateCarrierStep()
                }
                is CustomsStep -> {
                    copy(customsStep = customsStep.copy(status = DONE, data = newData as List<CustomsPackage>))
                        .invalidateCarrierStep()
                }
                is CarrierStep -> {
                    val paymentStatus = if (paymentsStep.data == null) READY else DONE
                    copy(
                        carrierStep = carrierStep.copy(status = DONE, data = newData as List<ShippingRate>),
                        paymentsStep = paymentsStep.copy(status = paymentStatus)
                    )
                }
                is PaymentsStep -> copy(
                    paymentsStep = paymentsStep.copy(status = DONE, data = newData as PaymentMethod)
                )
            }
                .checkRequirementsAndUpdateState()
        }

        fun checkRequirementsAndUpdateState() = updateForInternationalRequirements()
            .calculateNextStep()
            .fixStates()

        private fun updateForInternationalRequirements(): StepsState {
            val originAddressStep = if (isCustomsFormRequired &&
                !originAddressStep.data.phone.isValidPhoneNumber(ORIGIN)
            ) {
                originAddressStep.copy(status = READY)
            } else originAddressStep

            val shippingAddressStep = if (isCustomsFormRequired &&
                !shippingAddressStep.data.phone.isValidPhoneNumber(DESTINATION)
            ) {
                shippingAddressStep.copy(status = READY)
            } else shippingAddressStep

            val customsStep = customsStep.copy(
                isVisible = isCustomsFormRequired,
                data = if (isCustomsFormRequired) customsStep.data else null
            )

            return copy(
                originAddressStep = originAddressStep,
                shippingAddressStep = shippingAddressStep,
                customsStep = customsStep
            )
        }

        private fun invalidateCarrierStep(): StepsState {
            val carrierStep = carrierStep.copy(data = emptyList(), status = NOT_READY)
            return copy(carrierStep = carrierStep)
        }

        private fun invalidateCustomsStep(): StepsState {
            val customsStep = customsStep.copy(data = null, status = NOT_READY)
            return copy(customsStep = customsStep)
        }

        private fun calculateNextStep(): StepsState {
            val nextStep = indexOfFirst { it.status != DONE }
            return if (nextStep != -1) updateStepStatus(get(nextStep), READY) else this
        }

        /**
         * Updates the steps so that only one step is READY at each time
         */
        private fun fixStates(): StepsState {
            val indexOfReadyState = indexOfFirst { it.status == READY }
            var updatedStates = this
            for (i in indexOfReadyState + 1 until backingList.size) {
                val step = get(i)
                if (step.status == DONE) continue
                updatedStates = updatedStates.updateStepStatus(step, NOT_READY)
            }
            return updatedStates
        }

        private fun <T> updateStepStatus(step: Step<T>, newStatus: StepStatus): StepsState {
            return when (step) {
                is OriginAddressStep -> copy(originAddressStep = originAddressStep.copy(status = newStatus))
                is ShippingAddressStep -> copy(shippingAddressStep = shippingAddressStep.copy(status = newStatus))
                is PackagingStep -> copy(packagingStep = packagingStep.copy(status = newStatus))
                is CustomsStep -> copy(customsStep = customsStep.copy(status = newStatus))
                is CarrierStep -> copy(carrierStep = carrierStep.copy(status = newStatus))
                is PaymentsStep -> copy(paymentsStep = paymentsStep.copy(status = newStatus))
            }
        }

        override fun iterator(): Iterator<Step<out Any?>> {
            return backingList.iterator()
        }

        operator fun get(index: Int): Step<out Any?> = backingList[index]
    }

    enum class StepStatus {
        NOT_READY, READY, DONE
    }

    sealed class Error {
        object DataLoadingError : Error()
        object AddressValidationError : Error()
        object PackagesLoadingError : Error()
        object PurchaseError : Error()
    }

    sealed class State : Parcelable {
        open val data: StateMachineData? = null

        @Parcelize
        object Idle : State()

        @Parcelize
        object DataLoadingFailure : State()

        @Parcelize
        data class DataLoading(val orderId: Long) : State()

        @Parcelize
        data class WaitingForInput(override val data: StateMachineData) : State()

        @Parcelize
        data class OriginAddressValidation(override val data: StateMachineData) : State()

        @Parcelize
        data class OriginAddressSuggestion(override val data: StateMachineData) : State()

        @Parcelize
        data class OriginAddressEditing(override val data: StateMachineData) : State()

        @Parcelize
        data class ShippingAddressValidation(override val data: StateMachineData) : State()

        @Parcelize
        data class ShippingAddressSuggestion(override val data: StateMachineData) : State()

        @Parcelize
        data class ShippingAddressEditing(override val data: StateMachineData) : State()

        @Parcelize
        data class PackageSelection(override val data: StateMachineData) : State()

        @Parcelize
        data class CustomsDeclaration(override val data: StateMachineData) : State()

        @Parcelize
        data class ShippingCarrierSelection(override val data: StateMachineData) : State()

        @Parcelize
        data class PaymentSelection(override val data: StateMachineData) : State()

        @Parcelize
        data class PurchaseLabels(override val data: StateMachineData, val fulfillOrder: Boolean) : State()
    }

    sealed class Event {
        abstract class UserInput : Event()

        data class FlowStarted(val orderId: Long) : Event()
        data class DataLoaded(
            val order: Order,
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

        object OriginAddressValidationStarted : UserInput()
        object EditOriginAddressRequested : UserInput()

        object ShippingAddressValidationStarted : UserInput()
        object EditShippingAddressRequested : UserInput()

        object PackageSelectionStarted : UserInput()
        object EditPackagingRequested : UserInput()
        object EditPackagingCanceled : Event()
        data class PackagesSelected(val shippingPackages: List<ShippingLabelPackage>) : Event()

        object CustomsDeclarationStarted : UserInput()
        object EditCustomsRequested : UserInput()
        object EditCustomsCanceled : Event()
        data class CustomsFormFilledOut(val customsPackages: List<CustomsPackage>) : Event()

        object ShippingCarrierSelectionStarted : UserInput()
        object EditShippingCarrierRequested : UserInput()
        data class ShippingCarrierSelected(val rates: List<ShippingRate>) : Event()
        object ShippingCarrierSelectionCanceled : Event()

        object PaymentSelectionStarted : UserInput()
        object EditPaymentRequested : UserInput()
        object EditPaymentCanceled : Event()
        data class PaymentSelected(val paymentMethod: PaymentMethod) : Event()

        data class PurchaseStarted(val fulfillOrder: Boolean) : UserInput()
        data class PurchaseSuccess(val labels: List<ShippingLabel>) : Event()
        object PurchaseFailed : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        data class ShowError(val error: Error) : SideEffect()

        data class ShowAddressSuggestion(
            val entered: Address,
            val suggested: Address,
            val type: AddressType
        ) : SideEffect()

        data class OpenAddressEditor(
            val address: Address,
            val type: AddressType,
            val validationResult: ValidationResult? = null,
            val requiresPhoneNumber: Boolean
        ) : SideEffect()

        data class ShowPackageOptions(
            val shippingPackages: List<ShippingLabelPackage>
        ) : SideEffect()

        data class ShowCustomsForm(
            val originCountryCode: String,
            val destinationCountryCode: String,
            val shippingPackages: List<ShippingLabelPackage>,
            val customsPackages: List<CustomsPackage>
        ) : SideEffect()

        data class ShowCarrierOptions(val data: StateMachineData) : SideEffect()

        object ShowPaymentOptions : SideEffect()

        data class ShowLabelsPrint(
            val orderId: Long,
            val labels: List<ShippingLabel>
        ) : SideEffect()

        object TrackFlowStart : SideEffect()
        data class TrackCompletedStep(val step: Step<*>) : SideEffect()
    }

    class InvalidStateException(message: String) : Exception(message)

    data class Transition(val state: State, val sideEffect: SideEffect?)
}
