package com.woocommerce.android.ui.orders.wooshippinglabels

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CARRIER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_STARTED
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState.Declared
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.HazmatState.NoSelection
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.NotSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.ShippingRatesState
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.address.ObserveShippingLabelNotice
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.VerifyDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.FetchOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.ObserveOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBannerUiState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbarData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.CreateDefaultCustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireCustomsForm
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.WooShippingCustomsValidator
import com.woocommerce.android.ui.orders.wooshippinglabels.domain.DownloadAndPrintInvoiceUseCase
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.models.toAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.ObserveShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.datasource.WooShippingRateModel
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.GetShippingRates
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.InvalidDestinationNameRateException
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.NoAvailableRatesException
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateOption
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@SuppressWarnings("LargeClass")
@HiltViewModel
class WooShippingLabelCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository,
    private val getShipments: GetShipments,
    private val currencyFormatter: CurrencyFormatter,
    private val observeOriginAddresses: ObserveOriginAddresses,
    private val fetchOriginAddresses: FetchOriginAddresses,
    private val getShippingRates: GetShippingRates,
    private val purchaseShippingLabel: PurchaseShippingLabel,
    private val markOrderAsComplete: MarkOrderAsComplete,
    observeAccountSettings: ObserveAccountSettings,
    private val fetchAccountSettings: FetchAccountSettings,
    private val addressValidationHelper: AddressValidationHelper,
    private val verifyDestinationAddress: VerifyDestinationAddress,
    private val observeShippingLabelNotice: ObserveShippingLabelNotice,
    private val createDefaultCustomsData: CreateDefaultCustomsData,
    private val shouldRequireCustoms: ShouldRequireCustomsForm,
    private val customsValidator: WooShippingCustomsValidator,
    private val fetchShippingLabelFile: FetchShippingLabelFile,
    private val observeShippingLabelStatus: ObserveShippingLabelStatus,
    private val downloadAndPrintInvoiceUseCase: DownloadAndPrintInvoiceUseCase,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingLabelCreationFragmentArgs by savedState.navArgs()

    private var printJob: Job? = null

    // Carrier whose Terms of Service sheet is currently being shown, so the acceptance event
    // can report the same carrier. Set when navigating to the ToS screen, cleared on acceptance.
    private var pendingTermsOfServiceCarrier: String? = null

    var snackbarData by mutableStateOf<ShippingLabelsSnackbarData?>(null)

    private val emptyOrder = Order.getEmptyOrder(Date(), Date())
    private val order = MutableStateFlow(emptyOrder)
    private val destinationAddress = MutableStateFlow(DestinationShippingAddress.EMPTY)
    private val shippingAddresses = MutableStateFlow<List<WooShippingAddresses>>(emptyList())
    private val loadTrigger = MutableSharedFlow<Unit>()

    private val shipments = MutableStateFlow<List<ShipmentUIModel>>(emptyList())
    private val shipmentItems = MutableStateFlow<List<List<ShippableItemModel>>>(emptyList())

    private val selectedPackagesFlow = MutableStateFlow<List<PackageData?>>(emptyList())
    private val customsFormDataFlow = MutableStateFlow<List<CustomsData?>>(emptyList())
    private val shipmentWeightsFlow = MutableStateFlow<List<Float?>>(emptyList())
    private val packageSelectionsFlow = MutableStateFlow<List<PackageSelectionState>>(emptyList())
    private val customsStatesFlow = MutableStateFlow<List<CustomsState>>(emptyList())
    private val hazmatStatesFlow = MutableStateFlow<List<HazmatState>>(emptyList())

    private val accountSettings = observeAccountSettings()
        .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val uiState = MutableStateFlow(
        UIControlsState(
            markOrderComplete = false,
            selectedIndex = navArgs.shipmentId,
            paperSizeOption = WooShippingLabelPaperSize.LABEL
        )
    )

    private val selectedRatesSortOrdersFlow = MutableStateFlow<List<ShippingSortOption>>(emptyList())
    private val refreshShippingRates = MutableSharedFlow<Unit>()
    val weightInputsFlow = MutableStateFlow<List<WeightInput>>(emptyList())

    private val cheapestComparator = Comparator<ShippingRateUI> { r1, r2 ->
        r1.defaultRate.rate.price.compareTo(r2.defaultRate.rate.price)
    }
    private val fastestComparator = Comparator<ShippingRateUI> { r1, r2 ->
        r1.defaultRate.rate.deliveryDays.compareTo(r2.defaultRate.rate.deliveryDays)
    }

    private val selectedRatesFlow = MutableStateFlow<List<ShippingRateUI?>>(emptyList())
    private val shippingRatesListFlow = MutableStateFlow<List<Map<CarrierUI, List<ShippingRateUI>>>>(emptyList())
    private val shippingRatesStatesFlow = MutableStateFlow<List<ShippingRatesState>>(emptyList())

    private val selectedShipmentIndex: Int
        get() = uiState.value.selectedIndex

    val viewState: MutableStateFlow<WooShippingViewState> = MutableStateFlow(
        WooShippingViewState.Loading(R.string.shipping_label_create_title)
    )

    init {
        launch { observeShippingLabelInformation() }
        launch { getHazmatSelection() }
        launch { getDestinationAddress() }
        launch { trackScreenShownEvent() }
        launch { getSavedShipments() }
        launch { setAccountDefaults() }
        launch { getShippingAddresses() }
        launch { getOrderInformation() }
        launch { observeShipmentWeight() }
        launch { observePackageChanges() }
        launch { observeShippingRates() }
        launch { observeShippingRatesState() }
        launch { observeShippingAddresses() }
        launch { observeCustomsDataChanges() }
        launch { observeNotices() }
        launch { observeShippingLabelAsyncPurchaseStatus() }
    }

    private suspend fun trackScreenShownEvent() {
        val unfulfilledShipmentsCount = shipments.drop(1).first().count { !it.isPurchasedOrInProgress }
        analyticsTracker.track(
            AnalyticsEvent.WCS_CREATE_SHIPPING_LABEL_FORM_SHOWN,
            mapOf("unfulfilled_shipments_count" to unfulfilledShipmentsCount)
        )
    }

    private suspend fun getOrderInformation() {
        orderDetailRepository.getOrderById(navArgs.orderId)?.let {
            order.value = it
        } ?: run {
            triggerEvent(Event.ShowSnackbar(R.string.woo_shipping_labels_loading_order_error))
            postTriggerEvent(Event.Exit)
        }
    }

    private suspend fun observeNotices() =
        combine(
            shipments,
            uiState.map { it.selectedIndex }.distinctUntilChanged()
        ) { shipments, selectedShipmentIndex ->
            shipments[selectedShipmentIndex]
        }.flatMapLatest { shipment ->
            if (shipment.isPurchasedOrInProgress) {
                flowOf(null)
            } else {
                observeShippingLabelNotice(
                    shippingAddresses,
                    customsStatesFlow.filter { it.isNotEmpty() },
                    uiState.map { it.selectedIndex }.distinctUntilChanged(),
                    viewModelScope
                )
            }
        }.onStart { delay(NOTIFICATIONS_DELAY) }
            .collectLatest { noticeBanner ->
                uiState.update {
                    it.copy(
                        noticeBannerUiState = noticeBanner?.copy(
                            onTapped = {
                                when (noticeBanner.type) {
                                    NoticeType.MISSING_ORIGIN_ADDRESS,
                                    NoticeType.UNVERIFIED_ORIGIN_ADDRESS -> {
                                        shippingAddresses.value.getOrNull(selectedShipmentIndex)
                                            ?.shipFrom?.let { shipFrom ->
                                                onEditOriginAddress(shipFrom)
                                            }
                                    }

                                    NoticeType.MISSING_DESTINATION_ADDRESS,
                                    NoticeType.UNVERIFIED_DESTINATION_ADDRESS -> {
                                        shippingAddresses.value.getOrNull(selectedShipmentIndex)
                                            ?.shipTo?.let { shipTo ->
                                                onEditDestinationAddress(shipTo)
                                            }
                                    }

                                    NoticeType.MISSING_ITN -> {
                                        onEditCustomsClick()
                                    }

                                    else -> {}
                                }
                            }
                        )
                    )
                }
            }

    private suspend fun observeShippingLabelAsyncPurchaseStatus() {
        val shipmentWithIndex = uiState.map { it.selectedIndex }.distinctUntilChanged()
            .flatMapLatest { index ->
                shipments.map { it.getOrNull(index) }
                    .filterNotNull()
                    .map { Pair(it, index) }
            }

        shipmentWithIndex
            .filter { (shipment, _) -> shipment.label?.status == ShippingLabelStatus.PURCHASE_IN_PROGRESS }
            .flatMapLatest { (shipment, index) ->
                observeShippingLabelStatus(
                    orderId = navArgs.orderId,
                    labelId = shipment.label!!.labelId
                ).map {
                    Triple(shipment, index, it)
                }
            }.collect { (shipment, index, result) ->
                val originAddress = shippingAddresses.value.getOrNull(index)?.shipFrom?.toAddress()

                // If result has a label model update the label with it. Otherwise, just update the status.
                val newLabel = result.shippingLabelModel ?: shipment.label?.copy(status = result.status)
                updateShipment(index, shipment.copy(label = newLabel?.copy(originAddress = originAddress)))

                if (result.status == ShippingLabelStatus.PURCHASED) {
                    analyticsTracker.track(AnalyticsEvent.WCS_PURCHASE_STEP, mapOf(KEY_STATE to "purchase_success"))
                } else if (result.status == ShippingLabelStatus.PURCHASE_ERROR) {
                    analyticsTracker.track(
                        AnalyticsEvent.WCS_PURCHASE_STEP,
                        mapOf(KEY_STATE to "purchase_failed", KEY_ERROR to result.shippingLabelModel?.error)
                    )
                }

                if (result.status == ShippingLabelStatus.PURCHASED && uiState.value.markOrderComplete) {
                    markOrderAsComplete(navArgs.orderId).fold(
                        onSuccess = {
                            analyticsTracker.track(
                                stat = AnalyticsEvent.SHIPPING_LABEL_ORDER_FULFILL_SUCCEEDED,
                                properties = mapOf(AnalyticsTracker.KEY_IS_REVAMPED_FLOW to true)
                            )
                        },
                        onFailure = {
                            triggerEvent(Event.ShowSnackbar(R.string.woo_shipping_labels_order_completion_error))
                            analyticsTracker.track(
                                stat = AnalyticsEvent.SHIPPING_LABEL_ORDER_FULFILL_FAILED,
                                properties = mapOf(AnalyticsTracker.KEY_IS_REVAMPED_FLOW to true),
                                errorType = (it as? WooException)?.error?.type?.name,
                                errorContext = this@WooShippingLabelCreationViewModel.javaClass.simpleName,
                                errorDescription = (it as? WooException)?.error?.message
                            )
                        }
                    )
                }
            }
    }

    private suspend fun getHazmatSelection() {
        shipments.filter { it.isNotEmpty() }.first().let { shipmentList ->
            hazmatStatesFlow.value = shipmentList.map { shipment ->
                shipment.label?.hazmatCategory?.let { Declared(it) } ?: NoSelection
            }
        }
    }

    private suspend fun getDestinationAddress() {
        combine(
            order.drop(1),
            shipments.drop(1),
            uiState.map { it.selectedIndex }.distinctUntilChanged()
        ) { order, shipments, selectedIndex ->
            Pair(order, shipments[selectedIndex].label?.destinationAddress)
        }.collectLatest { (order, labelDestination) ->
            val orderShippingEmail = order.shippingAddress.email.ifBlank { order.billingAddress.email }

            if (labelDestination == null) {
                if (destinationAddress.value == DestinationShippingAddress.EMPTY) {
                    val defaultDestination = DestinationShippingAddress(
                        address = order.shippingAddress.copy(email = orderShippingEmail),
                        isVerified = false
                    )
                    destinationAddress.value = defaultDestination
                }

                if (addressValidationHelper.isMissingDestinationAddress(order.shippingAddress).not() &&
                    !destinationAddress.value.isVerified
                ) {
                    verifyDestinationAddress(order.id).fold(
                        onSuccess = {
                            destinationAddress.value = it.copy(
                                address = it.address.copy(email = orderShippingEmail)
                            )
                        },
                        onFailure = {
                            if (destinationAddress.value == DestinationShippingAddress.EMPTY) {
                                destinationAddress.value = DestinationShippingAddress(
                                    address = order.shippingAddress.copy(email = orderShippingEmail),
                                    isVerified = false
                                )
                            }
                        }
                    )
                }
            } else {
                // Using stored destination address for purchased labels
                destinationAddress.value = DestinationShippingAddress(
                    address = labelDestination.copy(email = orderShippingEmail),
                    isVerified = true
                )
            }
        }
    }

    private suspend fun getSavedShipments() {
        order.drop(1).collectLatest { order -> shipments.value = getShipments(order) }
    }

    private suspend fun setAccountDefaults() {
        accountSettings.first()?.let { accountSettings ->
            uiState.update {
                it.copy(
                    paperSizeOption = accountSettings.paperSize,
                    markOrderComplete = accountSettings.lastOrderCompleted
                )
            }
        }
    }

    @Suppress("ComplexCondition")
    @OptIn(FlowPreview::class)
    private suspend fun observeShippingRates() {
        combine(
            accountSettings,
            selectedPackagesFlow.filter { it.isNotEmpty() },
            shippingAddresses.filter { it.isNotEmpty() },
            shipmentWeightsFlow.filter { it.isNotEmpty() },
            customsStatesFlow.filter { it.isNotEmpty() },
            hazmatStatesFlow.filter { it.isNotEmpty() },
            refreshShippingRates.onStart { emit(Unit) },
        ) { accountSettings, selectedPackages, addresses, shipmentWeights, customState, hazmatStates, _ ->
            val customsFulfilled = customState[selectedShipmentIndex] is CustomsState.DataAvailable ||
                customState[selectedShipmentIndex] is CustomsState.NotRequired
            val selectedPackage = selectedPackages[selectedShipmentIndex]
            val selectedAddress = addresses[selectedShipmentIndex]
            if (selectedPackage != null && customsFulfilled) {
                ShippingRatesInfo(
                    orderId = navArgs.orderId,
                    packageSelected = selectedPackage,
                    shipFrom = selectedAddress.shipFrom,
                    shipTo = selectedAddress.shipTo.address,
                    weight = shipmentWeights[selectedShipmentIndex],
                    currencyCode = accountSettings?.storeOptions?.currencySymbol,
                    customsData = customsFormDataFlow.value[selectedShipmentIndex],
                    hazmatSelection = hazmatStates[selectedShipmentIndex].hazmatSelection
                )
            } else {
                null
            }
        }.debounce(MULTIPLE_CALLS_DELAY)
            .collectLatest { updateShippingRates(selectedShipmentIndex, it) }
    }

    private suspend fun observeShippingAddresses() {
        shippingAddresses.collect { shippingAddressesList ->
            customsFormDataFlow.update {
                val customsDataList = it.toMutableList()
                customsDataList.forEachIndexed { index, customsData ->
                    val addresses = shippingAddressesList.getOrNull(index) ?: return@forEachIndexed
                    val shipment = shipments.value.getOrNull(index) ?: return@forEachIndexed
                    if (customsData == null && shouldRequireCustoms(addresses)) {
                        customsDataList[index] = createDefaultCustomsData(
                            shipment = shipment,
                            shippingAddresses = addresses
                        )
                    } else if (customsData != null && !shouldRequireCustoms(addresses)) {
                        customsDataList[index] = null
                    }
                }
                customsDataList
            }
        }
    }

    private suspend fun observeShippingRatesState() {
        fun onSelectedShippingRateChanged(
            rate: ShippingRateUI?
        ) {
            if (selectedRatesFlow.value[selectedShipmentIndex] != rate) {
                selectedRatesFlow.update { it.toMutableList().apply { set(selectedShipmentIndex, rate) } }

                analyticsTracker.track(AnalyticsEvent.WCS_RATE_SELECTION_STEP, mapOf(KEY_STATE to "selected"))
            }
        }

        fun onSelectedRateOptionChanged(
            option: WooShippingRateModel.Option,
            checked: Boolean
        ) {
            selectedRatesFlow.update { currentRates ->
                val currentRate = currentRates[selectedShipmentIndex] ?: return
                val newRate = if (!option.isAdditionalOption) {
                    currentRate.copy(selectedOption = if (checked) option else ShippingRateOption.DEFAULT)
                } else {
                    currentRate.copy(
                        additionalSelectedOptions = currentRate.additionalSelectedOptions.let {
                            if (checked) it + option else it - option
                        }
                    )
                }
                currentRates.toMutableList().apply {
                    set(selectedShipmentIndex, newRate)
                }
            }
        }

        combine(
            shippingRatesListFlow.filter { it.isNotEmpty() },
            selectedRatesFlow.filter { it.isNotEmpty() },
            selectedRatesSortOrdersFlow.filter { it.isNotEmpty() },
        ) { shippingRates, selectedRates, selectedRatesSortOrders ->
            Triple(shippingRates, selectedRates, selectedRatesSortOrders)
        }
            .filter { (shippingRates, selectedRates, selectedRatesSortOrders) ->
                shippingRates.size == selectedRates.size && shippingRates.size == selectedRatesSortOrders.size
            }
            .map { (shippingRates, selectedRates, selectedRatesSortOrders) ->
                shippingRates.mapIndexed { index, map ->
                    if (map.isEmpty()) {
                        ShippingRatesState.NoAvailable
                    } else {
                        val sortedShippingRates = sortShippingRates(selectedRatesSortOrders[index], map)
                        preselectShippingRateIfNoneSelected(selectedRates, index, sortedShippingRates)
                        ShippingRatesState.DataState(
                            selectedRatesSortOrder = selectedRatesSortOrders[index],
                            shippingRates = sortedShippingRates,
                            selectedRate = selectedRates[index],
                            onSelectedShippingRateChanged = ::onSelectedShippingRateChanged,
                            onSelectedRateOptionChanged = ::onSelectedRateOptionChanged
                        )
                    }
                }
            }.collectLatest {
                shippingRatesStatesFlow.value = it
            }
    }

    private fun preselectShippingRateIfNoneSelected(
        selectedRates: List<ShippingRateUI?>,
        index: Int,
        sortedShippingRates: Map<CarrierUI, List<ShippingRateUI>>
    ) {
        if (selectedRates[index] == null) {
            val defaultSelectedRate = sortedShippingRates[sortedShippingRates.keys.first()]?.first()
            selectedRatesFlow.update {
                it.toMutableList().apply {
                    set(
                        selectedShipmentIndex,
                        defaultSelectedRate
                    )
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observeShipmentWeight() {
        combine(
            shipmentItems.filter { it.isNotEmpty() && it.size > selectedShipmentIndex },
            selectedPackagesFlow.filter { it.isNotEmpty() && it.size == shipments.value.size },
            weightInputsFlow
                .filter { it.isNotEmpty() && it.size == shipments.value.size }
                .debounce { if (it[selectedShipmentIndex].autoFilled) 0 else TYPING_DELAY }
        ) { shipmentItems, selectedPackage, weightInputs ->
            if (selectedPackage.size == shipments.value.size && weightInputs.size == shipments.value.size) {
                shipmentItems.mapIndexed { index, _ -> weightInputs[index].weight.toFloatOrNull() }
            } else {
                null
            }
        }.filterNotNull()
            .collectLatest { shipmentWeightsFlow.value = it }
    }

    private suspend fun observePackageChanges() {
        combine(
            selectedPackagesFlow.filter { it.isNotEmpty() },
            shipmentWeightsFlow.filter { it.isNotEmpty() },
            accountSettings.map { it?.storeOptions },
            packageSelectionsFlow.filter { it.isNotEmpty() }
        ) { packagesSelected, shipmentWeights, storeOptions, _ ->
            packagesSelected.mapIndexed { index, selectedPackageData ->
                val selectedShipmentWeight = shipmentWeights[index]
                if (selectedPackageData == null) {
                    NotSelected
                } else {
                    DataAvailable(
                        selectedPackage = selectedPackageData,
                        shipmentWeight = selectedShipmentWeight.toString(),
                        weightUnit = storeOptions?.weightUnit ?: ""
                    )
                }
            }
        }.collectLatest { packageSelectionsFlow.value = it }
    }

    private suspend fun observeCustomsDataChanges() {
        combine(
            shippingAddresses,
            customsFormDataFlow.filter { it.isNotEmpty() }
        ) { addresses, customsData ->
            customsData.mapIndexed { index, currentItemCustomsData ->
                val selectedAddress = addresses.getOrNull(index)
                val originCountryCode = selectedAddress?.shipFrom?.country.orEmpty()
                val destinationCountryCode = selectedAddress?.shipTo?.address?.country?.code.orEmpty()
                val customsFormValidationResult = currentItemCustomsData?.let {
                    customsValidator.validate(it, originCountryCode, destinationCountryCode)
                }

                when (customsFormValidationResult) {
                    WooShippingCustomsValidator.FormValidationResult.Valid ->
                        CustomsState.DataAvailable(customsData = currentItemCustomsData)

                    WooShippingCustomsValidator.FormValidationResult.ItnMissing ->
                        CustomsState.ItnMissing

                    WooShippingCustomsValidator.FormValidationResult.Invalid ->
                        CustomsState.Unavailable

                    null -> CustomsState.NotRequired
                }
            }
        }.collectLatest { customsStatesFlow.value = it }
    }

    private suspend fun createDefaultCustomsData(
        shipment: ShipmentUIModel,
        shippingAddresses: WooShippingAddresses,
    ): CustomsData? {
        return if (shouldRequireCustoms(shippingAddresses)) {
            createDefaultCustomsData(
                items = shipment.items,
                originAddress = shippingAddresses.shipFrom
            )
        } else {
            null
        }
    }

    private suspend fun getShippingAddresses() {
        combine(
            order.drop(1),
            destinationAddress,
            observeOriginAddresses(),
            shipments.drop(1),
            uiState.map { it.selectedIndex }.distinctUntilChanged(),
        ) { order, destination, originAddresses, shipments, selectedIndex ->
            val currentShipment = shipments[selectedIndex]
            val updatedAddress = if (currentShipment.isPurchasedOrInProgress) {
                val selectedAddress = shippingAddresses.value.getOrNull(selectedShipmentIndex)
                val shipFrom = selectedAddress?.shipFrom?.takeIf { it != OriginShippingAddress.EMPTY }
                    ?: currentShipment.label?.originAddress?.let { originAddress ->
                        OriginShippingAddress.fromAddress(originAddress).copy(isVerified = true)
                    }

                WooShippingAddresses(
                    shipFrom = shipFrom ?: OriginShippingAddress.EMPTY,
                    originAddresses = originAddresses.orEmpty(),
                    shipTo = DestinationShippingAddress(
                        address = currentShipment.label?.destinationAddress ?: destination.address,
                        isVerified = true
                    )
                )
            } else if (originAddresses.isNullOrEmpty()) {
                WooShippingAddresses.EMPTY
            } else {
                val selectedOriginAddress = getSelectedOriginAddress(originAddresses, selectedIndex)
                WooShippingAddresses(
                    shipFrom = selectedOriginAddress,
                    originAddresses = originAddresses,
                    shipTo = destination
                )
            }

            if (shippingAddresses.value.isEmpty()) {
                // Initialize the list during the initial load
                shippingAddresses.value = List(shipments.size) { WooShippingAddresses.EMPTY }
            }

            shippingAddresses.value.toMutableList().apply { set(selectedIndex, updatedAddress) }
        }.collect { shippingAddresses.value = it }
    }

    /**
     * Updates shippingRatesInfo for the selected shipment
     */
    @Suppress("CyclomaticComplexMethod")
    private suspend fun updateShippingRates(index: Int, shippingRatesInfo: ShippingRatesInfo?) {
        fun updateState(newState: ShippingRatesState) {
            shippingRatesStatesFlow.update {
                it.toMutableList().apply { set(index, newState) }
            }
        }

        when {
            shippingRatesInfo == null -> updateState(ShippingRatesState.NoAvailable)

            shippingRatesInfo.shipTo == null ||
                !addressValidationHelper.canFetchShippingRates(shippingRatesInfo.shipTo) ->
                updateState(
                    ShippingRatesState.MissingInfo(
                        missingTitle = R.string.woo_shipping_labels_shipping_rates_missing_destination,
                        missingDescription = R.string.woo_shipping_labels_shipping_rates_missing_destination_desc
                    )
                )

            shippingRatesInfo.weight == null || shippingRatesInfo.weight == 0f ->
                updateState(
                    ShippingRatesState.MissingInfo(
                        missingTitle = R.string.woo_shipping_labels_shipping_rates_missing_weight,
                        missingDescription = R.string.woo_shipping_labels_shipping_rates_missing_weight_desc
                    )
                )

            else -> {
                val sortOrder = selectedRatesSortOrdersFlow.value[index]
                updateState(ShippingRatesState.Loading(sortOrder))
                getShippingRates(
                    shippingRatesInfo.orderId,
                    shippingRatesInfo.packageSelected,
                    shippingRatesInfo.shipTo,
                    shippingRatesInfo.shipFrom,
                    shippingRatesInfo.weight,
                    shippingRatesInfo.currencyCode,
                    shippingRatesInfo.customsData,
                    shippingRatesInfo.hazmatSelection
                ).fold(
                    onSuccess = { result ->
                        shippingRatesListFlow.value = shippingRatesListFlow.value.toMutableList().apply {
                            set(index, result)
                        }
                        selectedRatesFlow.value = selectedRatesFlow.value.toMutableList().apply {
                            set(index, null)
                        }
                        trackShippingRatesLoading(isSuccess = true)
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is NoAvailableRatesException -> exception.messageResId
                            is InvalidDestinationNameRateException ->
                                R.string.woo_shipping_labels_package_creation_shipping_rates_destination_name_error
                            else -> R.string.woo_shipping_labels_package_creation_shipping_rates_loading_error
                        }

                        updateState(ShippingRatesState.Error(errorMessage))
                        trackShippingRatesLoading(isSuccess = false, error = exception.message)
                    }
                )
            }
        }
    }

    private fun trackShippingRatesLoading(isSuccess: Boolean, error: String? = null) {
        if (isSuccess) {
            analyticsTracker.track(AnalyticsEvent.WCS_RATE_SELECTION_STEP, mapOf(KEY_STATE to "loading_success"))
        } else {
            analyticsTracker.track(
                AnalyticsEvent.WCS_RATE_SELECTION_STEP,
                mapOf(KEY_STATE to "loading_failed", KEY_ERROR to error)
            )
        }
    }

    @Suppress("ComplexCondition", "LongMethod")
    private suspend fun observeShippingLabelInformation() {
        combine(
            accountSettings,
            order.drop(1),
            shipments.drop(1),
            shippingAddresses.drop(1),
            shippingRatesStatesFlow,
            packageSelectionsFlow,
            uiState,
            customsStatesFlow,
            hazmatStatesFlow
        ) { accountSettings, order, shipments, addresses, shippingRatesList,
            packageSelections, uiState, customsState, hazmatStates ->
            if (accountSettings == null) {
                return@combine WooShippingViewState.Error
            }

            shipmentItems.value = shipments.map { it.items }
            adjustFlowSizesToShipmentCount(shipments)

            val destinationStatus = when {
                addressValidationHelper.isMissingDestinationAddress(
                    addresses[uiState.selectedIndex].shipTo.address
                ) -> AddressStatus.MissingAddress

                addresses[uiState.selectedIndex].shipTo.isVerified -> AddressStatus.Verified
                else -> AddressStatus.Unverified
            }

            val shippingLineSummary = order.getShippingLinesSummary(currencyFormatter)
            val shipmentUIList = shipmentItems.value.mapIndexed { index, shippableItemModels ->
                shippableItemModels.toUIModel(
                    currencyFormatter,
                    accountSettings.storeOptions.dimensionUnit,
                    accountSettings.storeOptions.weightUnit,
                    shipments[index],
                    hazmatStatesFlow.value[index].hazmatSelection,
                    packageSelectionsFlow.value[index],
                    shippingRatesStatesFlow.value[index],
                    customsStatesFlow.value[index]
                )
            }

            return@combine WooShippingViewState.DataState(
                shipmentUIList = shipmentUIList,
                totalItems = shipmentItems.value.flatten().sumByFloat { it.quantity }.toInt(),
                totalItemsCost = shipmentItems.value.flatten().getFormattedTotalPrice(currencyFormatter),
                weightUnit = accountSettings.storeOptions.weightUnit,
                shippingLines = shippingLineSummary,
                shippingAddresses = addresses,
                uiState = uiState,
                destinationStatus = destinationStatus,
                paymentsSectionUI = PaymentsSectionUI(
                    selectedPaymentMethod = accountSettings.paymentMethodOptions.selectedPaymentMethod,
                    onEditPaymentMethodClicked = ::onEditPaymentMethodClicked
                ),
                purchaseSectionUI = PurchaseSectionUI(
                    isVisible = !shipmentUIList[uiState.selectedIndex].isReadOnly &&
                        shippingRatesStatesFlow.value[uiState.selectedIndex] is ShippingRatesState.DataState,
                    isOrderAlreadyCompleted = order.status == Order.Status.Completed,
                    markOrderComplete = uiState.markOrderComplete,
                    formattedPrice = shipmentUIList[uiState.selectedIndex].shipmentCostUI?.formattedTotalPrice,
                    onMarkOrderCompleteChange = ::onMarkOrderCompleteChange,
                    onPurchaseShippingLabel = ::onPurchaseShippingLabel
                ),
                isSplitShipmentsSupported = ciabSiteGateKeeper.isFeatureSupported(
                    feature = CIABAffectedFeature.WooShippingSplitShipments
                )
            )
        }.combine(loadTrigger.onStart { emit(Unit) }) { viewState, _ ->
            viewState
        }.collectLatest {
            viewState.value = it
        }
    }

    private suspend fun adjustFlowSizesToShipmentCount(shipments: List<ShipmentUIModel>) {
        suspend fun <T> MutableStateFlow<List<T>>.updateSize(defaultValue: suspend (Int) -> T) =
            update { currentList ->
                if (currentList.size <= shipments.size) {
                    currentList + List(shipments.size - currentList.size) { index ->
                        defaultValue(currentList.size + index)
                    }
                } else {
                    List(shipments.size) { defaultValue(it) }
                }
            }

        val originAddresses = shippingAddresses.value.first().originAddresses
        shippingAddresses.updateSize {
            WooShippingAddresses.EMPTY.copy(
                originAddresses = originAddresses,
                shipFrom = originAddresses.firstOrNull() ?: WooShippingAddresses.EMPTY.shipFrom
            )
        }
        selectedPackagesFlow.updateSize { null }
        customsFormDataFlow.updateSize { index ->
            createDefaultCustomsData(shipments[index], shippingAddresses.value[index])
        }
        shipmentWeightsFlow.updateSize { null }
        packageSelectionsFlow.updateSize { NotSelected }
        customsStatesFlow.updateSize { CustomsState.NotRequired }
        hazmatStatesFlow.updateSize { NoSelection }
        selectedRatesSortOrdersFlow.updateSize { ShippingSortOption.FASTEST }
        selectedRatesFlow.updateSize { null }
        shippingRatesListFlow.updateSize { emptyMap() }
        shippingRatesStatesFlow.updateSize { ShippingRatesState.NoAvailable }
        weightInputsFlow.updateSize { WeightInput("", false) }
    }

    private fun updateShipment(index: Int, shipment: ShipmentUIModel) {
        shipments.value = shipments.value.toMutableList().apply { set(index, shipment) }
    }

    fun onShipmentSplit(newShipments: List<ShipmentUIModel>) {
        if (selectedShipmentIndex >= newShipments.size) {
            uiState.update {
                it.copy(selectedIndex = it.selectedIndex.coerceAtMost(newShipments.size - 1))
            }
        }
        shipments.value = newShipments
        resetWeight(selectedShipmentIndex)
    }

    fun onShippingLabelRefunded(labelId: Long) {
        // Find the shipment with the given labelId
        val shipmentIndex = shipments.value.indexOfFirst { it.label?.labelId == labelId }

        // If the shipment is found, reset its state
        if (shipmentIndex != -1) {
            updateShipment(shipmentIndex, shipments.value[shipmentIndex].copy(label = null))
            selectedRatesFlow.value = selectedRatesFlow.value.toMutableList().apply { set(shipmentIndex, null) }
            selectedPackagesFlow.value = selectedPackagesFlow.value.toMutableList().apply { set(shipmentIndex, null) }
        }
    }

    private fun getSelectedOriginAddress(
        originAddresses: List<OriginShippingAddress>,
        selectedIndex: Int
    ): OriginShippingAddress {
        val selectedAddress = shippingAddresses.value.getOrNull(selectedIndex)?.shipFrom
            ?.takeIf { it != OriginShippingAddress.EMPTY }

        return selectedAddress?.let { currentAddress ->
            originAddresses.firstOrNull { it.id == currentAddress.id } ?: currentAddress
        } ?: originAddresses.first()
    }

    fun onSelectedShipmentChanged(index: Int) {
        if (index >= shipments.value.size) return // This can happen after shipment split when the UI is not updated yet

        uiState.value = uiState.value.copy(selectedIndex = index)
    }

    fun onOriginAddressSelected(address: OriginShippingAddress) {
        shippingAddresses.value.getOrNull(selectedShipmentIndex)?.let {
            shippingAddresses.value = shippingAddresses.value.toMutableList().apply {
                set(selectedShipmentIndex, it.copy(shipFrom = address))
            }
        }
    }

    fun onEditOriginAddress(address: OriginShippingAddress) {
        triggerEvent(NavigateToOriginAddressEdit(address))
    }

    fun onEditDestinationAddress(destinationAddress: DestinationShippingAddress) {
        triggerEvent(
            NavigateToDestinationAddressEdit(
                destinationAddress = destinationAddress,
                orderId = navArgs.orderId
            )
        )
    }

    fun onUpdateDestinationAddress(updatedDestinationAddress: DestinationShippingAddress) {
        destinationAddress.value = updatedDestinationAddress
    }

    fun onRefreshShippingRates() {
        launch { refreshShippingRates.emit(Unit) }
    }

    fun onMarkOrderCompleteChange(value: Boolean) {
        uiState.update { it.copy(markOrderComplete = value) }
    }

    fun onSelectPackageClicked() {
        launch {
            triggerEvent(
                NavigatePackageSelection(
                    storeOptions = accountSettings.first()?.storeOptions ?: StoreOptionsModel.EMPTY
                )
            )
        }
    }

    fun onCarrierTermsAccepted() {
        pendingTermsOfServiceCarrier?.let { carrier ->
            analyticsTracker.track(
                AnalyticsEvent.WCS_CARRIER_TOS,
                mapOf(KEY_STATE to TOS_STATE_ACCEPTED, KEY_CARRIER to carrier)
            )
            pendingTermsOfServiceCarrier = null
        }

        fun selectMatchingRate(
            newRates: Map<CarrierUI, List<ShippingRateUI>>,
            previouslySelectedRate: ShippingRateUI,
            onSelected: () -> Unit
        ) {
            val newRate = newRates.values.flatten().find {
                it.defaultRate.rate.serviceId == previouslySelectedRate.defaultRate.rate.serviceId
            }?.copy(
                selectedOption = previouslySelectedRate.selectedOption,
                additionalSelectedOptions = previouslySelectedRate.additionalSelectedOptions
            ) ?: return

            selectedRatesFlow.update { currentRates ->
                currentRates.toMutableList().apply {
                    set(selectedShipmentIndex, newRate)
                }
            }
            onSelected()
        }

        val selectedRate = selectedRatesFlow.value[selectedShipmentIndex] ?: return

        // Observe the shipping rates state and use the new rates after the refresh
        launch {
            val newRatesState = shippingRatesStatesFlow.drop(1)
                .filter { it[selectedShipmentIndex] !is ShippingRatesState.Loading }
                .first()
            if (newRatesState[selectedShipmentIndex] !is ShippingRatesState.DataState) {
                WooLog.w(WooLog.T.SHIPPING_LABELS, "Refreshing shipping rates failed")
                return@launch
            }
            val newRates = (newRatesState[selectedShipmentIndex] as ShippingRatesState.DataState).shippingRates
            selectMatchingRate(
                newRates = newRates,
                previouslySelectedRate = selectedRate,
                onSelected = {
                    onPurchaseShippingLabel()
                }
            )
        }

        // Trigger a refresh of the shipping rates
        launch { refreshShippingRates.emit(Unit) }
    }

    @Suppress("ComplexCondition")
    fun onPurchaseShippingLabel() {
        val selectedShipmentIndex = selectedShipmentIndex
        val selectedPackage = selectedPackagesFlow.value[selectedShipmentIndex]
        val selectedAddress = shippingAddresses.value.getOrNull(selectedShipmentIndex)
        val shippingRate = selectedRatesFlow.value[selectedShipmentIndex]
        val weight = shipmentWeightsFlow.value[selectedShipmentIndex]

        if (selectedPackage == null || selectedAddress == null || shippingRate == null || weight == null) return

        if (addressValidationHelper.isMissingOriginAddress(selectedAddress.shipFrom)) {
            showPurchaseOriginAddressErrorSnackbar(selectedAddress.shipFrom)
            return
        }

        if (!addressValidationHelper.isPhoneValidForShippingLabel(selectedAddress.shipTo.address.phone)) {
            showPurchasePhoneErrorSnackbar(selectedAddress.shipTo)
            return
        }

        val orderId = navArgs.orderId
        val lastOrderComplete = uiState.value.markOrderComplete
        val shippableItemsIdList = shipmentItems.value[selectedShipmentIndex].map { it.productId }
        val hazmatSelection = hazmatStatesFlow.value[selectedShipmentIndex].hazmatSelection

        updateShipment(
            selectedShipmentIndex,
            shipments.value[selectedShipmentIndex].copy(isPurchaseAPILoading = true)
        )

        val customsData = customsFormDataFlow.value[selectedShipmentIndex]

        analyticsTracker.track(AnalyticsEvent.WCS_PURCHASE_STEP, mapOf(KEY_STATE to VALUE_STARTED))

        launch {
            purchaseShippingLabel(
                orderId,
                shippableItemsIdList,
                selectedPackage,
                selectedShipmentIndex,
                selectedAddress.shipTo.address,
                selectedAddress.shipFrom,
                shippingRate,
                weight,
                lastOrderComplete,
                customsData,
                hazmatSelection
            ).fold(
                onSuccess = {
                    val updatedShipment = shipments.value[selectedShipmentIndex].copy(
                        isPurchaseAPILoading = false,
                        label = it.labels.firstOrNull()
                    )
                    updateShipment(selectedShipmentIndex, updatedShipment)
                },
                onFailure = { exception ->
                    handlePurchaseFailure(exception, selectedShipmentIndex, selectedAddress)
                }
            )
        }
    }

    private fun showPurchaseOriginAddressErrorSnackbar(originAddress: OriginShippingAddress) {
        snackbarData = ShippingLabelsSnackbarData(
            message = R.string.woo_shipping_labels_purchase_origin_address_error,
            actionLabel = R.string.edit,
        ) {
            snackbarData = null
            onEditOriginAddress(originAddress)
        }
    }

    private fun showPurchasePhoneErrorSnackbar(destinationAddress: DestinationShippingAddress) {
        snackbarData = ShippingLabelsSnackbarData(
            message = R.string.woo_shipping_labels_purchase_phone_error,
            actionLabel = R.string.edit,
        ) {
            snackbarData = null
            onEditDestinationAddress(destinationAddress)
        }
    }

    private fun handlePurchaseFailure(
        exception: Throwable,
        selectedShipmentIndex: Int,
        selectedAddress: WooShippingAddresses
    ) {
        updateShipment(
            selectedShipmentIndex,
            shipments.value[selectedShipmentIndex].copy(isPurchaseAPILoading = false)
        )
        when (exception) {
            is WooException if exception.error.apiErrorCode == UPSDAP_MISSING_TOS_ERROR_CODE -> {
                pendingTermsOfServiceCarrier = CARRIER_UPSDAP
                analyticsTracker.track(
                    AnalyticsEvent.WCS_CARRIER_TOS,
                    mapOf(KEY_STATE to TOS_STATE_SHOWN, KEY_CARRIER to CARRIER_UPSDAP)
                )
                triggerEvent(
                    NavigateToUPSDAPTermsOfService(originAddress = selectedAddress.shipFrom)
                )
            }

            is WooException if exception.error.apiErrorCode == FEDEX_MISSING_TOS_ERROR_CODE -> {
                pendingTermsOfServiceCarrier = CARRIER_FEDEX
                analyticsTracker.track(
                    AnalyticsEvent.WCS_CARRIER_TOS,
                    mapOf(KEY_STATE to TOS_STATE_SHOWN, KEY_CARRIER to CARRIER_FEDEX)
                )
                triggerEvent(NavigateToFedExTermsOfService)
            }

            is WooException if exception.error.message?.contains("phone", ignoreCase = true) == true -> {
                analyticsTracker.track(
                    AnalyticsEvent.WCS_PURCHASE_STEP,
                    mapOf(KEY_STATE to "purchase_failed", KEY_ERROR to "invalid_phone")
                )
                showPurchasePhoneErrorSnackbar(selectedAddress.shipTo)
            }

            else -> {
                analyticsTracker.track(
                    AnalyticsEvent.WCS_PURCHASE_STEP,
                    mapOf(KEY_STATE to "purchase_failed", KEY_ERROR to exception.message)
                )
                snackbarData = ShippingLabelsSnackbarData(
                    message = R.string.woo_shipping_labels_purchase_error,
                    actionLabel = R.string.retry,
                ) {
                    snackbarData = null
                    onPurchaseShippingLabel()
                }
            }
        }
    }

    fun onSelectedRateSortOrderChanged(option: ShippingSortOption) {
        selectedRatesSortOrdersFlow.value = selectedRatesSortOrdersFlow.value.toMutableList().apply {
            set(selectedShipmentIndex, option)
        }
    }

    fun onSplitShipmentButtonTapped() {
        viewModelScope.launch {
            val currentStoreOptions = accountSettings.first()?.storeOptions
            val currentShipmentItems = shipmentItems.value
            if (currentStoreOptions != null && currentShipmentItems.isNotEmpty()) {
                triggerEvent(
                    NavigateToSplitShipment(
                        SplitShipmentArgs(
                            orderId = navArgs.orderId,
                            storeOptions = currentStoreOptions,
                            shipments = shipments.value
                        )
                    )
                )
            }
        }
    }

    private fun sortShippingRates(
        option: ShippingSortOption,
        shippingRates: Map<CarrierUI, List<ShippingRateUI>>
    ): Map<CarrierUI, List<ShippingRateUI>> {
        val comparator = when (option) {
            ShippingSortOption.CHEAPEST -> {
                cheapestComparator
            }

            ShippingSortOption.FASTEST -> {
                fastestComparator
            }
        }
        return shippingRates.mapValues { it.value.sortedWith(comparator) }
    }

    fun onPackageSelected(packageData: PackageData) {
        val index = selectedShipmentIndex
        selectedPackagesFlow.value = selectedPackagesFlow.value.toMutableList().apply {
            set(index, packageData)
        }

        resetWeight(selectedShipmentIndex)
    }

    private fun resetWeight(shipmentIndex: Int) {
        val selectedWeightInput = weightInputsFlow.value[shipmentIndex]
        if (selectedWeightInput.weight.isNotEmpty() && !selectedWeightInput.autoFilled) return

        val itemsWeight = shipmentItems.value[shipmentIndex].sumByFloat { it.weight * it.quantity }
        val packageWeight = selectedPackagesFlow.value[shipmentIndex]?.weight?.toFloatOrNull() ?: 0f
        val totalWeight = itemsWeight + packageWeight

        weightInputsFlow.value = weightInputsFlow.value.toMutableList().apply {
            set(shipmentIndex, WeightInput(weight = totalWeight.toString(), autoFilled = true))
        }
    }

    fun onCustomsDataAvailable(customsData: CustomsData) {
        customsFormDataFlow.value = customsFormDataFlow.value.toMutableList().apply {
            set(selectedShipmentIndex, customsData)
        }
    }

    fun onWeightChange(input: String) {
        weightInputsFlow.value = weightInputsFlow.value.toMutableList().apply {
            set(selectedShipmentIndex, WeightInput(weight = input, autoFilled = false))
        }
    }

    fun onEditCustomsClick() {
        val selectedAddress = shippingAddresses.value.getOrNull(selectedShipmentIndex)
        val originCountryCode = selectedAddress?.shipFrom?.country.orEmpty()
        val destinationCountryCode = selectedAddress?.shipTo?.address?.country?.code.orEmpty()

        launch {
            val event = NavigateToCustomsFormEdit(
                originCountryCode = originCountryCode,
                destinationCountryCode = destinationCountryCode,
                customData = requireNotNull(customsFormDataFlow.value[selectedShipmentIndex]),
                storeOptions = accountSettings.first()?.storeOptions ?: StoreOptionsModel.EMPTY
            )
            triggerEvent(event)
        }
    }

    fun onHazmatNoticeClick() {
        val selectedCategory = hazmatStatesFlow.value[selectedShipmentIndex]
            .run { this as? Declared }
            ?.hazmatCategory

        // Disables the current Snackbar before navigation
        // to avoid presentation conflict with the Hazmat selection result
        snackbarData = null
        triggerEvent(NavigateToHazmatFormEdit(selectedCategory))
    }

    fun onHazmatCategorySelected(selectedCategory: ShippingLabelHazmatCategory?) {
        val previousStates = hazmatStatesFlow.value
        val newState = when (selectedCategory) {
            null -> NoSelection
            else -> Declared(selectedCategory)
        }
        if (newState == previousStates[selectedShipmentIndex]) return

        hazmatStatesFlow.value = previousStates.toMutableList().apply {
            this[selectedShipmentIndex] = newState
        }.toList()

        val snackbarMessage = if (selectedCategory != null) {
            R.string.woo_shipping_labels_hazmat_selection_set
        } else {
            R.string.woo_shipping_labels_hazmat_selection_removed
        }

        snackbarData = ShippingLabelsSnackbarData(
            message = snackbarMessage,
            actionLabel = R.string.undo,
            hasIcon = true,
            dismissAction = { snackbarData = null }
        ) {
            hazmatStatesFlow.value = previousStates
        }
    }

    fun onLabelPaperSizeOptionSelected(paperSize: WooShippingLabelPaperSize) {
        uiState.update { it.copy(paperSizeOption = paperSize) }
    }

    fun onPrintShippingLabelClicked() {
        val fallbackViewState = viewState.value
        viewState.value = WooShippingViewState.Loading(R.string.shipping_label_print_screen_title)
        launch {
            val labelId = shipments.value[selectedShipmentIndex].label?.labelId ?: return@launch
            val paperSize = uiState.value.paperSizeOption
            val labelFile = fetchShippingLabelFile(
                labelIds = listOf(labelId),
                paperSize = paperSize.name.lowercase(Locale.US)
            )

            labelFile?.let {
                triggerEvent(OpenShippingLabelFile(it))
            } ?: triggerEvent(ShowError(R.string.shipping_label_purchased_print_error))

            viewState.value = fallbackViewState
        }
    }

    fun onTrackShipmentClicked() {
        val carrierId = shipments.value[selectedShipmentIndex].label?.carrierId ?: return
        val trackingNumber = shipments.value[selectedShipmentIndex].label?.tracking ?: return
        ShipmentTrackingUrls.fromCarrier(carrierId, trackingNumber)
            ?.let { triggerEvent(OpenUrl(it)) }
            ?: triggerEvent(ShowError(R.string.shipping_label_purchased_tracking_error))
    }

    fun onSchedulePickUpClicked() {
        val carrierId = shipments.value[selectedShipmentIndex].label?.carrierId ?: return
        Carrier.fromCarrierId(carrierId)?.let {
            triggerEvent(OpenUrl(it.pickupUrl))
        } ?: triggerEvent(ShowError(R.string.shipping_label_purchased_pickup_error))
    }

    fun onRefundClicked() {
        shipments.value[selectedShipmentIndex].label?.labelId?.let { labelId ->
            triggerEvent(NavigateToRefundRequest(navArgs.orderId, labelId))
        }
    }

    fun onPrintCustomsClicked(storageDirectory: File) {
        val commercialInvoiceUrl = shipments.value[selectedShipmentIndex].label?.commercialInvoiceUrl ?: return
        downloadAndPrintInvoice(commercialInvoiceUrl, storageDirectory)
    }

    private fun downloadAndPrintInvoice(invoiceUrl: String, storageDirectory: File) {
        printJob?.cancel()
        printJob = launch {
            val fallbackViewState = viewState.value
            viewState.value = WooShippingViewState.Loading(R.string.shipping_label_print_screen_title)
            try {
                downloadAndPrintInvoiceUseCase(invoiceUrl, storageDirectory)
                    .onSuccess { file ->
                        triggerEvent(PrintCustomsForm(file))
                    }
                    .onFailure {
                        WooLog.e(WooLog.T.SHIPPING_LABELS, it)
                        triggerEvent(Event.ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
                    }
            } finally {
                viewState.value = fallbackViewState
            }
        }
    }

    fun onLearnMoreClicked() {
        triggerEvent(OpenLearnMoreScreen)
    }

    fun onNavigateBack() {
        triggerEvent(Event.Exit)
    }

    fun onRetry() {
        viewState.value = WooShippingViewState.Loading(R.string.shipping_label_create_title)

        // Retry loading data that may have previously resulted in errors.
        launch {
            try {
                joinAll(
                    launch { getOrderInformation() },
                    launch { fetchAccountSettings() },
                    launch { fetchOriginAddresses() }
                )
            } catch (e: CancellationException) {
                WooLog.d(WooLog.T.ORDERS, "CancellationException while retrying: $e")
            } finally {
                loadTrigger.emit(Unit)
            }
        }
    }

    private fun onEditPaymentMethodClicked() {
        triggerEvent(NavigateToPaymentMethodEdit)
    }

    data class NavigatePackageSelection(val storeOptions: StoreOptionsModel) : Event()

    data class NavigateToOriginAddressEdit(val originAddress: OriginShippingAddress) : Event()
    data class NavigateToDestinationAddressEdit(
        val destinationAddress: DestinationShippingAddress,
        val orderId: Long
    ) : Event()

    data class NavigateToSplitShipment(val shipmentArgs: SplitShipmentArgs) : Event()

    @Parcelize
    data class SplitShipmentArgs(
        val orderId: Long,
        val shipments: List<ShipmentUIModel>,
        val storeOptions: StoreOptionsModel
    ) : Parcelable

    data class NavigateToCustomsFormEdit(
        val originCountryCode: String,
        val destinationCountryCode: String,
        val customData: CustomsData,
        val storeOptions: StoreOptionsModel
    ) : Event()

    data class NavigateToHazmatFormEdit(val selectedCategory: ShippingLabelHazmatCategory?) : Event()

    sealed class WooShippingViewState {
        data object Error : WooShippingViewState()
        data class Loading(@StringRes val screenTitle: Int) : WooShippingViewState()
        data class DataState(
            val shipmentUIList: List<ShipmentUI>,
            val totalItems: Int,
            val totalItemsCost: String,
            val weightUnit: String,
            val shippingLines: List<ShippingLineSummaryUI>,
            val shippingAddresses: List<WooShippingAddresses>,
            val uiState: UIControlsState,
            val destinationStatus: AddressStatus,
            val paymentsSectionUI: PaymentsSectionUI,
            val purchaseSectionUI: PurchaseSectionUI,
            val isSplitShipmentsSupported: Boolean
        ) : WooShippingViewState() {
            val shouldShowSplitShipmentButton: Boolean
                get() {
                    val unpurchasedShipments = shipmentUIList.filterNot { it.isPurchased }
                    val hasMultipleUnfulfilledItems = unpurchasedShipments.size > 1 ||
                        (unpurchasedShipments.firstOrNull()?.totalItemQuantity ?: 0) > 1
                    return isSplitShipmentsSupported &&
                        shipmentUIList.none { it.isPurchaseInProgress } &&
                        hasMultipleUnfulfilledItems
                }
        }
    }

    @Parcelize
    sealed class ShippingRatesState : Parcelable {
        data object NoAvailable : ShippingRatesState()
        data class MissingInfo(
            val missingTitle: Int,
            val missingDescription: Int
        ) : ShippingRatesState()

        data class Error(@StringRes val message: Int) : ShippingRatesState()

        data class Loading(
            val selectedRatesSortOrder: ShippingSortOption
        ) : ShippingRatesState()

        data class DataState(
            val selectedRatesSortOrder: ShippingSortOption,
            val shippingRates: Map<CarrierUI, List<ShippingRateUI>>,
            val selectedRate: ShippingRateUI?,
            val onSelectedShippingRateChanged: (ShippingRateUI) -> Unit,
            val onSelectedRateOptionChanged: (WooShippingRateModel.Option, Boolean) -> Unit
        ) : ShippingRatesState()
    }

    @Parcelize
    sealed class PackageSelectionState : Parcelable {
        data object NotSelected : PackageSelectionState()
        data class DataAvailable(
            val selectedPackage: PackageData,
            val shipmentWeight: String,
            val weightUnit: String
        ) : PackageSelectionState()
    }

    data class WeightInput(val weight: String, val autoFilled: Boolean)

    data class UIControlsState(
        val markOrderComplete: Boolean,
        val selectedIndex: Int = 0,
        val noticeBannerUiState: NoticeBannerUiState? = null,
        val paperSizeOption: WooShippingLabelPaperSize,
    )

    data class ShippingRatesInfo(
        val orderId: Long,
        val packageSelected: PackageData,
        val shipFrom: OriginShippingAddress,
        val shipTo: Address?,
        val weight: Float?,
        val currencyCode: String?,
        val customsData: CustomsData?,
        val hazmatSelection: ShippingLabelHazmatCategory?
    )

    @Parcelize
    sealed class CustomsState : Parcelable {
        data object NotRequired : CustomsState()
        data object ItnMissing : CustomsState()
        data object Unavailable : CustomsState()
        data class DataAvailable(val customsData: CustomsData) : CustomsState()
    }

    @Parcelize
    sealed class HazmatState : Parcelable {
        data object NoSelection : HazmatState()
        data class Declared(val hazmatCategory: ShippingLabelHazmatCategory) : HazmatState()

        val hazmatSelection: ShippingLabelHazmatCategory?
            get() = (this as? Declared)?.hazmatCategory
    }

    data class OpenShippingLabelFile(val file: File) : Event()
    data class OpenUrl(val url: String) : Event()
    data class ShowError(val errorResId: Int) : Event()
    data class NavigateToRefundRequest(val orderId: Long, val labelId: Long) : Event()
    data object NavigateToPaymentMethodEdit : Event()
    data class NavigateToUPSDAPTermsOfService(val originAddress: OriginShippingAddress) : Event()
    data object NavigateToFedExTermsOfService : Event()

    object OpenLearnMoreScreen : Event()

    data class PrintCustomsForm(val file: File) : Event()

    enum class Carrier(val pickupUrl: String) {
        USPS("https://tools.usps.com/schedule-pickup-steps.htm"),
        FEDEX("https://www.fedex.com/en-us/shipping/schedule-manage-pickups.html"),
        UPS("https://wwwapps.ups.com/pickup/request"),
        DHL("https://mydhl.express.dhl/us/en/schedule-pickup.html#/schedule-pickup#label-reference");

        companion object {
            fun fromCarrierId(carrierId: String): Carrier? {
                return when (carrierId) {
                    "usps" -> USPS
                    "fedex" -> FEDEX
                    "ups" -> UPS
                    "dhlexpress" -> DHL
                    else -> null
                }
            }
        }
    }

    companion object {
        private const val NOTIFICATIONS_DELAY = 2_000L
        private const val TYPING_DELAY = 800L
        private const val MULTIPLE_CALLS_DELAY = 50L

        @VisibleForTesting
        const val UPSDAP_MISSING_TOS_ERROR_CODE = "missing_upsdap_terms_of_service_acceptance"

        @VisibleForTesting
        const val FEDEX_MISSING_TOS_ERROR_CODE = "missing_fedex_terms_of_service_acceptance"

        // Carrier Terms of Service tracking. Carrier values match the backend IDs (and iOS).
        private const val CARRIER_UPSDAP = "upsdap"
        private const val CARRIER_FEDEX = "fedex"
        private const val TOS_STATE_SHOWN = "shown"
        private const val TOS_STATE_ACCEPTED = "accepted"
    }
}

@Parcelize
data class WooShippingAddresses(
    val shipFrom: OriginShippingAddress,
    val shipTo: DestinationShippingAddress,
    val originAddresses: List<OriginShippingAddress>
) : Parcelable {
    companion object {
        val EMPTY = WooShippingAddresses(
            shipFrom = OriginShippingAddress.EMPTY,
            shipTo = DestinationShippingAddress.EMPTY,
            originAddresses = emptyList()
        )
    }
}

@Parcelize
data class ShippableItemUI(
    val itemId: Long,
    val productId: Long,
    val title: String,
    val formattedSize: String,
    val formattedWeight: String,
    val formattedPrice: String,
    val quantity: Float,
    val imageUrl: String? = null
) : Parcelable

@Parcelize
data class ShipmentUI(
    val shippableItems: List<ShippableItemUI>,
    val formattedTotalWeight: String,
    val formattedTotalPrice: String,
    val packageSelectionState: PackageSelectionState,
    val customsState: CustomsState,
    val hazmatState: HazmatState,
    val shippingRatesState: ShippingRatesState,
    val shipmentCostUI: ShipmentCostUI?,
    val isPurchaseAPILoading: Boolean = false,
    val labelPurchaseStatus: LabelPurchaseStatus = LabelPurchaseStatus.Idle
) : Parcelable {
    val isPurchased
        get() = labelPurchaseStatus is LabelPurchaseStatus.Purchased

    val isPurchaseInProgress
        get() = labelPurchaseStatus is LabelPurchaseStatus.PurchaseInProgress

    val isReadOnly
        get() = isPurchaseInProgress || isPurchased

    val totalItemQuantity
        get() = shippableItems.sumByFloat { it.quantity }.toInt()
}

@Parcelize
data class ShippingLineSummaryUI(
    val title: String,
    val amount: String
) : Parcelable

@Parcelize
data class ShipmentCostUI(
    val serviceName: String,
    val formattedBasePrice: String,
    val formattedTotalPrice: String,
    val optionsWithFees: Map<String, String>,
) : Parcelable

sealed interface LabelPurchaseStatus : Parcelable {
    @Parcelize
    data object Idle : LabelPurchaseStatus

    @Parcelize
    data object PurchaseInProgress : LabelPurchaseStatus

    @Parcelize
    data class Purchased(
        val availablePrintSizes: List<WooShippingLabelPaperSize>,
        val isRefundAvailable: Boolean = false,
        val isCustomsFormAvailable: Boolean = false
    ) : LabelPurchaseStatus

    @Parcelize
    data class Failed(val errorMessage: String?) : LabelPurchaseStatus
}

data class PurchaseSectionUI(
    val isVisible: Boolean,
    val isOrderAlreadyCompleted: Boolean,
    val markOrderComplete: Boolean,
    val formattedPrice: String?,
    val onMarkOrderCompleteChange: (Boolean) -> Unit,
    val onPurchaseShippingLabel: () -> Unit,
)

data class PaymentsSectionUI(
    val selectedPaymentMethod: PaymentMethodModel?,
    val onEditPaymentMethodClicked: () -> Unit
)
