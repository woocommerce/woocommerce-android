package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPackageDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPaymentDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.Failed
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.Loading
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.WaitingForInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Data
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.PackagesLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressChangeSuggested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressEditCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidationFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPackagingCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPaymentCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PackagesSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PaymentSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressAccepted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressDiscarded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.FlowStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.text.DecimalFormat

@ExperimentalCoroutinesApi
class CreateShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository,
    private val stateMachine: ShippingLabelsStateMachine,
    private val addressValidator: ShippingLabelAddressValidator,
    private val site: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val accountStore: AccountStore,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val STATE_KEY = "state"
    }

    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    init {
        initializeStateMachine()
    }

    private fun initializeStateMachine() {
        launch {
            stateMachine.transitions.collect { transition ->
                when (transition.state) {
                    is State.DataLoading -> viewState = viewState.copy(uiState = Loading)
                    is State.DataLoadingFailure -> viewState = viewState.copy(uiState = Failed)
                    is State.WaitingForInput -> {
                        viewState = viewState.copy(uiState = WaitingForInput)
                        updateViewState(transition.state.data)
                    }
                    else -> { }
                }
                transition.sideEffect?.let { sideEffect ->
                    when (sideEffect) {
                        SideEffect.NoOp -> {
                        }
                        is SideEffect.ShowError -> showError(sideEffect.error)
                        is SideEffect.LoadData -> handleResult { loadData(sideEffect.orderId) }
                        is SideEffect.ValidateAddress -> handleResult(
                            progressDialogTitle = string.shipping_label_edit_address_validation_progress_title,
                            progressDialogMessage = string.shipping_label_edit_address_progress_message
                        ) {
                            validateAddress(sideEffect.address, sideEffect.type)
                        }
                        is SideEffect.OpenAddressEditor -> triggerEvent(
                            ShowAddressEditor(
                                sideEffect.address,
                                sideEffect.type,
                                sideEffect.validationResult
                            )
                        )
                        is SideEffect.ShowAddressSuggestion -> triggerEvent(
                            ShowSuggestedAddress(
                                sideEffect.entered,
                                sideEffect.suggested,
                                sideEffect.type
                            )
                        )
                        is SideEffect.ShowPackageOptions -> openPackagesDetails(sideEffect.shippingPackages)
                        is SideEffect.ShowCustomsForm -> handleResult { Event.CustomsFormFilledOut }
                        is SideEffect.ShowCarrierOptions -> handleResult { Event.ShippingCarrierSelected }
                        is SideEffect.ShowPaymentOptions -> openPaymentDetails()
                    }
                }
                // save the current state
                savedState[STATE_KEY] = transition.state
            }
        }

        val state = savedState.get<State>(STATE_KEY)
        if (state != null) {
            stateMachine.initialize(state)
        } else {
            stateMachine.start(arguments.orderIdentifier)
        }
    }

    private suspend fun handleResult(
        @StringRes progressDialogTitle: Int = 0,
        @StringRes progressDialogMessage: Int = 0,
        action: suspend () -> Event
    ) {
        if (progressDialogTitle != 0) {
            val progressDialogState = ProgressDialogState(
                isShown = true,
                title = progressDialogTitle,
                message = progressDialogMessage
            )
            viewState = viewState.copy(progressDialogState = progressDialogState)
        }
        stateMachine.handleEvent(action())
        if (progressDialogTitle != 0) {
            viewState = viewState.copy(progressDialogState = ProgressDialogState(isShown = false))
        }
    }

    private fun openPackagesDetails(currentShippingPackages: List<ShippingLabelPackage>) {
        triggerEvent(
            ShowPackageDetails(
                orderIdentifier = arguments.orderIdentifier,
                shippingLabelPackages = currentShippingPackages
            )
        )
    }

    private fun openPaymentDetails() {
        triggerEvent(ShowPaymentDetails)
    }

    private fun updateViewState(data: Data) {
        viewState = when (data.flowSteps.maxBy { it.ordinal } ?: FlowStep.ORIGIN_ADDRESS) {
            FlowStep.ORIGIN_ADDRESS -> {
                viewState.copy(
                    originAddressStep = Step.current(data.originAddress.toString()),
                    shippingAddressStep = Step.notDone(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.notDone(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone(data.currentPaymentMethod.stepDescription)
                )
            }
            FlowStep.SHIPPING_ADDRESS -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.current(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.notDone(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone(data.currentPaymentMethod.stepDescription)
                )
            }
            FlowStep.PACKAGING -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.current(),
                    customsStep = Step.notDone(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone(data.currentPaymentMethod.stepDescription)
                )
            }
            FlowStep.CUSTOMS -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(getPackageDetailsDescription(data.shippingPackages)),
                    customsStep = Step.current(),
                    carrierStep = Step.notDone(),
                    paymentStep = Step.notDone(data.currentPaymentMethod.stepDescription)
                )
            }
            FlowStep.CARRIER -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(getPackageDetailsDescription(data.shippingPackages)),
                    customsStep = Step.done(),
                    carrierStep = Step.current(),
                    paymentStep = Step.notDone(data.currentPaymentMethod.stepDescription)
                )
            }
            FlowStep.PAYMENT -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(getPackageDetailsDescription(data.shippingPackages)),
                    customsStep = Step.done(),
                    carrierStep = Step.done(),
                    paymentStep = Step.current(data.currentPaymentMethod.stepDescription)
                )
            }
            FlowStep.DONE -> {
                viewState.copy(
                    originAddressStep = Step.done(data.originAddress.toString()),
                    shippingAddressStep = Step.done(data.shippingAddress.toString()),
                    packagingDetailsStep = Step.done(getPackageDetailsDescription(data.shippingPackages)),
                    customsStep = Step.done(),
                    carrierStep = Step.done(),
                    paymentStep = Step.done(data.currentPaymentMethod.stepDescription)
                )
            }
        }
    }

    private fun showError(error: Error) {
        when (error) {
            DataLoadingError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            AddressValidationError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            PackagesLoadingError -> triggerEvent(ShowSnackbar(string.shipping_label_packages_loading_error))
        }
    }

    private suspend fun loadData(orderId: String): Event {
        val order = requireNotNull(orderDetailRepository.getOrder(orderId))
        val accountSettings = shippingLabelRepository.getAccountSettings().let {
            if (it.isError) return Event.DataLoadingFailed
            it.model!!
        }
        return Event.DataLoaded(
            originAddress = getStoreAddress(),
            shippingAddress = order.shippingAddress,
            currentPaymentMethod = accountSettings.paymentMethods.find { it.id == accountSettings.selectedPaymentId })
    }

    private fun getStoreAddress(): Address {
        val siteSettings = wooStore.getSiteSettings(site.get())
        return Address(
            company = site.get().name,
            firstName = accountStore.account.firstName,
            lastName = accountStore.account.lastName,
            phone = "",
            email = "",
            country = siteSettings?.countryCode ?: "",
            state = siteSettings?.stateCode ?: "",
            address1 = siteSettings?.address ?: "",
            address2 = siteSettings?.address2 ?: "",
            city = siteSettings?.city ?: "",
            postcode = siteSettings?.postalCode ?: ""
        )
    }

    private suspend fun validateAddress(address: Address, type: AddressType): Event {
        return when (val result = addressValidator.validateAddress(address, type)) {
            ValidationResult.Valid -> AddressValidated(address)
            is ValidationResult.SuggestedChanges -> AddressChangeSuggested(result.suggested)
            is ValidationResult.NotFound,
            is ValidationResult.Invalid,
            is ValidationResult.NameMissing -> AddressInvalid(address, result)
            is ValidationResult.Error -> AddressValidationFailed
        }
    }

    private fun getPackageDetailsDescription(shippingPackages: List<ShippingLabelPackage>): String {
        val firstLine = if (shippingPackages.size == 1) {
            shippingPackages.first().selectedPackage!!.title
        } else {
            // TODO properly test this during M3
            resourceProvider.getString(
                string.shipping_label_multi_packages_items_count,
                shippingPackages.sumBy { it.items.size },
                shippingPackages.size
            )
        }

        val weightDimension = wooStore.getProductSettings(site.get())?.weightUnit ?: ""
        val stringResource = if (shippingPackages.size == 1) {
            string.shipping_label_single_package_total_weight
        } else {
            string.shipping_label_multi_packages_total_weight
        }
        val weightFormatted = with(DecimalFormat()) {
            maximumFractionDigits = 4
            minimumFractionDigits = 0
            format(shippingPackages.sumByDouble { it.weight })
        }

        val secondLine = resourceProvider.getString(
            stringResource,
            weightFormatted,
            weightDimension
        )

        return "$firstLine\n$secondLine"
    }

    private val PaymentMethod?.stepDescription: String?
        get() {
            if (this == null) return null
            return resourceProvider.getString(string.shipping_label_selected_payment_description, cardDigits)
        }

    fun retry() = stateMachine.handleEvent(Event.FlowStarted(arguments.orderIdentifier))

    fun onAddressEditConfirmed(address: Address) {
        stateMachine.handleEvent(AddressValidated(address))
    }

    fun onAddressEditCanceled() {
        stateMachine.handleEvent(AddressEditCanceled)
    }

    fun onSuggestedAddressDiscarded() {
        stateMachine.handleEvent(SuggestedAddressDiscarded)
    }

    fun onSuggestedAddressAccepted(address: Address) {
        stateMachine.handleEvent(SuggestedAddressAccepted(address))
    }

    fun onSuggestedAddressEditRequested(address: Address) {
        stateMachine.handleEvent(EditAddressRequested(address))
    }

    fun onPackagesUpdated(packages: List<ShippingLabelPackage>) {
        stateMachine.handleEvent(PackagesSelected(packages))
    }

    fun onPackagesEditCanceled() {
        stateMachine.handleEvent(EditPackagingCanceled)
    }

    fun onPaymentsUpdated(paymentMethod: PaymentMethod) {
        stateMachine.handleEvent(PaymentSelected(paymentMethod))
    }

    fun onPaymentsEditCanceled() {
        stateMachine.handleEvent(EditPaymentCanceled)
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
            event?.let {
                stateMachine.handleEvent(it)
            }
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

    override fun onCleared() {
        super.onCleared()
        shippingLabelRepository.clearCache()
    }

    @Parcelize
    data class ViewState(
        val uiState: UiState? = null,
        val originAddressStep: Step? = null,
        val shippingAddressStep: Step? = null,
        val packagingDetailsStep: Step? = null,
        val customsStep: Step? = null,
        val carrierStep: Step? = null,
        val paymentStep: Step? = null,
        val progressDialogState: ProgressDialogState = ProgressDialogState()
    ) : Parcelable

    enum class UiState {
        Loading, Failed, WaitingForInput
    }

    @Parcelize
    data class ProgressDialogState(
        val isShown: Boolean = false,
        @StringRes val title: Int = 0,
        @StringRes val message: Int = 0
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
