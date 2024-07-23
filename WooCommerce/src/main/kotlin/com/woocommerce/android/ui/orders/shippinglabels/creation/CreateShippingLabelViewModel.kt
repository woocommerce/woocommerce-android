package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_AMOUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FULFILL_ORDER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TOTAL_DURATION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CARRIER_RATES_SELECTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CARRIER_RATES_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CUSTOMS_COMPLETE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CUSTOMS_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DESTINATION_ADDRESS_COMPLETE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DESTINATION_ADDRESS_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORIGIN_ADDRESS_COMPLETE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORIGIN_ADDRESS_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PACKAGES_SELECTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PACKAGES_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PAYMENT_METHOD_SELECTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PAYMENT_METHOD_STARTED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PURCHASE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PURCHASE_INITIATED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PURCHASE_READY
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PURCHASE_SUCCEEDED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STARTED
import com.woocommerce.android.extensions.sumByBigDecimal
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowAddressEditor
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowCustomsForm
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPackageDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPaymentDetails
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowPrintShippingLabels
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowShippingRates
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowSuggestedAddress
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelEvent.ShowWooDiscountBottomSheet
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.Failed
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.Loading
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelViewModel.UiState.WaitingForInput
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.AddressValidationError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.DataLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.PackagesLoadingError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Error.PurchaseError
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressChangeSuggested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressEditCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressInvalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidated
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.AddressValidationFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.CustomsDeclarationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.CustomsFormFilledOut
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.DataLoaded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.DataLoadingFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditCustomsCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditCustomsRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditOriginAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPackagingCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPackagingRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPaymentCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditPaymentRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditShippingAddressRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.EditShippingCarrierRequested
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.FlowStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.OriginAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PackageSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PackagesSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PaymentSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PaymentSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PurchaseFailed
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PurchaseStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.PurchaseSuccess
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingAddressValidationStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelected
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelectionCanceled
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.ShippingCarrierSelectionStarted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressAccepted
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Event.SuggestedAddressDiscarded
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.SideEffect
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.State
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StateMachineData
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CarrierStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.CustomsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.OriginAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PackagingStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.PaymentsStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.Step.ShippingAddressStep
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.DONE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.NOT_READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepStatus.READY
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelsStateMachine.StepsState
import com.woocommerce.android.ui.orders.shippinglabels.creation.banner.CheckEUShippingScenario
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.text.DecimalFormat
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class CreateShippingLabelViewModel @Inject constructor(
    savedState: SavedStateHandle,
    parameterRepository: ParameterRepository,
    checkEUShippingScenario: CheckEUShippingScenario,
    private val orderDetailRepository: OrderDetailRepository,
    private val shippingLabelRepository: ShippingLabelRepository,
    private val stateMachine: ShippingLabelsStateMachine,
    private val addressValidator: ShippingLabelAddressValidator,
    private val site: SelectedSite,
    private val wooStore: WooCommerceStore,
    private val accountStore: AccountStore,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val getLocations: GetLocations,
    private val appPrefs: AppPrefsWrapper
) : ScopedViewModel(savedState) {
    companion object {
        private const val STATE_KEY = KEY_STATE
        private const val KEY_SHIPPING_LABELS_PARAMETERS = "key_shipping_labels_parameters"
    }

    private val parameters: SiteParameters by lazy {
        parameterRepository.getParameters(KEY_SHIPPING_LABELS_PARAMETERS, savedState)
    }

    private val arguments: CreateShippingLabelFragmentArgs by savedState.navArgs()

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val shouldDisplayShippingNotice = checkEUShippingScenario(stateMachine.transitions)
        .distinctUntilChanged()
        .onEach { if (it) AnalyticsTracker.track(AnalyticsEvent.EU_SHIPPING_NOTICE_SHOWN) }
        .asLiveData()

    init {
        initializeStateMachine()
    }

    private fun initializeStateMachine() {
        val state = savedState.get<State>(STATE_KEY)
        if (state != null) {
            stateMachine.initialize(state)
        } else {
            stateMachine.start(arguments.orderId)
        }

        launch {
            stateMachine.transitions.collect { transition ->
                // save the current state
                savedState[STATE_KEY] = transition.state

                when (transition.state) {
                    is State.DataLoading -> {
                        viewState = viewState.copy(uiState = Loading)
                        handleResult { loadData(transition.state.orderId) }
                    }
                    is State.DataLoadingFailure -> viewState = viewState.copy(uiState = Failed)
                    is State.WaitingForInput -> {
                        viewState = viewState.copy(
                            uiState = WaitingForInput,
                            progressDialogState = ProgressDialogState(isShown = false)
                        )
                        updateViewState(transition.state.data)
                    }
                    is State.OriginAddressValidation -> {
                        handleResult(
                            progressDialogTitle = string.shipping_label_edit_address_validation_progress_title,
                            progressDialogMessage = string.shipping_label_edit_address_progress_message
                        ) {
                            validateAddress(
                                address = transition.state.data.stepsState.originAddressStep.data,
                                type = ORIGIN,
                                isCustomsFormRequired = transition.state.data.isCustomsFormRequired
                            )
                        }
                    }
                    is State.ShippingAddressValidation -> {
                        handleResult(
                            progressDialogTitle = string.shipping_label_edit_address_validation_progress_title,
                            progressDialogMessage = string.shipping_label_edit_address_progress_message
                        ) {
                            validateAddress(
                                address = transition.state.data.stepsState.shippingAddressStep.data,
                                type = DESTINATION,
                                isCustomsFormRequired = transition.state.data.isCustomsFormRequired
                            )
                        }
                    }
                    is State.PurchaseLabels -> {
                        handleResult(
                            progressDialogTitle = string.shipping_label_create_purchase_progress_title,
                            progressDialogMessage = string.shipping_label_create_purchase_progress_message
                        ) {
                            purchaseLabels(transition.state.data, transition.state.fulfillOrder)
                        }
                    }
                    else -> {
                    }
                }
                transition.sideEffect?.let { sideEffect ->
                    when (sideEffect) {
                        SideEffect.NoOp -> {
                        }
                        is SideEffect.ShowError -> showError(sideEffect.error)
                        is SideEffect.OpenAddressEditor -> triggerEvent(
                            ShowAddressEditor(
                                sideEffect.address,
                                sideEffect.type,
                                sideEffect.validationResult,
                                sideEffect.isCustomsFormRequired
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
                        is SideEffect.ShowCustomsForm -> openCustomsForm(
                            sideEffect.originCountryCode,
                            sideEffect.destinationCountryCode,
                            sideEffect.shippingPackages,
                            sideEffect.customsPackages
                        )
                        is SideEffect.ShowCarrierOptions -> openShippingCarrierRates(sideEffect.data)
                        is SideEffect.ShowPaymentOptions -> openPaymentDetails()
                        is SideEffect.ShowLabelsPrint -> openPrintLabelsScreen(sideEffect.orderId, sideEffect.labels)
                        is SideEffect.TrackFlowStart -> trackFlowStart()
                        is SideEffect.TrackCompletedStep -> trackCompletedStep(sideEffect.step)
                    }
                }
            }
        }
    }

    private fun trackFlowStart() = AnalyticsTracker.track(
        AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW,
        mapOf(KEY_STATE to VALUE_STARTED)
    )

    private fun trackPurchaseInitiated(amount: BigDecimal, fulfillOrder: Boolean) {
        AnalyticsTracker.track(
            AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW,
            mapOf(
                KEY_STATE to VALUE_PURCHASE_INITIATED,
                KEY_AMOUNT to amount.toPlainString(),
                KEY_FULFILL_ORDER to fulfillOrder
            )
        )
    }

    private fun trackCompletedStep(step: Step<*>) {
        val action = when (step) {
            is OriginAddressStep -> VALUE_ORIGIN_ADDRESS_COMPLETE
            is ShippingAddressStep -> VALUE_DESTINATION_ADDRESS_COMPLETE
            is PackagingStep -> VALUE_PACKAGES_SELECTED
            is CarrierStep -> VALUE_CARRIER_RATES_SELECTED
            is CustomsStep -> VALUE_CUSTOMS_COMPLETE
            is PaymentsStep -> VALUE_PAYMENT_METHOD_SELECTED
        }
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW, mapOf(KEY_STATE to action))
    }

    private fun trackStartedStep(step: FlowStep) {
        val action = when (step) {
            FlowStep.ORIGIN_ADDRESS -> VALUE_ORIGIN_ADDRESS_STARTED
            FlowStep.SHIPPING_ADDRESS -> VALUE_DESTINATION_ADDRESS_STARTED
            FlowStep.PACKAGING -> VALUE_PACKAGES_STARTED
            FlowStep.CUSTOMS -> VALUE_CUSTOMS_STARTED
            FlowStep.CARRIER -> VALUE_CARRIER_RATES_STARTED
            FlowStep.PAYMENT -> VALUE_PAYMENT_METHOD_STARTED
        }
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW, mapOf(KEY_STATE to action))
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

    private fun openShippingCarrierRates(data: StateMachineData) {
        triggerEvent(
            ShowShippingRates(
                data.order,
                data.stepsState.originAddressStep.data,
                data.stepsState.shippingAddressStep.data,
                data.stepsState.packagingStep.data,
                data.stepsState.customsStep.data,
                data.stepsState.carrierStep.data
            )
        )
    }

    private fun openPackagesDetails(currentShippingPackages: List<ShippingLabelPackage>) {
        triggerEvent(
            ShowPackageDetails(
                orderId = arguments.orderId,
                shippingLabelPackages = currentShippingPackages
            )
        )
    }

    private fun openCustomsForm(
        originCountryCode: String,
        destinationCountryCode: String,
        shippingPackages: List<ShippingLabelPackage>,
        customsPackages: List<CustomsPackage>
    ) {
        triggerEvent(
            ShowCustomsForm(
                originCountryCode = originCountryCode,
                destinationCountryCode = destinationCountryCode,
                shippingPackages = shippingPackages,
                customsPackages = customsPackages,
                isEUShippingScenario = shouldDisplayShippingNotice.value ?: false
            )
        )
    }

    private fun openPaymentDetails() {
        triggerEvent(ShowPaymentDetails)
    }

    private fun openPrintLabelsScreen(orderId: Long, labels: List<ShippingLabel>) {
        triggerEvent(ShowPrintShippingLabels(orderId, labels))
    }

    private fun updateViewState(stateMachineData: StateMachineData) {
        fun <T> Step<T>.mapToUiState(): StepUiState {
            if (!isVisible) return StepUiState.hide()

            val description = when (this) {
                is OriginAddressStep -> data.toString()
                is ShippingAddressStep -> data.toString()
                is PackagingStep -> stepDescription
                is CustomsStep -> stepDescription
                is CarrierStep -> stepDescription
                is PaymentsStep -> stepDescription
            }

            return when (status) {
                NOT_READY -> StepUiState.notDone(description)
                READY -> StepUiState.current(description)
                DONE -> StepUiState.done(description)
            }
        }

        fun StepsState.getOrderSummary(): OrderSummaryState {
            val isVisible = originAddressStep.status == DONE &&
                shippingAddressStep.status == DONE &&
                packagingStep.status == DONE &&
                (!customsStep.isVisible || customsStep.status == DONE) &&
                carrierStep.status == DONE &&
                paymentsStep.status == DONE
            if (!isVisible) return OrderSummaryState()

            AnalyticsTracker.track(
                AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW,
                mapOf(KEY_STATE to VALUE_PURCHASE_READY)
            )

            with(stateMachineData.stepsState.carrierStep.data) {
                val price = sumByBigDecimal { it.price }
                val discount = sumByBigDecimal { it.discount }
                val individualPackagesPrices = if (size > 1) {
                    map { rate ->
                        val labelPackage = stateMachineData.stepsState.packagingStep.data
                            .first { it.packageId == rate.packageId }
                        Pair(labelPackage, rate.price)
                    }.toMap()
                } else {
                    emptyMap()
                }

                return OrderSummaryState(
                    isVisible = true,
                    individualPackagesPrices = individualPackagesPrices,
                    price = price,
                    discount = discount,
                    // TODO: Once we start supporting countries other than the US, we'll need to verify what currency the shipping labels purchases use
                    currency = parameters.currencyCode
                )
            }
        }

        viewState = viewState.copy(
            originAddressStep = stateMachineData.stepsState.originAddressStep.mapToUiState(),
            shippingAddressStep = stateMachineData.stepsState.shippingAddressStep.mapToUiState(),
            packagingDetailsStep = stateMachineData.stepsState.packagingStep.mapToUiState(),
            customsStep = stateMachineData.stepsState.customsStep.mapToUiState(),
            carrierStep = stateMachineData.stepsState.carrierStep.mapToUiState(),
            paymentStep = stateMachineData.stepsState.paymentsStep.mapToUiState(),
            orderSummaryState = stateMachineData.stepsState.getOrderSummary()
        )
    }

    private fun showError(error: Error) {
        when (error) {
            DataLoadingError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            AddressValidationError -> triggerEvent(ShowSnackbar(string.dashboard_stats_error))
            PackagesLoadingError -> triggerEvent(ShowSnackbar(string.shipping_label_packages_loading_error))
            PurchaseError -> triggerEvent(ShowSnackbar(string.shipping_label_create_purchase_error))
        }
    }

    private suspend fun loadData(orderId: Long): Event {
        val order = requireNotNull(orderDetailRepository.getOrderById(orderId))
        val accountSettings = shippingLabelRepository.getAccountSettings().let {
            if (it.isError) return DataLoadingFailed
            it.model!!
        }
        return DataLoaded(
            order = order,
            originAddress = getStoreAddress(),
            shippingAddress = getShippingAddress(order),
            currentPaymentMethod = accountSettings.paymentMethods.find { it.id == accountSettings.selectedPaymentId }
        )
    }

    private fun getStoreAddress(): Address {
        val siteSettings = wooStore.getSiteSettings(site.get())
        val (country, state) = getLocations(
            countryCode = siteSettings?.countryCode.orEmpty(),
            stateCode = siteSettings?.stateCode.orEmpty()
        )
        return Address(
            company = site.get().name,
            firstName = accountStore.account.firstName,
            lastName = accountStore.account.lastName,
            phone = appPrefs.getStorePhoneNumber(site.getSelectedSiteId()),
            email = "",
            country = country,
            state = state,
            address1 = siteSettings?.address ?: "",
            address2 = siteSettings?.address2 ?: "",
            city = siteSettings?.city ?: "",
            postcode = siteSettings?.postalCode ?: ""
        )
    }

    private fun getShippingAddress(order: Order): Address {
        val phoneNumber = order.shippingPhone
        return order.shippingAddress.copy(phone = phoneNumber)
    }

    private suspend fun validateAddress(address: Address, type: AddressType, isCustomsFormRequired: Boolean): Event {
        return when (val result = addressValidator.validateAddress(address, type, isCustomsFormRequired)) {
            ValidationResult.Valid -> AddressValidated(address)
            is ValidationResult.SuggestedChanges -> {
                if (result.isTrivial) {
                    AddressValidated(result.suggested)
                } else {
                    AddressChangeSuggested(result.suggested)
                }
            }
            is ValidationResult.NotFound,
            is ValidationResult.Invalid,
            is ValidationResult.NameMissing, ValidationResult.PhoneInvalid -> AddressInvalid(address, result)
            is ValidationResult.Error -> AddressValidationFailed
        }
    }

    private suspend fun purchaseLabels(data: StateMachineData, fulfillOrder: Boolean): Event {
        val amount = data.stepsState.carrierStep.data.sumByBigDecimal { it.price }
        trackPurchaseInitiated(amount, fulfillOrder)

        var wooResult: WooResult<List<ShippingLabel>>
        val duration = measureTimeMillis {
            wooResult = shippingLabelRepository.purchaseLabels(
                orderId = data.order.id,
                origin = data.stepsState.originAddressStep.data,
                destination = data.stepsState.shippingAddressStep.data,
                packages = data.stepsState.packagingStep.data,
                rates = data.stepsState.carrierStep.data,
                customsPackages = data.stepsState.customsStep.data
            )
        } / 1000.0

        return if (wooResult.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW,
                mapOf(
                    KEY_STATE to VALUE_PURCHASE_FAILED,
                    KEY_TOTAL_DURATION to duration
                )
            )
            PurchaseFailed
        } else {
            if (fulfillOrder) {
                orderDetailRepository.updateOrderStatus(
                    orderId = data.order.id,
                    newStatus = CoreOrderStatus.COMPLETED.value
                ).collect { updateOrderResult ->
                    when (updateOrderResult) {
                        is OptimisticUpdateResult -> {
                            // noop
                        }
                        is RemoteUpdateResult -> {
                            if (updateOrderResult.event.isError) {
                                AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_ORDER_FULFILL_FAILED)
                                triggerEvent(ShowSnackbar(string.shipping_label_create_purchase_fulfill_error))
                            } else {
                                AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_ORDER_FULFILL_SUCCEEDED)
                            }
                        }
                    }
                }
            }
            AnalyticsTracker.track(
                AnalyticsEvent.SHIPPING_LABEL_PURCHASE_FLOW,
                mapOf(
                    KEY_STATE to VALUE_PURCHASE_SUCCEEDED,
                    KEY_AMOUNT to amount,
                    KEY_TOTAL_DURATION to duration
                )
            )
            PurchaseSuccess(wooResult.model!!)
        }
    }

    private val PackagingStep.stepDescription: String
        get() {
            return if (status == DONE && data.isNotEmpty()) {
                val firstLine = if (data.size == 1) {
                    data.first().selectedPackage!!.title
                } else {
                    resourceProvider.getString(
                        string.shipping_label_multi_packages_items_count,
                        data.sumOf { it.itemsCount },
                        data.size
                    )
                }

                val weightDimension = wooStore.getProductSettings(site.get())?.weightUnit ?: ""
                val stringResource = if (data.size == 1) {
                    string.shipping_label_single_package_total_weight
                } else {
                    string.shipping_label_multi_packages_total_weight
                }
                val weightFormatted = with(DecimalFormat()) {
                    maximumFractionDigits = 4
                    minimumFractionDigits = 0
                    format(data.sumByFloat { it.weight })
                }

                val secondLine = resourceProvider.getString(
                    stringResource,
                    weightFormatted,
                    weightDimension
                )

                "$firstLine\n$secondLine"
            } else {
                resourceProvider.getString(string.shipping_label_create_packaging_details_description)
            }
        }

    private val PaymentsStep.stepDescription: String?
        get() {
            return if (status == DONE && data != null) {
                resourceProvider.getString(string.shipping_label_selected_payment_description, data.cardDigits)
            } else {
                resourceProvider.getString(string.shipping_label_create_payment_description)
            }
        }

    private val CarrierStep.stepDescription: String
        get() {
            return if (status == DONE && data.isNotEmpty()) {
                val firstLine: String
                val secondLine: String
                if (data.size > 1) {
                    firstLine = resourceProvider.getString(string.shipping_label_selected_rates_description, data.size)
                    secondLine = resourceProvider.getString(
                        string.shipping_label_selected_rates_total_description,
                        data.sumByBigDecimal { it.price }.format()
                    )
                } else {
                    val rate = data.first()
                    firstLine = rate.serviceName

                    val total = data.sumByBigDecimal { it.price }.format()
                    val deliveryDays = StringUtils.getQuantityString(
                        resourceProvider = resourceProvider,
                        quantity = rate.deliveryDays,
                        default = string.shipping_label_shipping_carrier_rates_delivery_estimate_many,
                        one = string.shipping_label_shipping_carrier_rates_delivery_estimate_one
                    )
                    secondLine = "$total - $deliveryDays"
                }
                return "$firstLine\n$secondLine"
            } else {
                resourceProvider.getString(string.shipping_label_create_carrier_description)
            }
        }

    private val CustomsStep.stepDescription: String
        get() = resourceProvider.getString(
            if (status == DONE) {
                string.shipping_label_create_customs_done
            } else {
                string.shipping_label_create_customs_description
            }
        )

    fun retry() = stateMachine.handleEvent(FlowStarted(arguments.orderId))

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

    fun onShippingCarriersSelected(rates: List<ShippingRate>) {
        stateMachine.handleEvent(ShippingCarrierSelected(rates))
    }

    fun onShippingCarrierSelectionCanceled() {
        stateMachine.handleEvent(ShippingCarrierSelectionCanceled)
    }

    fun onCustomsFilledOut(customsPackages: List<CustomsPackage>) {
        stateMachine.handleEvent(CustomsFormFilledOut(customsPackages))
    }

    fun onCustomsEditCanceled() {
        stateMachine.handleEvent(EditCustomsCanceled)
    }

    fun onWooDiscountInfoClicked() {
        AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_DISCOUNT_INFO_BUTTON_TAPPED)
        triggerEvent(ShowWooDiscountBottomSheet)
    }

    fun onPurchaseButtonClicked(fulfillOrder: Boolean) {
        stateMachine.handleEvent(PurchaseStarted(fulfillOrder))
    }

    fun onEditButtonTapped(step: FlowStep) {
        trackStartedStep(step)
        when (step) {
            FlowStep.ORIGIN_ADDRESS -> EditOriginAddressRequested
            FlowStep.SHIPPING_ADDRESS -> EditShippingAddressRequested
            FlowStep.PACKAGING -> EditPackagingRequested
            FlowStep.CUSTOMS -> EditCustomsRequested
            FlowStep.CARRIER -> EditShippingCarrierRequested
            FlowStep.PAYMENT -> EditPaymentRequested
        }.also { event ->
            event.let {
                stateMachine.handleEvent(it)
            }
        }
    }

    fun onContinueButtonTapped(step: FlowStep) {
        trackStartedStep(step)
        when (step) {
            FlowStep.ORIGIN_ADDRESS -> OriginAddressValidationStarted
            FlowStep.SHIPPING_ADDRESS -> ShippingAddressValidationStarted
            FlowStep.PACKAGING -> PackageSelectionStarted
            FlowStep.CUSTOMS -> CustomsDeclarationStarted
            FlowStep.CARRIER -> ShippingCarrierSelectionStarted
            FlowStep.PAYMENT -> PaymentSelectionStarted
        }.also { event ->
            event.let { stateMachine.handleEvent(it) }
        }
    }

    fun onBackButtonClicked() {
        val stepsState = stateMachine.transitions.value.state.data?.stepsState
        if (stepsState?.any { it.status == DONE } == true) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = { _, _ ->
                        triggerEvent(Exit)
                    }
                )
            )
        } else {
            triggerEvent(Exit)
        }
    }

    fun onShippingNoticeLearnMoreClicked(learnMoreUrl: String) {
        triggerEvent(OpenShippingInstructions(learnMoreUrl))
    }

    override fun onCleared() {
        super.onCleared()
        shippingLabelRepository.clearCache()
    }

    private fun BigDecimal.format(): String {
        return PriceUtils.formatCurrency(this, parameters.currencyCode, currencyFormatter)
    }

    @Parcelize
    data class ViewState(
        val uiState: UiState = WaitingForInput,
        val originAddressStep: StepUiState? = null,
        val shippingAddressStep: StepUiState? = null,
        val packagingDetailsStep: StepUiState? = null,
        val customsStep: StepUiState? = null,
        val carrierStep: StepUiState? = null,
        val paymentStep: StepUiState? = null,
        val orderSummaryState: OrderSummaryState = OrderSummaryState(),
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
    data class OrderSummaryState(
        val isVisible: Boolean = false,
        val individualPackagesPrices: Map<ShippingLabelPackage, BigDecimal> = emptyMap(),
        val price: BigDecimal = BigDecimal.ZERO,
        val discount: BigDecimal = BigDecimal.ZERO,
        val currency: String? = null
    ) : Parcelable

    @Parcelize
    data class StepUiState(
        val isVisible: Boolean = true,
        val details: String? = null,
        val isEnabled: Boolean? = null,
        val isContinueButtonVisible: Boolean? = null,
        val isEditButtonVisible: Boolean? = null,
        val isHighlighted: Boolean? = null
    ) : Parcelable {
        companion object {
            fun hide() = StepUiState(
                isVisible = false
            )

            fun notDone(newDetails: String? = null) = StepUiState(
                details = newDetails,
                isEnabled = false,
                isContinueButtonVisible = false,
                isEditButtonVisible = false,
                isHighlighted = false
            )

            fun current(newDetails: String? = null) = StepUiState(
                details = newDetails,
                isEnabled = true,
                isContinueButtonVisible = true,
                isEditButtonVisible = false,
                isHighlighted = true
            )

            fun done(newDetails: String? = null) = StepUiState(
                details = newDetails,
                isEnabled = true,
                isContinueButtonVisible = false,
                isEditButtonVisible = true,
                isHighlighted = false
            )
        }
    }

    enum class FlowStep {
        ORIGIN_ADDRESS, SHIPPING_ADDRESS, PACKAGING, CUSTOMS, CARRIER, PAYMENT
    }

    data class OpenShippingInstructions(val url: String) : MultiLiveEvent.Event()
}
