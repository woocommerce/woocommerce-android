package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import android.util.Log
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.FlowState.ORIGIN_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.FlowState.PACKAGING
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.FlowState.SHIPPING_ADDRESS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.Invalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.NotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.Valid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Data
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditOriginAddressTapped
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditPackagingAddressTapped
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.EditShippingAddressTapped
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.OriginAddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.OriginAddressNotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.OriginAddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.OriginAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.PackageSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingAddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingAddressNotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingAddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.Event.ShippingAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.LoadData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.NoOp
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.OpenOriginAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.OpenShippingAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowOriginAddressSuggestions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowPackagingDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ShowShippingAddressSuggestions
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.UpdateViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ValidateOriginAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelStateMachine.SideEffect.ValidateShippingAddress
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
                Log.d("onko: Effect", sideEffect.toString())
                when (sideEffect) {
                    is LoadData -> loadData(sideEffect.orderId)
                    is ShowError -> showError(sideEffect.error)
                    is ValidateOriginAddress -> validateOriginAddress(sideEffect.address)
                    ShowOriginAddressSuggestions -> TODO()
                    is OpenOriginAddressEditor -> TODO()
                    is ValidateShippingAddress -> validateShippingAddress(sideEffect.address)
                    ShowShippingAddressSuggestions -> TODO()
                    is OpenShippingAddressEditor -> TODO()
                    ShowPackagingDetails -> TODO()
                    is UpdateViewState -> updateViewState(sideEffect.data)
                    NoOp -> {}
                }
            }
        }
        stateMachine.start(arguments.orderIdentifier)
    }

    private fun updateViewState(data: Data) {
        val state = getFlowState(data)
        viewState = when (state) {
            ORIGIN_ADDRESS -> {
                 viewState.copy(
                     originAddressStep = Step(
                         details = data.originAddress.toString(),
                         isEnabled = true,
                         isContinueButtonVisible = true,
                         isEditButtonVisible = false
                     ),
                     shippingAddressStep = Step(
                         details = data.shippingAddress.toString(),
                         isEnabled = false,
                         isContinueButtonVisible = false,
                         isEditButtonVisible = false
                     ),
                     packagingDetailsStep = Step(
                         details = "Select packaging details",
                         isEnabled = false,
                         isContinueButtonVisible = false,
                         isEditButtonVisible = false
                     )
                 )
            }
            SHIPPING_ADDRESS -> {
                viewState.copy(
                    originAddressStep = Step(
                        details = data.originAddress.toString(),
                        isEnabled = true,
                        isContinueButtonVisible = false,
                        isEditButtonVisible = true
                    ),
                    shippingAddressStep = Step(
                        details = data.shippingAddress.toString(),
                        isEnabled = true,
                        isContinueButtonVisible = true,
                        isEditButtonVisible = false
                    ),
                    packagingDetailsStep = Step(
                        details = "Select packaging details",
                        isEnabled = false,
                        isContinueButtonVisible = false,
                        isEditButtonVisible = false
                    )
                )
            }
            PACKAGING -> {
                viewState.copy(
                    originAddressStep = Step(
                        details = data.originAddress.toString(),
                        isEnabled = true,
                        isContinueButtonVisible = false,
                        isEditButtonVisible = true
                    ),
                    shippingAddressStep = Step(
                        details = data.shippingAddress.toString(),
                        isEnabled = true,
                        isContinueButtonVisible = false,
                        isEditButtonVisible = true
                    ),
                    packagingDetailsStep = Step(
                        details = "Small package 1",
                        isEnabled = true,
                        isContinueButtonVisible = true,
                        isEditButtonVisible = false
                    )
                )
            }
            else -> { viewState }
        }
    }

    private fun showError(error: Error) {
        when (error) {
            DataLoadingError -> triggerEvent(ShowSnackbar(R.string.dashboard_stats_error))
        }
    }

    private fun loadData(orderId: String) {
        val order = requireNotNull(orderDetailRepository.getOrder(orderId))
        stateMachine.handleEvent(DataLoaded(order.billingAddress, order.shippingAddress))
    }

    private fun validateOriginAddress(address: Address) {
        when (addressValidator.validateAddress(address)) {
            Valid -> stateMachine.handleEvent(OriginAddressValidated(address))
            Invalid -> stateMachine.handleEvent(OriginAddressInvalid)
            NotRecognized -> stateMachine.handleEvent(OriginAddressNotRecognized)
        }
    }

    private fun validateShippingAddress(address: Address) {
        when (addressValidator.validateAddress(address)) {
            Valid -> stateMachine.handleEvent(ShippingAddressValidated(address))
            Invalid -> stateMachine.handleEvent(ShippingAddressInvalid)
            NotRecognized -> stateMachine.handleEvent(ShippingAddressNotRecognized)
        }
    }

    private fun getFlowState(data: Data): FlowState {
        return when {
            data.isShippingAddressValidated -> PACKAGING
            data.isOriginAddressValidated -> SHIPPING_ADDRESS
            else -> ORIGIN_ADDRESS
        }
    }

    fun onEditOriginAddressButtonTapped() {
        stateMachine.handleEvent(EditOriginAddressTapped)
    }

    fun onEditShippingAddressButtonTapped() {
        stateMachine.handleEvent(EditShippingAddressTapped)
    }

    fun onEditPackagingButtonTapped() {
        stateMachine.handleEvent(EditPackagingAddressTapped)
    }

    fun onValidateOriginButtonTapped() {
        stateMachine.handleEvent(OriginAddressValidationStarted)
    }

    fun onValidateShippingButtonTapped() {
        stateMachine.handleEvent(ShippingAddressValidationStarted)
    }

    fun onShowPackagingDetailsButtonTapped() {
        stateMachine.handleEvent(PackageSelectionStarted)
    }

    @Parcelize
    data class ViewState(
        val originAddressStep: Step? = null,
        val shippingAddressStep: Step? = null,
        val packagingDetailsStep: Step? = null
    ) : Parcelable

    @Parcelize
    data class Step(
        val details: String,
        val isEnabled: Boolean,
        val isContinueButtonVisible: Boolean,
        val isEditButtonVisible: Boolean
    ) : Parcelable

    private enum class FlowState {
        ORIGIN_ADDRESS, SHIPPING_ADDRESS, PACKAGING, CARRIER, PAYMENT
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<CreateShippingLabelViewModel>
}
