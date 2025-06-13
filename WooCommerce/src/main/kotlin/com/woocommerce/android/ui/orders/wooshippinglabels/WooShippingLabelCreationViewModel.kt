package com.woocommerce.android.ui.orders.wooshippinglabels

import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.shippinglabels.ShipmentTrackingUrls
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelHazmatCategory
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.ItnMissing
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.NotRequired
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.Unavailable
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
import com.woocommerce.android.ui.orders.wooshippinglabels.components.WooShippingLabelPaperSize
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireCustomsForm
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireITN
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchaseState
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PurchasedLabelData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.ObserveShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.domain.GetShippingRates
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.CarrierUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingRateUI
import com.woocommerce.android.ui.orders.wooshippinglabels.rates.ui.ShippingSortOption
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.FlowPreview
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.File
import java.math.BigDecimal
import java.util.Date
import java.util.Locale
import javax.inject.Inject

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
    private val observeAccountSettings: ObserveAccountSettings,
    private val fetchAccountSettings: FetchAccountSettings,
    private val addressValidationHelper: AddressValidationHelper,
    private val verifyDestinationAddress: VerifyDestinationAddress,
    private val observeShippingLabelNotice: ObserveShippingLabelNotice,
    private val shouldRequireCustoms: ShouldRequireCustomsForm,
    private val shouldRequireITN: ShouldRequireITN,
    private val fetchShippingLabelFile: FetchShippingLabelFile,
    private val observeShippingLabelStatus: ObserveShippingLabelStatus
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingLabelCreationFragmentArgs by savedState.navArgs()

    var snackbarData by mutableStateOf<ShippingLabelsSnackbarData?>(null)

    private val emptyOrder = Order.getEmptyOrder(Date(), Date())
    private val order = MutableStateFlow<Order>(emptyOrder)
    private val destinationAddress = MutableStateFlow<DestinationShippingAddress>(DestinationShippingAddress.EMPTY)
    private val shippingAddresses = MutableStateFlow<WooShippingAddresses?>(WooShippingAddresses.EMPTY)
    private val loadTrigger = MutableSharedFlow<Unit>()

    private val shipments = MutableStateFlow<List<ShipmentUIModel>>(emptyList())
    private val shipmentItems = MutableStateFlow<List<List<ShippableItemModel>>>(emptyList())

    private val selectedPackagesFlow = MutableStateFlow<List<PackageData?>>(emptyList())
    private val customsFormDataFlow = MutableStateFlow<List<CustomsData?>>(emptyList())
    private val packageWeightsFlow = MutableStateFlow<List<PackageWeight?>>(emptyList())
    private val packageSelectionsFlow = MutableStateFlow<List<PackageSelectionState>>(emptyList())
    private val customsStatesFlow = MutableStateFlow<List<CustomsState>>(emptyList())
    private val hazmatStatesFlow = MutableStateFlow<List<HazmatState>>(emptyList())

    private val accountSettings = observeAccountSettings()
        .shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val uiState = MutableStateFlow(
        UIControlsState(
            markOrderComplete = false,
            selectedIndex = 0,
            isShipmentDetailsExpanded = false,
            paperSizeOption = WooShippingLabelPaperSize.LABEL
        )
    )

    private val selectedRatesSortOrdersFlow = MutableStateFlow<List<ShippingSortOption>>(emptyList())
    private val refreshShippingRates = MutableSharedFlow<Unit>()
    var customWeight by mutableStateOf(emptyList<String>())
        private set

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
        launch { getDestinationAddress() }
        launch { getSavedShipments() }
        launch { getShippingAddresses() }
        launch { getOrderInformation() }
        launch { observePackageWeight() }
        launch { observePackageChanges() }
        launch { observeShippingRates() }
        launch { observeShippingRatesState() }
        launch { observeCustomsDataChanges() }
        launch { observeNotices() }
    }

    private suspend fun getOrderInformation() {
        orderDetailRepository.getOrderById(navArgs.orderId)?.let {
            order.value = it
        } ?: run {
            triggerEvent(Event.ShowSnackbar(R.string.woo_shipping_labels_loading_order_error))
            postTriggerEvent(Event.Exit)
        }
    }

    private suspend fun observeNotices() = observeShippingLabelNotice(
        shippingAddresses,
        customsStatesFlow.filter { it.isNotEmpty() },
        uiState.map { it.selectedIndex }.distinctUntilChanged(),
        viewModelScope
    ).onStart { delay(NOTIFICATIONS_DELAY) }
        .collectLatest { noticeBanner ->
            uiState.update {
                it.copy(
                    noticeBannerUiState = noticeBanner?.copy(
                        onTapped = {
                            when (noticeBanner.type) {
                                NoticeType.UNVERIFIED_ORIGIN_ADDRESS -> {
                                    shippingAddresses.value?.shipFrom?.let { shipFrom -> onEditOriginAddress(shipFrom) }
                                }

                                NoticeType.MISSING_DESTINATION_ADDRESS, NoticeType.UNVERIFIED_DESTINATION_ADDRESS -> {
                                    shippingAddresses.value?.shipTo?.let { shipTo -> onEditDestinationAddress(shipTo) }
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

    private fun observeShippingLabelPurchaseStatus(shipmentId: Int) {
        launch {
            val labelId = shipments.value[shipmentId].labelId ?: return@launch
            observeShippingLabelStatus(orderId = navArgs.orderId, labelId = labelId).onEach { result ->
                updateShipment(
                    shipmentId,
                    shipments.value[shipmentId].copy(
                        status = result.status,
                        refundableAmount = result.refundableAmount ?: BigDecimal.ZERO
                    )
                )
            }.launchIn(this)
        }
    }

    private suspend fun getDestinationAddress() {
        order.drop(1).collectLatest { order ->
            val defaultDestination = DestinationShippingAddress(
                address = order.shippingAddress.copy(email = order.billingAddress.email),
                isVerified = false
            )

            destinationAddress.value = defaultDestination

            if (addressValidationHelper.isMissingDestinationAddress(order.shippingAddress).not()) {
                verifyDestinationAddress(order.id).fold(
                    onSuccess = { destinationAddress.value = it },
                    onFailure = { }
                )
            }
        }
    }

    private suspend fun getSavedShipments() {
        order.drop(1).collectLatest { order -> shipments.value = getShipments(order) }
    }

    @Suppress("ComplexCondition")
    @OptIn(FlowPreview::class)
    private suspend fun observeShippingRates() {
        combine(
            selectedPackagesFlow.filter { it.isNotEmpty() },
            shippingAddresses,
            packageWeightsFlow.filter { it.isNotEmpty() },
            customsStatesFlow.filter { it.isNotEmpty() },
            hazmatStatesFlow.filter { it.isNotEmpty() },
            refreshShippingRates.onStart { emit(Unit) },
        ) { selectedPackages, addresses, packageWeight, customState, hazmatStates, _ ->
            val customsFulfilled = customState[selectedShipmentIndex] is CustomsState.DataAvailable ||
                customState[selectedShipmentIndex] is NotRequired
            val selectedPackage = selectedPackages[selectedShipmentIndex]
            if (selectedPackage != null && addresses != null && customsFulfilled) {
                ShippingRatesInfo(
                    orderId = navArgs.orderId,
                    packageSelected = selectedPackage,
                    shipFrom = addresses.shipFrom,
                    shipTo = addresses.shipTo.address,
                    weight = packageWeight[selectedShipmentIndex]?.totalWeight,
                    currencyCode = order.value.currency,
                    customsData = customsFormDataFlow.value[selectedShipmentIndex],
                    hazmatSelection = hazmatStates[selectedShipmentIndex].hazmatSelection
                )
            } else {
                null
            }
        }.debounce(MULTIPLE_CALLS_DELAY)
            .collectLatest { updateShippingRates(selectedShipmentIndex, it) }
    }

    private suspend fun observeShippingRatesState() {
        combine(
            shippingRatesListFlow.filter { it.isNotEmpty() },
            selectedRatesFlow.filter { it.isNotEmpty() },
            selectedRatesSortOrdersFlow.filter { it.isNotEmpty() },
        ) { shippingRates, selectedRates, selectedRatesSortOrders ->
            shippingRates.mapIndexed { index, map ->
                if (map.isEmpty()) {
                    ShippingRatesState.NoAvailable
                } else {
                    ShippingRatesState.DataState(
                        selectedRatesSortOrders[index],
                        sortShippingRates(selectedRatesSortOrders[index], map),
                        selectedRates[index]
                    )
                }
            }
        }.collectLatest {
            shippingRatesStatesFlow.value = it
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observePackageWeight() {
        combine(
            shipmentItems.filter { it.isNotEmpty() && it.size > selectedShipmentIndex },
            selectedPackagesFlow.filter { it.isNotEmpty() && it.size == shipments.value.size },
            snapshotFlow { customWeight }
                .filter { it.isNotEmpty() && it.size == shipments.value.size }
                .debounce(TYPING_DELAY)
        ) { shipmentItems, selectedPackage, customWeightString ->
            if (selectedPackage.size == shipments.value.size && customWeightString.size == shipments.value.size) {
                shipmentItems.mapIndexed { index, shipmentItemModelList ->
                    val itemsWeight = shipmentItemModelList.sumByFloat { it.weight }
                    val packageWeight = selectedPackage[index]?.weight?.toFloatOrNull()
                    PackageWeight(
                        itemsWeight = itemsWeight,
                        packageWeight = packageWeight,
                        customWeight = customWeightString[index].toFloatOrNull()
                    )
                }
            } else {
                null
            }
        }.filterNotNull()
            .collectLatest { packageWeightsFlow.value = it }
    }

    private suspend fun observePackageChanges() {
        combine(
            selectedPackagesFlow.filter { it.isNotEmpty() },
            packageWeightsFlow.filter { it.isNotEmpty() },
            accountSettings.map { it?.storeOptions },
            packageSelectionsFlow.filter { it.isNotEmpty() }
        ) { packagesSelected, packageWeight, storeOptions, _ ->
            packagesSelected.mapIndexed { index, selectedPackageData ->
                val selectedPackageWeight = packageWeight[index]
                if (selectedPackageData == null || selectedPackageWeight == null) {
                    NotSelected
                } else {
                    DataAvailable(
                        selectedPackage = selectedPackageData,
                        defaultWeight = selectedPackageWeight.defaultWeight.toString(),
                        weightUnit = storeOptions?.weightUnit ?: ""
                    )
                }
            }
        }.collectLatest { packageSelectionsFlow.value = it }
    }

    // This logic will be updated later once the Customs data state is available
    private suspend fun observeCustomsDataChanges() {
        combine(
            shippingAddresses,
            customsFormDataFlow.filter { it.isNotEmpty() },
            shipmentItems.filter { it.isNotEmpty() },
        ) { addresses, customsData, shipmentItems ->
            val customsRequired = addresses != null && shouldRequireCustoms(addresses)

            shipmentItems.mapIndexed { index, shippableItemModelList ->
                val currentItemCustomsData = customsData[index]
                val itnMissing = currentItemCustomsData?.itn.isNullOrEmpty() && shippableItemModelList.isItnRequired()

                when {
                    customsRequired && itnMissing -> ItnMissing
                    currentItemCustomsData != null -> CustomsState.DataAvailable(currentItemCustomsData)
                    customsRequired -> Unavailable
                    else -> NotRequired
                }
            }
        }.collectLatest { customsStatesFlow.value = it }
    }

    private suspend fun getShippingAddresses() {
        combine(destinationAddress, observeOriginAddresses()) { destination, originAddresses ->
            if (!originAddresses.isNullOrEmpty()) {
                val selectedOriginAddress = getSelectedOriginAddress(originAddresses)
                WooShippingAddresses(
                    shipFrom = selectedOriginAddress,
                    originAddresses = originAddresses,
                    shipTo = destination
                )
            } else {
                null
            }
        }.collect { shippingAddresses.value = it }
    }

    /**
     * Updates shippingRatesInfo for the selected shipment
     */
    @Suppress("CyclomaticComplexMethod")
    private suspend fun updateShippingRates(index: Int, shippingRatesInfo: ShippingRatesInfo?) {
        val currentMutableShippingRatesList = shippingRatesStatesFlow.value.toMutableList()
        when {
            shippingRatesInfo == null -> {
                shippingRatesStatesFlow.value = currentMutableShippingRatesList.apply {
                    set(index, ShippingRatesState.NoAvailable)
                }
            }

            shippingRatesInfo.shipTo == null ||
                !addressValidationHelper.canFetchShippingRates(shippingRatesInfo.shipTo) ->
                shippingRatesStatesFlow.value = currentMutableShippingRatesList.apply {
                    set(
                        index,
                        ShippingRatesState.MissingInfo(
                            missingTitle = R.string.woo_shipping_labels_shipping_rates_missing_destination,
                            missingDescription = R.string.woo_shipping_labels_shipping_rates_missing_destination_desc
                        )
                    )
                }

            shippingRatesInfo.weight == null || shippingRatesInfo.weight == 0f ->
                shippingRatesStatesFlow.value = currentMutableShippingRatesList.apply {
                    set(
                        index,
                        ShippingRatesState.MissingInfo(
                            missingTitle = R.string.woo_shipping_labels_shipping_rates_missing_weight,
                            missingDescription = R.string.woo_shipping_labels_shipping_rates_missing_weight_desc
                        )
                    )
                }

            else -> {
                val sortOrder = selectedRatesSortOrdersFlow.value[index]
                shippingRatesStatesFlow.value = currentMutableShippingRatesList.apply {
                    set(index, ShippingRatesState.Loading(sortOrder))
                }

                val shippingRatesResult = getShippingRates(
                    shippingRatesInfo.orderId,
                    shippingRatesInfo.packageSelected,
                    shippingRatesInfo.shipTo,
                    shippingRatesInfo.shipFrom,
                    shippingRatesInfo.weight,
                    shippingRatesInfo.currencyCode,
                    shippingRatesInfo.customsData,
                    shippingRatesInfo.hazmatSelection
                )

                if (shippingRatesResult.isSuccess && shippingRatesResult.getOrThrow().isNotEmpty()) {
                    shippingRatesListFlow.value = shippingRatesListFlow.value.toMutableList().apply {
                        set(index, shippingRatesResult.getOrThrow())
                    }
                } else {
                    shippingRatesStatesFlow.value = currentMutableShippingRatesList.apply {
                        set(index, ShippingRatesState.Error)
                    }
                }
                selectedRatesFlow.value = selectedRatesFlow.value.toMutableList().apply { set(index, null) }
            }
        }
    }

    @Suppress("ComplexCondition")
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
            if (accountSettings == null || addresses == null ||
                shipments.any { it.purchaseState is PurchaseState.Error }
            ) {
                return@combine WooShippingViewState.Error
            }

            val destinationStatus = when {
                addressValidationHelper.isMissingDestinationAddress(addresses.shipTo.address) -> {
                    AddressStatus.MISSING_ADDRESS
                }

                addresses.shipTo.isVerified -> AddressStatus.VERIFIED
                else -> AddressStatus.UNVERIFIED
            }

            shipmentItems.value = shipments.map { it.items }
            adjustFlowSizesToShipmentCount(shipments.size)

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
                shippingLines = shippingLineSummary,
                shippingAddresses = addresses,
                uiState = uiState,
                destinationStatus = destinationStatus,
                paymentsSectionUI = PaymentsSectionUI(accountSettings.paymentMethodOptions.selectedPaymentMethod)
            )
        }.combine(loadTrigger.onStart { emit(Unit) }) { viewState, _ ->
            viewState
        }.collectLatest {
            viewState.value = it
        }
    }

    private fun adjustFlowSizesToShipmentCount(shipmentSize: Int) {
        fun <T> MutableStateFlow<List<T>>.updateSize(defaultValue: T) = this.update { currentList ->
            if (currentList.size <= shipmentSize) {
                currentList + List(shipmentSize - currentList.size) { defaultValue }
            } else {
                List(shipmentSize) { defaultValue }
            }
        }

        selectedPackagesFlow.updateSize(null)
        customsFormDataFlow.updateSize(null)
        packageWeightsFlow.updateSize(null)
        packageSelectionsFlow.updateSize(NotSelected)
        customsStatesFlow.updateSize(NotRequired)
        hazmatStatesFlow.updateSize(NoSelection)
        selectedRatesSortOrdersFlow.updateSize(ShippingSortOption.FASTEST)
        selectedRatesFlow.updateSize(null)
        shippingRatesListFlow.updateSize(emptyMap())
        shippingRatesStatesFlow.updateSize(ShippingRatesState.NoAvailable)

        customWeight = if (customWeight.size <= shipmentSize) {
            customWeight + List(shipmentSize - customWeight.size) { "" }
        } else {
            List(shipmentSize) { "" }
        }
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
    }

    private fun getSelectedOriginAddress(originAddresses: List<OriginShippingAddress>): OriginShippingAddress {
        return shippingAddresses.value?.shipFrom?.takeIf {
            it != OriginShippingAddress.EMPTY
        } ?: originAddresses.first()
    }

    fun onSelectedShipmentChanged(index: Int) {
        if (index >= shipments.value.size) return // This can happen after shipment split when the UI is not updated yet

        uiState.value = uiState.value.copy(selectedIndex = index)
    }

    fun onOriginAddressSelected(address: OriginShippingAddress) {
        shippingAddresses.value?.let {
            shippingAddresses.value = it.copy(shipFrom = address)
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

    fun onShipmentDetailsExpandedChange(value: Boolean) {
        uiState.update { it.copy(isShipmentDetailsExpanded = value) }
    }

    fun onSelectPackageClicked() {
        triggerEvent(NavigatePackageSelection)
    }

    @Suppress("ComplexCondition")
    fun onPurchaseShippingLabel() {
        val selectedShipmentIndex = selectedShipmentIndex
        val selectedPackage = selectedPackagesFlow.value[selectedShipmentIndex]
        val addresses = shippingAddresses.value
        val shippingRate = selectedRatesFlow.value[selectedShipmentIndex]?.selectedOption?.rate
        val weight = packageWeightsFlow.value[selectedShipmentIndex]?.totalWeight

        if (selectedPackage == null || addresses == null || shippingRate == null || weight == null) return

        val orderId = navArgs.orderId
        val lastOrderComplete = uiState.value.markOrderComplete
        val shippableItemsIdList = shipmentItems.value[selectedShipmentIndex].map { it.productId }
        val hazmatSelection = hazmatStatesFlow.value[selectedShipmentIndex].hazmatSelection

        val fallbackPurchaseState = shipments.value[selectedShipmentIndex].purchaseState
        updateShipment(
            selectedShipmentIndex,
            shipments.value[selectedShipmentIndex].copy(purchaseState = PurchaseState.InProgress)
        )

        val customsData = customsFormDataFlow.value[selectedShipmentIndex]?.let { listOf(it) }

        launch {
            val result = purchaseShippingLabel(
                orderId,
                shippableItemsIdList,
                selectedPackage,
                addresses.shipTo.address,
                addresses.shipFrom,
                shippingRate,
                weight,
                lastOrderComplete,
                customsData,
                hazmatSelection
            )

            if (result.isSuccess) {
                handlePurchaseSuccess(result, selectedShipmentIndex)
            } else {
                updateShipment(
                    selectedShipmentIndex,
                    shipments.value[selectedShipmentIndex].copy(purchaseState = fallbackPurchaseState)
                )
                snackbarData = ShippingLabelsSnackbarData(
                    message = R.string.woo_shipping_labels_purchase_error,
                    actionLabel = R.string.retry,
                ) { onPurchaseShippingLabel() }
            }
        }
    }

    private fun handlePurchaseSuccess(result: Result<PurchasedLabelData>, shipmentId: Int) {
        updateShipment(shipmentId, shipments.value[shipmentId].copy(purchaseState = PurchaseState.Success))
        result.getOrNull()
            ?.labels
            ?.firstOrNull()
            ?.let { purchasedLabel ->
                updateShipment(
                    shipmentId,
                    shipments.value[shipmentId].copy(
                        purchased = true,
                        labelId = purchasedLabel.labelId,
                        carrierId = purchasedLabel.carrierId,
                        trackingNumber = purchasedLabel.tracking,
                        refundableAmount = purchasedLabel.refundableAmount,
                        purchaseDate = purchasedLabel.created
                    )
                )
                observeShippingLabelPurchaseStatus(shipmentId)
            }
    }

    fun onSelectedRateSortOrderChanged(option: ShippingSortOption) {
        selectedRatesSortOrdersFlow.value = selectedRatesSortOrdersFlow.value.toMutableList().apply {
            set(selectedShipmentIndex, option)
        }
    }

    fun onSelectedSippingRateChanged(rate: ShippingRateUI) {
        selectedRatesFlow.update {
            selectedRatesFlow.value.toMutableList().apply { set(selectedShipmentIndex, rate) }
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
        selectedPackagesFlow.value = selectedPackagesFlow.value.toMutableList().apply {
            set(selectedShipmentIndex, packageData)
        }
    }

    fun onCustomsDataAvailable(customsData: CustomsData) {
        customsFormDataFlow.value = customsFormDataFlow.value.toMutableList().apply {
            set(selectedShipmentIndex, customsData)
        }
    }

    fun onCustomWeightChange(input: String) {
        customWeight = customWeight.toMutableList().apply { set(selectedShipmentIndex, input) }
    }

    fun onEditCustomsClick() {
        val destinationCountryCode = shippingAddresses.value
            ?.shipTo?.address?.country?.code.orEmpty()

        val event = NavigateToCustomsFormEdit(
            shippableItems = shipmentItems.value[selectedShipmentIndex],
            destinationCountryCode = destinationCountryCode,
            customData = customsFormDataFlow.value[selectedShipmentIndex]
        )
        triggerEvent(event)
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
            val labelId = shipments.value[selectedShipmentIndex].labelId ?: return@launch
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
        val carrierId = shipments.value[selectedShipmentIndex].carrierId ?: return
        val trackingNumber = shipments.value[selectedShipmentIndex].trackingNumber ?: return
        ShipmentTrackingUrls.fromCarrier(carrierId, trackingNumber)
            ?.let { triggerEvent(OpenUrl(it)) }
            ?: triggerEvent(ShowError(R.string.shipping_label_purchased_tracking_error))
    }

    fun onSchedulePickUpClicked() {
        val carrierId = shipments.value[selectedShipmentIndex].carrierId ?: return
        Carrier.fromCarrierId(carrierId)?.let {
            triggerEvent(OpenUrl(it.pickupUrl))
        } ?: triggerEvent(ShowError(R.string.shipping_label_purchased_pickup_error))
    }

    fun onRefundClicked() {
        val selectedShipment = shipments.value[selectedShipmentIndex]
        triggerEvent(NavigateToRefundRequest(navArgs.orderId, selectedShipment))
    }

    fun onLearnMoreClicked() {
        triggerEvent(OpenLearnMoreScreen)
    }

    fun onEditPaymentMethodClicked() {
        println("TODO: Implement payment method editing")
    }

    fun allowBackNavigation(): Boolean {
        val state = uiState.value
        return when {
            state.isShipmentDetailsExpanded -> {
                uiState.update { it.copy(isShipmentDetailsExpanded = false) }
                false
            }

            else -> true
        }
    }

    fun onNavigateBack() {
        if (allowBackNavigation()) triggerEvent(Event.Exit)
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

    private fun List<ShippableItemModel>.isItnRequired(): Boolean {
        val totalShippingValue = map { it.shippingTotalValue }
            .takeIf { it.isNotEmpty() }
            ?.reduce { acc, current -> acc + current }
            ?: BigDecimal.ZERO

        val destinationCountryCode = shippingAddresses.value
            ?.shipTo?.address?.country?.code.orEmpty()

        return shouldRequireITN(destinationCountryCode, totalShippingValue)
    }

    data object NavigatePackageSelection : Event()

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
        val shippableItems: List<ShippableItemModel>,
        val destinationCountryCode: String,
        val customData: CustomsData?
    ) : Event()

    data class NavigateToHazmatFormEdit(val selectedCategory: ShippingLabelHazmatCategory?) : Event()

    sealed class WooShippingViewState {
        data object Error : WooShippingViewState()
        data class Loading(@StringRes val screenTitle: Int) : WooShippingViewState()
        data class DataState(
            val shipmentUIList: List<ShipmentUI>,
            val totalItems: Int,
            val totalItemsCost: String,
            val shippingLines: List<ShippingLineSummaryUI>,
            val shippingAddresses: WooShippingAddresses,
            val uiState: UIControlsState,
            val destinationStatus: AddressStatus,
            val paymentsSectionUI: PaymentsSectionUI
        ) : WooShippingViewState() {
            val shouldShowSplitShipmentButton: Boolean
                get() {
                    val unpurchasedShipments = shipmentUIList.filterNot { it.purchased }
                    return unpurchasedShipments.size > 1 ||
                        (unpurchasedShipments.firstOrNull()?.totalItemQuantity ?: 0) > 1
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

        data object Error : ShippingRatesState()

        data class Loading(
            val selectedRatesSortOrder: ShippingSortOption
        ) : ShippingRatesState()

        data class DataState(
            val selectedRatesSortOrder: ShippingSortOption,
            val shippingRates: Map<CarrierUI, List<ShippingRateUI>>,
            val selectedRate: ShippingRateUI? = null
        ) : ShippingRatesState()
    }

    @Parcelize
    sealed class PackageSelectionState : Parcelable {
        data object NotSelected : PackageSelectionState()
        data class DataAvailable(
            val selectedPackage: PackageData,
            val defaultWeight: String,
            val weightUnit: String
        ) : PackageSelectionState()
    }

    data class PackageWeight(
        val itemsWeight: Float,
        val packageWeight: Float? = null,
        val customWeight: Float? = null
    ) {
        val defaultWeight: Float
            get() = itemsWeight + (packageWeight ?: 0f)
        val totalWeight: Float
            get() = customWeight ?: defaultWeight
    }

    data class UIControlsState(
        val markOrderComplete: Boolean,
        val selectedIndex: Int = 0,
        val isShipmentDetailsExpanded: Boolean,
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
    data class NavigateToRefundRequest(val orderId: Long, val shipment: ShipmentUIModel) : Event()

    object OpenLearnMoreScreen : Event()

    enum class Carrier(val pickupUrl: String) {
        USPS("https://tools.usps.com/schedule-pickup-steps.htm"),
        UPS("https://wwwapps.ups.com/pickup/request"),
        DHL("https://mydhl.express.dhl/us/en/schedule-pickup.html#/schedule-pickup#label-reference");

        companion object {
            fun fromCarrierId(carrierId: String): Carrier? {
                return when (carrierId) {
                    "usps" -> USPS
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
    val purchased: Boolean,
    val packageSelectionState: PackageSelectionState,
    val customsState: CustomsState,
    val hazmatState: HazmatState,
    val shippingRatesState: ShippingRatesState,
    val purchaseState: PurchaseState = PurchaseState.NoStarted,
    val status: ShippingLabelStatus = ShippingLabelStatus.UNKNOWN,
) : Parcelable {
    val totalItemQuantity
        get() = shippableItems.sumByFloat { it.quantity }.toInt()
}

@Parcelize
data class ShippingLineSummaryUI(
    val title: String,
    val amount: String
) : Parcelable

@Parcelize
data class ShippingRateSummaryUI(
    val serviceName: String,
    val total: String,
    val optionName: String? = null,
    val optionFee: String? = null
) : Parcelable

data class PaymentsSectionUI(
    val selectedPaymentMethod: PaymentMethodModel?
)
