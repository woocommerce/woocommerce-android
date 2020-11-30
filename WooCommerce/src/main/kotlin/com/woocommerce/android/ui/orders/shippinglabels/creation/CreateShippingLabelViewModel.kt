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
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Data
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.CustomsDeclarationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.CustomsFormFilledOut
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditCustomsRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditOriginAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditPackagingRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditShippingAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditShippingCarrierRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressEditFinished
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressNotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditPaymentRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.OriginAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PackageSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PackagesSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PaymentSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PaymentSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingCarrierSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingCarrierSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.SuggestedAddressSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.FlowStep
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
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowCarrierOptions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowCustomsForm
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowAddressSuggestion
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowPackageOptions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowPaymentDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.UpdateViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ValidateAddress
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
    private val stateMachine: ShippingLabelStateMachine,
    private val addressValidator: ShippingAddressValidator
) : ScopedViewModel(savedState, dispatchers) {
    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        initializeStateMachine()
    }

    private fun initializeStateMachine() {
        launch {
            stateMachine.effects.collect { sideEffect ->
                when (sideEffect) {
                    NoOp -> null
                    is ShowError -> {
                        showError(sideEffect.error)
                        null
                    }
                    is UpdateViewState -> {
                        updateViewState(sideEffect.data)
                        null
                    }
                    is LoadData -> loadData(sideEffect.orderId)
                    is ValidateAddress -> validateAddress(sideEffect.address)
                    is OpenAddressEditor -> AddressEditFinished(sideEffect.address)
                    is ShowAddressSuggestion -> SuggestedAddressSelected(sideEffect.entered)
                    is ShowPackageOptions -> PackagesSelected
                    is ShowCustomsForm -> CustomsFormFilledOut
                    is ShowCarrierOptions -> ShippingCarrierSelected
                    is ShowPaymentDetails -> PaymentSelected
                }.also { event ->
                    event?.let { stateMachine.handleEvent(it) }
                }
            }
        }
        stateMachine.start(arguments.orderIdentifier)
    }

    private fun updateViewState(data: Data) {
        val latestStep = data.stepsDone.maxBy { it.ordinal } ?: ORIGIN_ADDRESS
        viewState = when (latestStep) {
            ORIGIN_ADDRESS -> {
                 viewState.copy(
                     originAddressStep = Step.current(data.originAddress.toString()),
                     shippingAddressStep = Step.notDone(data.shippingAddress.toString()),
                     packagingDetailsStep = Step.notDone(),
                     customsStep = Step.notDone(),
                     carrierStep = Step.notDone(),
                     paymentStep = Step.notDone()
                 )
            }
            SHIPPING_ADDRESS -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.current(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.notDone(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            PACKAGING -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.current(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            CUSTOMS -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.current(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone()
                )
            }
            CARRIER -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.done(),
                    carrierStep = Step.current(),
                    paymentStep = Step.notDone()
                )
            }
            PAYMENT -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(),
                    customsStep = Step.done(),
                    carrierStep = Step.done(),
                    paymentStep = Step.current()
                )
            }
            DONE -> {
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
            DataLoadingError -> triggerEvent(ShowSnackbar(R.string.dashboard_stats_error))
        }
    }

    private fun loadData(orderId: String): Event {
        val order = requireNotNull(orderDetailRepository.getOrder(orderId))
        return DataLoaded(order.billingAddress, order.shippingAddress)
    }

    private fun validateAddress(address: Address): Event {
        return when (val result = addressValidator.validateAddress(address)) {
            Valid -> AddressValidated(address)
            is Invalid -> AddressInvalid(result.suggested)
            NotRecognized -> AddressNotRecognized
        }
    }

    fun onEditButtonTapped(step: FlowStep) {
        when (step) {
            ORIGIN_ADDRESS -> EditOriginAddressRequested
            SHIPPING_ADDRESS -> EditShippingAddressRequested
            PACKAGING -> EditPackagingRequested
            CUSTOMS -> EditCustomsRequested
            CARRIER -> EditShippingCarrierRequested
            PAYMENT -> EditPaymentRequested
            DONE -> null
        }.also { event ->
            event?.let { stateMachine.handleEvent(it) }
        }
    }

    fun onContinueButtonTapped(step: FlowStep) {
        when (step) {
            ORIGIN_ADDRESS -> OriginAddressValidationStarted
            SHIPPING_ADDRESS -> ShippingAddressValidationStarted
            PACKAGING -> PackageSelectionStarted
            CUSTOMS -> CustomsDeclarationStarted
            CARRIER -> ShippingCarrierSelectionStarted
            PAYMENT -> PaymentSelectionStarted
            DONE -> null
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
