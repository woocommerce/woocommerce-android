package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.Invalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.NotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.Valid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Data
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class CreateShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val repository: ShippingLabelRepository,
    private val networkStatus: NetworkStatus,
    private val wcStore: WooCommerceStore,
    private val orderDetailRepository: OrderDetailRepository,
    private val stateMachine: ShippingLabelsStateMachine,
    private val addressValidator: ShippingAddressValidator
) : ScopedViewModel(savedState, dispatchers) {
    // TODO: temporary until the fragment is merged
//    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        initializeStateMachine()
    }

    private fun initializeStateMachine() {
        launch {
            stateMachine.effects.collect { sideEffect ->
                when (sideEffect) {
                    SideEffect.NoOp -> null
                    is SideEffect.ShowError -> {
                        showError(sideEffect.error)
                        null
                    }
                    is SideEffect.UpdateViewState -> {
                        updateViewState(sideEffect.data)
                        null
                    }
                    is SideEffect.LoadData -> loadData(sideEffect.orderId)
                    is SideEffect.ValidateAddress -> validateAddress(sideEffect.address)
                    is SideEffect.OpenAddressEditor -> Event.AddressEditFinished(sideEffect.address)
                    is SideEffect.ShowAddressSuggestion -> Event.SuggestedAddressSelected(sideEffect.entered)
                    is SideEffect.ShowPackageOptions -> Event.PackagesSelected
                    is SideEffect.ShowCustomsForm -> Event.CustomsFormFilledOut
                    is SideEffect.ShowCarrierOptions -> Event.ShippingCarrierSelected
                    is SideEffect.ShowPaymentDetails -> Event.PaymentSelected
                }.also { event ->
                    event?.let { stateMachine.handleEvent(it) }
                }
            }
        }
        // TODO: temporary until the fragment is merged
//        stateMachine.start(arguments.orderIdentifier)
    }

    private fun updateViewState(data: Data) {
        val latestStep = data.stepsDone.maxBy { it.ordinal } ?: FlowStep.ORIGIN_ADDRESS
        viewState = when (latestStep) {
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
            Error.DataLoadingError -> triggerEvent(ShowSnackbar(R.string.dashboard_stats_error))
        }
    }

    private fun loadData(orderId: String): Event {
        val order = requireNotNull(orderDetailRepository.getOrder(orderId))
        return Event.DataLoaded(order.billingAddress, order.shippingAddress)
    }

    private fun validateAddress(address: Address): Event {
        return when (val result = addressValidator.validateAddress(address)) {
            Valid -> Event.AddressValidated(address)
            is Invalid -> Event.AddressInvalid(result.suggested)
            NotRecognized -> Event.AddressNotRecognized
        }
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
            event?.let { stateMachine.handleEvent(it) }
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
        val paymentStep: Step? = null
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
