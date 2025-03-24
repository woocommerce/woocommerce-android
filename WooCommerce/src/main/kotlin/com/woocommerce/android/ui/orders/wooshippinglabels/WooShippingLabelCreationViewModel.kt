package com.woocommerce.android.ui.orders.wooshippinglabels

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.extensions.combine
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.ItnMissing
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.NotRequired
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.CustomsState.Unavailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.DataAvailable
import com.woocommerce.android.ui.orders.wooshippinglabels.WooShippingLabelCreationViewModel.PackageSelectionState.NotSelected
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.address.AddressValidationHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.address.ObserveShippingLabelNotice
import com.woocommerce.android.ui.orders.wooshippinglabels.address.destination.VerifyDestinationAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.FetchOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.address.origin.ObserveOriginAddresses
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeBannerUiState
import com.woocommerce.android.ui.orders.wooshippinglabels.components.NoticeType
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.CustomsData
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireCustomsForm
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ShouldRequireITN
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import com.woocommerce.android.ui.orders.wooshippinglabels.packages.ui.PackageData
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.PurchasedShippingLabelData
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WooShippingLabelCreationViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val orderDetailRepository: OrderDetailRepository,
    private val getShippableItems: GetShippableItems,
    private val currencyFormatter: CurrencyFormatter,
    private val observeOriginAddresses: ObserveOriginAddresses,
    private val fetchOriginAddresses: FetchOriginAddresses,
    private val getShippingRates: GetShippingRates,
    private val purchaseShippingLabel: PurchaseShippingLabel,
    private val observeStoreOptions: ObserveStoreOptions,
    private val fetchAccountSettings: FetchAccountSettings,
    private val shouldRequireCustoms: ShouldRequireCustomsForm,
    private val addressValidationHelper: AddressValidationHelper,
    private val verifyDestinationAddress: VerifyDestinationAddress,
    private val observeShippingLabelNotice: ObserveShippingLabelNotice,
    private val shouldRequireITN: ShouldRequireITN
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingLabelCreationFragmentArgs by savedState.navArgs()

    var actionSnackbar by mutableStateOf<ActionSnackbar?>(null)

    private val emptyOrder = Order.getEmptyOrder(Date(), Date())
    private val order = MutableStateFlow<Order>(emptyOrder)
    private val destinationAddress = MutableStateFlow<DestinationShippingAddress>(DestinationShippingAddress.EMPTY)
    private val shippingAddresses = MutableStateFlow<WooShippingAddresses?>(WooShippingAddresses.EMPTY)
    private val loadTrigger = MutableSharedFlow<Unit>()
    private val storeOptions = MutableStateFlow<StoreOptionsModel?>(StoreOptionsModel.EMPTY)

    private val shippableItems = MutableStateFlow<List<ShippableItemModel>>(emptyList())

    private val packageSelected = MutableStateFlow<PackageData?>(null)
    private val customsFormData = MutableStateFlow<CustomsData?>(null)
    private val packageWeight = MutableStateFlow<PackageWeight?>(null)
    private val packageSelection = MutableStateFlow<PackageSelectionState>(NotSelected)
    private val customsState = MutableStateFlow<CustomsState>(NotRequired)

    private val uiState = MutableStateFlow(
        UIControlsState(
            markOrderComplete = false,
            isShipmentDetailsExpanded = false,
            isAddressSelectionExpanded = false
        )
    )

    private val selectedRatesSortOrder = MutableStateFlow(ShippingSortOption.FASTEST)
    private val refreshShippingRates = MutableSharedFlow<Unit>()
    var customWeight by mutableStateOf("")
        private set

    private val purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.NoStarted)

    private val cheapestComparator = Comparator<ShippingRateUI> { r1, r2 ->
        r1.defaultRate.rate.price.compareTo(r2.defaultRate.rate.price)
    }
    private val fastestComparator = Comparator<ShippingRateUI> { r1, r2 ->
        r1.defaultRate.rate.deliveryDays.compareTo(r2.defaultRate.rate.deliveryDays)
    }

    private val selectedRate = MutableStateFlow<ShippingRateUI?>(null)
    private val shippingRates = MutableStateFlow<Map<CarrierUI, List<ShippingRateUI>>>(emptyMap())
    private val shippingRatesState = MutableStateFlow<ShippingRatesState>(ShippingRatesState.NoAvailable)

    val viewState: MutableStateFlow<WooShippingViewState> = MutableStateFlow(WooShippingViewState.Loading)

    init {
        launch { observeShippingLabelInformation() }
        launch { getStoreOptions() }
        launch { getDestinationAddress() }
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

    private suspend fun observeNotices() = observeShippingLabelNotice(shippingAddresses, customsState, viewModelScope)
        .onStart { delay(NOTIFICATIONS_DELAY) }
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

    private suspend fun getStoreOptions() {
        observeStoreOptions().collectLatest { options ->
            storeOptions.value = options
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

    @Suppress("ComplexCondition")
    @OptIn(FlowPreview::class)
    private suspend fun observeShippingRates() {
        combine(
            packageSelected,
            shippingAddresses,
            packageWeight,
            refreshShippingRates.onStart { emit(Unit) }
        ) { selectedPackage, addresses, packageWeight, _ ->
            if (selectedPackage != null && addresses != null) {
                ShippingRatesInfo(
                    orderId = navArgs.orderId,
                    packageSelected = selectedPackage,
                    shipFrom = addresses.shipFrom,
                    shipTo = addresses.shipTo.address,
                    weight = packageWeight?.totalWeight,
                    currencyCode = order.value.currency
                )
            } else {
                null
            }
        }
            .debounce(MULTIPLE_CALLS_DELAY)
            .collectLatest {
                updateShippingRates(it)
            }
    }

    private suspend fun observeShippingRatesState() {
        combine(
            shippingRates,
            selectedRate,
            selectedRatesSortOrder
        ) { shippingRates, selectedRate, selectedRatesSortOrder ->
            if (shippingRates.isEmpty()) {
                ShippingRatesState.NoAvailable
            } else {
                ShippingRatesState.DataState(
                    selectedRatesSortOrder,
                    sortShippingRates(selectedRatesSortOrder, shippingRates),
                    selectedRate
                )
            }
        }.collectLatest {
            shippingRatesState.value = it
        }
    }

    @OptIn(FlowPreview::class)
    private suspend fun observePackageWeight() {
        combine(
            shippableItems,
            packageSelected,
            snapshotFlow { customWeight }.debounce(TYPING_DELAY)
        ) { shippableItems, selectedPackage, customWeightString ->
            val itemsWeight = shippableItems.sumByFloat { it.weight }
            val packageWeight = selectedPackage?.weight?.toFloatOrNull()
            PackageWeight(
                itemsWeight = itemsWeight,
                packageWeight = packageWeight,
                customWeight = customWeightString.toFloatOrNull()
            )
        }.collectLatest {
            packageWeight.value = it
        }
    }

    private suspend fun observePackageChanges() {
        combine(
            packageSelected,
            packageWeight,
            storeOptions
        ) { packageSelected, packageWeight, storeOptions ->
            if (packageSelected == null || packageWeight == null) {
                NotSelected
            } else {
                DataAvailable(
                    selectedPackage = packageSelected,
                    defaultWeight = packageWeight.defaultWeight.toString(),
                    weightUnit = storeOptions?.weightUnit ?: ""
                )
            }
        }.collectLatest {
            packageSelection.value = it
        }
    }

    // This logic will be updated later once the Customs data state is available
    private fun observeCustomsDataChanges() {
        combine(
            shippingAddresses,
            customsFormData,
            shippableItems
        ) { addresses, customsData, shippableItems ->
            val customsRequired = addresses != null && shouldRequireCustoms(addresses)
            val itnMissing = customsFormData.value?.itn.isNullOrEmpty() && shippableItems.isItnRequired()

            when {
                customsRequired && itnMissing -> ItnMissing
                customsData != null -> CustomsState.DataAvailable(customsData)
                customsRequired -> Unavailable
                else -> NotRequired
            }
        }.onEach {
            customsState.value = it
        }.launchIn(viewModelScope)

        combine(
            packageSelected.filterNotNull(),
            customsState.filter { it is CustomsState.DataAvailable }
        ) { packageSelected, customState ->
            val customData = customState
                .run { this as? CustomsState.DataAvailable }
                ?.customsData?.copy(
                    packageId = packageSelected.id,
                    packageName = packageSelected.name
                )

            packageSelected.copy(customsData = customData)
        }.onEach {
            packageSelected.value = it
        }.launchIn(viewModelScope)
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

    private suspend fun updateShippingRates(shippingRatesInfo: ShippingRatesInfo?) {
        when {
            shippingRatesInfo == null -> shippingRatesState.value = ShippingRatesState.NoAvailable
            shippingRatesInfo.shipTo == null ||
                !addressValidationHelper.canFetchShippingRates(shippingRatesInfo.shipTo) ->
                shippingRatesState.value = ShippingRatesState.MissingInfo(
                    missingTitle = R.string.woo_shipping_labels_shipping_rates_missing_destination,
                    missingDescription = R.string.woo_shipping_labels_shipping_rates_missing_destination_desc
                )

            shippingRatesInfo.weight == null || shippingRatesInfo.weight == 0f ->
                shippingRatesState.value = ShippingRatesState.MissingInfo(
                    missingTitle = R.string.woo_shipping_labels_shipping_rates_missing_weight,
                    missingDescription = R.string.woo_shipping_labels_shipping_rates_missing_weight_desc
                )

            else -> {
                val sortOrder = selectedRatesSortOrder.value
                shippingRatesState.value = ShippingRatesState.Loading(sortOrder)

                val shippingRatesResult = getShippingRates(
                    shippingRatesInfo.orderId,
                    shippingRatesInfo.packageSelected,
                    shippingRatesInfo.shipTo,
                    shippingRatesInfo.shipFrom,
                    shippingRatesInfo.weight,
                    shippingRatesInfo.currencyCode
                )

                if (shippingRatesResult.isSuccess && shippingRatesResult.getOrThrow().isNotEmpty()) {
                    shippingRates.value = shippingRatesResult.getOrThrow()
                } else {
                    shippingRatesState.value = ShippingRatesState.Error
                }
                selectedRate.value = null
            }
        }
    }

    @Suppress("ComplexCondition")
    private suspend fun observeShippingLabelInformation() {
        combine(
            storeOptions.drop(1),
            order.drop(1),
            shippingAddresses.drop(1),
            shippingRatesState,
            packageSelection,
            uiState,
            purchaseState,
            customsState,
            loadTrigger.onStart { emit(Unit) }
        ) { storeOptions, order, addresses, shippingRates, packageSelection, uiState, purchaseState, customsState, _ ->
            if (storeOptions == null || addresses == null || purchaseState is PurchaseState.Error) {
                return@combine WooShippingViewState.Error
            }

            val items = getShippableItems(order)

            val destinationStatus = when {
                addressValidationHelper.isMissingDestinationAddress(addresses.shipTo.address) -> {
                    AddressStatus.MISSING_ADDRESS
                }

                addresses.shipTo.isVerified -> AddressStatus.VERIFIED
                else -> AddressStatus.UNVERIFIED
            }

            shippableItems.value = items

            val shippableItemsUI = items.map { item -> item.toUIModel(currencyFormatter, storeOptions) }
            val formattedTotalPrice = getTotalPrice(items)
            val formattedTotalWeight = getTotalWeight(items, storeOptions)

            val shippingLineSummary = getShippingLinesSummary(order)

            return@combine WooShippingViewState.DataState(
                shippableItems = ShippableItemsUI(
                    shippableItems = shippableItemsUI,
                    formattedTotalWeight = formattedTotalWeight,
                    formattedTotalPrice = formattedTotalPrice
                ),
                shippingLines = shippingLineSummary,
                shippingAddresses = addresses,
                shippingRates = shippingRates,
                packageSelection = packageSelection,
                uiState = uiState,
                purchaseState = purchaseState,
                customsState = customsState,
                destinationStatus = destinationStatus
            )
        }.collectLatest {
            viewState.value = it
        }
    }

    private fun getSelectedOriginAddress(originAddresses: List<OriginShippingAddress>): OriginShippingAddress {
        return shippingAddresses.value?.shipFrom?.takeIf {
            it != OriginShippingAddress.EMPTY
        } ?: originAddresses.first()
    }

    fun onShippingFromAddressChange(address: OriginShippingAddress) {
        shippingAddresses.value?.let {
            shippingAddresses.value = it.copy(shipFrom = address)
        }
    }

    fun onEditOriginAddress(address: OriginShippingAddress) {
        triggerEvent(StartOriginAddressEdit(address))
    }

    fun onEditDestinationAddress(destinationAddress: DestinationShippingAddress) {
        triggerEvent(
            StartDestinationAddressEdit(
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

    fun onShipmentDetailsExpandedChange(value: Boolean): Boolean {
        return if (uiState.value.isAddressSelectionExpanded.not()) {
            uiState.update { it.copy(isShipmentDetailsExpanded = value) }
            true
        } else {
            false
        }
    }

    fun onSelectAddressExpandedChange(value: Boolean): Boolean {
        uiState.update { it.copy(isAddressSelectionExpanded = value) }
        return true
    }

    private fun getTotalPrice(items: List<ShippableItemModel>): String {
        val totalPrice = items.sumOf { it.price }
        val formattedTotalPrice = items.firstOrNull()?.currency?.let {
            currencyFormatter.formatCurrency(totalPrice, it)
        } ?: currencyFormatter.formatCurrency(totalPrice)
        return formattedTotalPrice
    }

    private fun getTotalWeight(items: List<ShippableItemModel>, storeOptions: StoreOptionsModel): String {
        val totalWeight = items.sumByFloat { it.weight * it.quantity }
        return "${totalWeight.formatToString()} ${storeOptions.weightUnit}"
    }

    private fun getShippingLinesSummary(order: Order): List<ShippingLineSummaryUI> {
        return order.shippingLines.map {
            ShippingLineSummaryUI(
                title = it.methodTitle,
                amount = currencyFormatter.formatCurrency(it.total, order.currency)
            )
        }
    }

    fun onSelectPackageClicked() {
        triggerEvent(StartPackageSelection)
    }

    @Suppress("ComplexCondition")
    fun onPurchaseShippingLabel() {
        val selectedPackage = packageSelected.value
        val addresses = shippingAddresses.value
        val shippingRate = selectedRate.value?.selectedOption?.rate
        val weight = packageWeight.value?.totalWeight

        if (selectedPackage == null || addresses == null || shippingRate == null || weight == null) return

        val orderId = navArgs.orderId
        val lastOrderComplete = uiState.value.markOrderComplete
        val shippableItemsIdList = shippableItems.value.map { it.productId }

        val backupPurchaseState = purchaseState.value
        purchaseState.value = PurchaseState.InProgress

        launch {
            val result = purchaseShippingLabel(
                orderId,
                shippableItemsIdList,
                selectedPackage,
                addresses.shipTo.address,
                addresses.shipFrom,
                shippingRate,
                weight,
                lastOrderComplete
            )
            if (result.isSuccess) {
                purchaseState.value = PurchaseState.Success
                result.getOrNull()
                    ?.labels
                    ?.firstOrNull()
                    ?.let { purchasedLabel ->
                        val currentViewState = (viewState.value as? WooShippingViewState.DataState)
                        val selectedRate = selectedRate.value
                        if (currentViewState != null && selectedRate != null) {
                            PurchasedShippingLabelData(
                                labelId = purchasedLabel.labelId,
                                orderId = navArgs.orderId,
                                carrierId = purchasedLabel.carrierId,
                                trackingNumber = purchasedLabel.tracking,
                                addresses = currentViewState.shippingAddresses,
                                items = currentViewState.shippableItems,
                                rateSummary = selectedRate.summary,
                                shippingLines = currentViewState.shippingLines
                            )
                        } else {
                            null
                        }
                    }?.let { triggerEvent(LabelPurchased(purchaseData = it)) }
            } else {
                purchaseState.value = backupPurchaseState
                actionSnackbar = ActionSnackbar(
                    R.string.woo_shipping_labels_purchase_error,
                    R.string.retry
                ) { onPurchaseShippingLabel() }
            }
        }
    }

    fun onSelectedRateSortOrderChanged(option: ShippingSortOption) {
        selectedRatesSortOrder.value = option
    }

    fun onSelectedSippingRateChanged(rate: ShippingRateUI) {
        selectedRate.update { rate }
    }

    fun onSplitShipment() {
        triggerEvent(StartSplitShipment)
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
        packageSelected.value = packageData
    }

    fun onCustomsDataAvailable(customsData: CustomsData) {
        customsFormData.value = customsData
    }

    fun onCustomWeightChange(input: String) {
        customWeight = input
    }

    fun onEditCustomsClick() {
        val destinationCountryCode = shippingAddresses.value
            ?.shipTo?.address?.country?.code.orEmpty()

        val event = StartCustomsFormEdit(
            shippableItems = shippableItems.value,
            destinationCountryCode = destinationCountryCode,
            customData = customsFormData.value
        )
        triggerEvent(event)
    }

    fun allowBackNavigation(): Boolean {
        val state = uiState.value
        return when {
            state.isAddressSelectionExpanded -> {
                uiState.update { it.copy(isAddressSelectionExpanded = false) }
                false
            }

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
        viewState.value = WooShippingViewState.Loading

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
            ?: 0f

        val destinationCountryCode = shippingAddresses.value
            ?.shipTo?.address?.country?.code.orEmpty()

        return shouldRequireITN(destinationCountryCode, totalShippingValue)
    }

    data object StartPackageSelection : Event()
    data class LabelPurchased(val purchaseData: PurchasedShippingLabelData) : Event()
    data class StartOriginAddressEdit(val originAddress: OriginShippingAddress) : Event()
    data class StartDestinationAddressEdit(
        val destinationAddress: DestinationShippingAddress,
        val orderId: Long
    ) : Event()

    data object StartSplitShipment : Event()

    data class StartCustomsFormEdit(
        val shippableItems: List<ShippableItemModel>,
        val destinationCountryCode: String,
        val customData: CustomsData?
    ) : Event()

    sealed class WooShippingViewState {
        data object Error : WooShippingViewState()
        data object Loading : WooShippingViewState()
        data class DataState(
            val shippableItems: ShippableItemsUI,
            val shippingLines: List<ShippingLineSummaryUI>,
            val shippingAddresses: WooShippingAddresses,
            val shippingRates: ShippingRatesState,
            val packageSelection: PackageSelectionState,
            val uiState: UIControlsState,
            val purchaseState: PurchaseState,
            val customsState: CustomsState,
            val destinationStatus: AddressStatus
        ) : WooShippingViewState()
    }

    data class ActionSnackbar(
        val message: Int,
        val actionLabel: Int,
        val action: () -> Unit
    )

    sealed class ShippingRatesState {
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

    sealed class PackageSelectionState {
        data object NotSelected : PackageSelectionState()
        data class DataAvailable(
            val selectedPackage: PackageData,
            val defaultWeight: String,
            val weightUnit: String
        ) : PackageSelectionState()
    }

    sealed class PurchaseState {
        data object NoStarted : PurchaseState()
        data object InProgress : PurchaseState()
        data object Success : PurchaseState()
        data object Error : PurchaseState()
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
        val isShipmentDetailsExpanded: Boolean,
        val isAddressSelectionExpanded: Boolean,
        val noticeBannerUiState: NoticeBannerUiState? = null
    )

    data class ShippingRatesInfo(
        val orderId: Long,
        val packageSelected: PackageData,
        val shipFrom: OriginShippingAddress,
        val shipTo: Address?,
        val weight: Float?,
        val currencyCode: String?
    )

    // This will be extended later introducing the state with data coming from the Customs form
    sealed class CustomsState {
        data object NotRequired : CustomsState()
        data object ItnMissing : CustomsState()
        data object Unavailable : CustomsState()
        data class DataAvailable(val customsData: CustomsData) : CustomsState()
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
