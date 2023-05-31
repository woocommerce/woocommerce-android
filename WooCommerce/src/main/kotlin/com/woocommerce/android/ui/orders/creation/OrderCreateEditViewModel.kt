package com.woocommerce.android.ui.orders.creation

import android.os.Parcelable
import android.view.View
import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.BARCODE_SCANNING_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.BARCODE_SCANNING_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_COUPON_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_COUPON_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_PRODUCT_BARCODE_SCANNING_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CUSTOMER_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_FEE_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_FEE_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_NOTE_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_SHIPPING_METHOD_REMOVE
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_STATUS_CHANGE
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_FAILURE
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_COUPONS_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_CONTEXT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FROM
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_CUSTOMER_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_DIFFERENT_SHIPPING_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_FEES
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_SHIPPING_METHOD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PARENT_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ADDED_VIA
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_BARCODE_FORMAT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_FAILURE_REASON
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_SOURCE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TO
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.OrderNoteType.CUSTOMER
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_CREATION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_EDITING
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.extensions.runWithContext
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.model.Order.ShippingLine
import com.woocommerce.android.tracker.OrderDurationRecorder
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCoupon
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.SelectItems
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorRestriction
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.SelectedItem.Product
import com.woocommerce.android.ui.products.selector.variationIds
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import javax.inject.Inject
import com.woocommerce.android.model.Product as ModelProduct

@HiltViewModel
@Suppress("LargeClass")
class OrderCreateEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    private val orderDetailRepository: OrderDetailRepository,
    private val orderCreateEditRepository: OrderCreateEditRepository,
    private val mapItemToProductUiModel: MapItemToProductUiModel,
    private val createOrderItem: CreateOrderItem,
    private val determineMultipleLinesContext: DetermineMultipleLinesContext,
    private val tracker: AnalyticsTrackerWrapper,
    private val codeScanner: CodeScanner,
    private val productRepository: ProductListRepository,
    autoSyncOrder: AutoSyncOrder,
    autoSyncPriceModifier: AutoSyncPriceModifier,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val PARAMETERS_KEY = "parameters_key"
        private const val ORDER_CUSTOM_FEE_NAME = "order_custom_fee"
    }

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val args: OrderCreateEditFormFragmentArgs by savedState.navArgs()
    val mode: Mode = args.mode

    private val flow = when (mode) {
        Mode.Creation -> VALUE_FLOW_CREATION
        is Mode.Edit -> VALUE_FLOW_EDITING
    }

    private val _orderDraft = savedState.getStateFlow(viewModelScope, Order.EMPTY)
    val orderDraft = _orderDraft
        .asLiveData()

    val orderStatusData: LiveData<OrderStatus> = _orderDraft
        .map { it.status }
        .distinctUntilChanged()
        .map { status ->
            withContext(dispatchers.io) {
                orderDetailRepository.getOrderStatus(status.value)
            }
        }.asLiveData()

    val products: LiveData<List<ProductUIModel>> = _orderDraft
        .map { order -> order.items.filter { it.quantity > 0 } }
        .distinctUntilChanged()
        .map { items ->
            items.map { item -> mapItemToProductUiModel(item) }
        }.asLiveData()

    private val retryOrderDraftUpdateTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val syncStrategy =
        when (mode) {
            Mode.Creation -> autoSyncPriceModifier
            is Mode.Edit -> autoSyncOrder
        }

    fun getProductUIModelFromItem(item: Order.Item) = runBlocking {
        mapItemToProductUiModel(item)
    }

    val currentDraft
        get() = _orderDraft.value

    private val orderCreationStatus = Order.Status.Custom(Order.Status.AUTO_DRAFT)

    init {
        when (mode) {
            Mode.Creation -> {
                _orderDraft.update {
                    it.copy(
                        currency = parameterRepository.getParameters(
                            PARAMETERS_KEY,
                            savedState
                        ).currencyCode.orEmpty()
                    )
                }
                monitorOrderChanges()
                // Presence of barcode indicates that this screen was called from the
                // Order listing screen after scanning the barcode.
                if (args.sku.isNotNullOrEmpty() && args.barcodeFormat != null) {
                    fetchProductBySKU(args.sku!!, args.barcodeFormat!!, ScanningSource.ORDER_LIST)
                }
            }
            is Mode.Edit -> {
                viewModelScope.launch {
                    orderDetailRepository.getOrderById(mode.orderId)?.let { order ->
                        _orderDraft.value = order
                        viewState = viewState.copy(
                            isUpdatingOrderDraft = false,
                            showOrderUpdateSnackbar = false,
                            isEditable = order.isEditable,
                            multipleLinesContext = determineMultipleLinesContext(order)
                        )
                        monitorOrderChanges()
                        updateCouponButtonVisibility(order)
                    }
                }
            }
        }
    }

    fun onCustomerNoteEdited(newNote: String) {
        _orderDraft.value.let { order ->
            tracker.track(
                ORDER_NOTE_ADD,
                mapOf(
                    KEY_PARENT_ID to order.id,
                    KEY_STATUS to order.status,
                    KEY_TYPE to CUSTOMER,
                    KEY_FLOW to flow,
                )
            )
        }
        _orderDraft.update { it.copy(customerNote = newNote) }
    }

    fun onIncreaseProductsQuantity(id: Long) {
        tracker.track(
            ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(KEY_FLOW to flow)
        )
        _orderDraft.update { it.adjustProductQuantity(id, +1) }
    }

    fun onDecreaseProductsQuantity(id: Long) {
        _orderDraft.value.items
            .find { it.itemId == id }
            ?.takeIf { it.quantity == 1F }
            ?.let { onProductClicked(it) }
            ?: run {
                tracker.track(
                    ORDER_PRODUCT_QUANTITY_CHANGE,
                    mapOf(KEY_FLOW to flow)
                )
                _orderDraft.update { it.adjustProductQuantity(id, -1) }
            }
    }

    fun onOrderStatusChanged(status: Order.Status) {
        tracker.track(
            ORDER_STATUS_CHANGE,
            mapOf(
                KEY_ID to _orderDraft.value.id,
                KEY_FROM to _orderDraft.value.status.value,
                KEY_TO to status.value,
                KEY_FLOW to flow
            )
        )
        _orderDraft.update { it.copy(status = status) }
    }

    fun onRemoveProduct(item: Order.Item) = viewModelScope.launch {
        tracker.track(
            ORDER_PRODUCT_REMOVE,
            mapOf(KEY_FLOW to flow)
        )
        viewState = viewState.copy(isEditable = false)
        _orderDraft.update {
            it.removeItem(item)
        }
    }

    fun onProductsSelected(
        selectedItems: Collection<SelectedItem>,
        source: ScanningSource? = null,
        addedVia: ProductAddedVia = ProductAddedVia.MANUALLY
    ) {
        source?.let {
            tracker.track(
                ORDER_PRODUCT_ADD,
                mapOf(
                    KEY_FLOW to flow,
                    KEY_PRODUCT_COUNT to selectedItems.size,
                    KEY_SCANNING_SOURCE to source.source,
                    KEY_PRODUCT_ADDED_VIA to addedVia.addedVia,
                )
            )
        } ?: run {
            tracker.track(
                ORDER_PRODUCT_ADD,
                mapOf(
                    KEY_FLOW to flow,
                    KEY_PRODUCT_COUNT to selectedItems.size,
                    KEY_PRODUCT_ADDED_VIA to addedVia.addedVia,
                )
            )
        }

        viewModelScope.launch {
            _orderDraft.value.items.apply {
                val productsToRemove = filter { item ->
                    !item.isVariation && selectedItems.filterIsInstance<Product>().none { item.productId == it.id }
                }
                productsToRemove.forEach { itemToRemove ->
                    _orderDraft.update { order -> order.removeItem(itemToRemove) }
                }

                val variationsToRemove = filter { item ->
                    item.isVariation && selectedItems.variationIds.none { item.variationId == it }
                }
                variationsToRemove.forEach { itemToRemove ->
                    _orderDraft.update { order -> order.removeItem(itemToRemove) }
                }

                val itemsToAdd = selectedItems.filter { selectedItem ->
                    if (selectedItem is SelectedItem.ProductVariation) {
                        none { it.variationId == selectedItem.variationId }
                    } else {
                        none { it.productId == selectedItem.id }
                    }
                }.map {
                    if (it is SelectedItem.ProductVariation) {
                        createOrderItem(it.productId, it.variationId)
                    } else {
                        createOrderItem(it.id)
                    }
                }

                _orderDraft.update { order -> order.updateItems(order.items + itemsToAdd) }
            }
        }
    }

    private fun updateCouponButtonVisibility(order: Order) {
        viewState = viewState.copy(isCouponButtonEnabled = order.hasProducts() && order.isEditable)
    }

    private fun Order.hasProducts() = items.any { it.quantity > 0 }

    fun onScanClicked() {
        trackBarcodeScanningTapped()
        startScan()
    }

    private fun trackBarcodeScanningTapped() {
        tracker.track(ORDER_CREATION_PRODUCT_BARCODE_SCANNING_TAPPED)
    }
    private fun startScan() {
        viewModelScope.launch {
            codeScanner.startScan().collect { status ->
                when (status) {
                    is CodeScannerStatus.Failure -> {
                        tracker.track(
                            BARCODE_SCANNING_FAILURE,
                            mapOf(
                                KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source,
                                KEY_SCANNING_FAILURE_REASON to status.type.toString(),
                            )
                        )
                        sendAddingProductsViaScanningFailedEvent(
                            R.string.order_creation_barcode_scanning_scanning_failed
                        )
                    }
                    is CodeScannerStatus.Success -> {
                        tracker.track(
                            BARCODE_SCANNING_SUCCESS,
                            mapOf(KEY_SCANNING_SOURCE to ScanningSource.ORDER_CREATION.source)
                        )
                        viewState = viewState.copy(isUpdatingOrderDraft = true)
                        fetchProductBySKU(status.code, status.format)
                    }
                }
            }
        }
    }

    private fun fetchProductBySKU(
        sku: String,
        barcodeFormat: BarcodeFormat,
        source: ScanningSource = ScanningSource.ORDER_CREATION
    ) {
        val selectedItems = orderDraft.value?.items?.map { item ->
            if (item.isVariation) {
                SelectedItem.ProductVariation(item.productId, item.variationId)
            } else {
                SelectedItem.Product(item.productId)
            }
        }.orEmpty()
        viewModelScope.launch {
            productRepository.searchProductList(
                searchQuery = sku,
                skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
            )?.let { products ->
                viewState = viewState.copy(isUpdatingOrderDraft = false)
                products.firstOrNull()?.let { product ->
                    addScannedProduct(product, selectedItems, source, barcodeFormat)
                } ?: run {
                    trackProductSearchViaSKUFailureEvent(
                        source,
                        barcodeFormat,
                        "Empty data response (no product found for the SKU)",
                    )
                    sendAddingProductsViaScanningFailedEvent(
                        R.string.order_creation_barcode_scanning_unable_to_add_product
                    )
                }
            } ?: run {
                trackProductSearchViaSKUFailureEvent(
                    source,
                    barcodeFormat,
                    "Product search via SKU API call failed"
                )
                sendAddingProductsViaScanningFailedEvent(
                    R.string.order_creation_barcode_scanning_unable_to_add_product
                )
            }
        }
    }

    private fun addScannedProduct(
        product: ModelProduct,
        selectedItems: List<SelectedItem>,
        source: ScanningSource,
        barcodeFormat: BarcodeFormat
    ) {
        if (product.isVariable()) {
            if (product.parentId == 0L) {
                sendAddingProductsViaScanningFailedEvent(
                    message = string.order_creation_barcode_scanning_unable_to_add_variable_product
                )
                trackProductSearchViaSKUFailureEvent(
                    source,
                    barcodeFormat,
                    "Instead of specific variations, user tried to add parent variable product."
                )
                return
            } else {
                when (val alreadySelectedItemId = getItemIdIfVariableProductIsAlreadySelected(product)) {
                    null -> onProductsSelected(
                        selectedItems = selectedItems +
                            SelectedItem.ProductVariation(
                                productId = product.parentId,
                                variationId = product.remoteId
                            ),
                        source = source,
                        addedVia = ProductAddedVia.SCANNING,
                    )
                    else -> onIncreaseProductsQuantity(alreadySelectedItemId)
                }
            }
        } else {
            when (val alreadySelectedItemId = getItemIdIfProductIsAlreadySelected(product)) {
                null -> onProductsSelected(
                    selectedItems = selectedItems + Product(productId = product.remoteId),
                    source = source,
                    addedVia = ProductAddedVia.SCANNING,
                )
                else -> onIncreaseProductsQuantity(alreadySelectedItemId)
            }
        }
        trackProductSearchViaSKUSuccessEvent(source)
    }

    private fun trackProductSearchViaSKUSuccessEvent(source: ScanningSource) {
        tracker.track(
            PRODUCT_SEARCH_VIA_SKU_SUCCESS,
            mapOf(
                KEY_SCANNING_SOURCE to source.source
            )
        )
    }

    private fun trackProductSearchViaSKUFailureEvent(
        source: ScanningSource,
        barcodeFormat: BarcodeFormat,
        message: String,
    ) {
        tracker.track(
            PRODUCT_SEARCH_VIA_SKU_FAILURE,
            mapOf(
                KEY_SCANNING_SOURCE to source.source,
                KEY_SCANNING_BARCODE_FORMAT to barcodeFormat.formatName,
                KEY_SCANNING_FAILURE_REASON to message,
            )
        )
    }

    private fun getItemIdIfVariableProductIsAlreadySelected(product: ModelProduct): Long? {
        return _orderDraft.value.items.firstOrNull { item ->
            item.variationId == product.remoteId
        }?.itemId
    }

    private fun getItemIdIfProductIsAlreadySelected(product: ModelProduct): Long? {
        return _orderDraft.value.items.firstOrNull { item ->
            item.productId == product.remoteId
        }?.itemId
    }

    private fun sendAddingProductsViaScanningFailedEvent(
        @StringRes message: Int
    ) {
        triggerEvent(
            OnAddingProductViaScanningFailed(message) {
                startScan()
            }
        )
    }

    private fun Order.removeItem(item: Order.Item) = adjustProductQuantity(item.itemId, -item.quantity.toInt())

    fun onCustomerAddressEdited(customerId: Long?, billingAddress: Address, shippingAddress: Address) {
        val hasDifferentShippingDetails = _orderDraft.value.shippingAddress != _orderDraft.value.billingAddress
        tracker.track(
            ORDER_CUSTOMER_ADD,
            mapOf(
                KEY_FLOW to flow,
                KEY_HAS_DIFFERENT_SHIPPING_DETAILS to hasDifferentShippingDetails
            )
        )

        _orderDraft.update { order ->
            order.copy(
                customerId = customerId,
                billingAddress = billingAddress,
                shippingAddress = shippingAddress.takeIf { it != Address.EMPTY } ?: billingAddress
            )
        }
    }

    fun onEditOrderStatusClicked(currentStatus: OrderStatus) {
        launch(dispatchers.io) {
            orderDetailRepository
                .getOrderStatusOptions().toTypedArray()
                .runWithContext(dispatchers.main) {
                    triggerEvent(
                        ViewOrderStatusSelector(
                            currentStatus = currentStatus.statusKey,
                            orderStatusList = it
                        )
                    )
                }
        }
    }

    fun onCustomerClicked() {
        triggerEvent(EditCustomer)
    }

    fun onCustomerNoteClicked() {
        triggerEvent(EditCustomerNote)
    }

    fun onAddProductClicked() {
        val selectedItems = orderDraft.value?.items?.map { item ->
            if (item.isVariation) {
                SelectedItem.ProductVariation(item.productId, item.variationId)
            } else {
                Product(item.productId)
            }
        }.orEmpty()
        triggerEvent(
            SelectItems(
                selectedItems,
                listOf(
                    ProductSelectorRestriction.OnlyPublishedProducts,
                    ProductSelectorRestriction.NoVariableProductsWithNoVariations
                )
            )
        )
    }

    fun onProductClicked(item: Order.Item) {
        // Don't show details if the product is not synced yet
        if (!item.isSynced()) return
        triggerEvent(ShowProductDetails(item))
    }

    fun onRetryPaymentsClicked() {
        retryOrderDraftUpdateTrigger.tryEmit(Unit)
    }

    fun onFeeButtonClicked() {
        val order = _orderDraft.value
        val currentFee = order.feesLines.firstOrNull()

        val currentFeeValue = currentFee?.total
        val currentFeeTotalValue = currentFee?.getTotalValue() ?: BigDecimal.ZERO

        val orderSubtotal = order.total - currentFeeTotalValue
        triggerEvent(EditFee(orderSubtotal, currentFeeValue))
    }

    fun onCouponButtonClicked() {
        triggerEvent(EditCoupon(_orderDraft.value.couponLines.firstOrNull()?.code))
    }

    fun onShippingButtonClicked() {
        triggerEvent(EditShipping(currentDraft.shippingLines.firstOrNull { it.methodId != null }))
    }

    fun onCreateOrderClicked(order: Order) {
        when (mode) {
            Mode.Creation -> viewModelScope.launch {
                trackCreateOrderButtonClick()
                viewState = viewState.copy(isProgressDialogShown = true)
                orderCreateEditRepository.placeOrder(order).fold(
                    onSuccess = {
                        trackOrderCreationSuccess()
                        triggerEvent(ShowSnackbar(string.order_creation_success_snackbar))
                        triggerEvent(ShowCreatedOrder(it.id))
                    },
                    onFailure = {
                        trackOrderCreationFailure(it)
                        viewState = viewState.copy(isProgressDialogShown = false)
                        triggerEvent(ShowSnackbar(string.order_creation_failure_snackbar))
                    }
                )
            }
            is Mode.Edit -> {
                triggerEvent(Exit)
            }
        }
    }

    private fun trackOrderCreationSuccess() {
        tracker.track(
            ORDER_CREATION_SUCCESS,
            mutableMapOf<String, Any>().also { mutableMap ->
                OrderDurationRecorder.millisecondsSinceOrderAddNew().getOrNull()?.let { timeElapsed ->
                    mutableMap[AnalyticsTracker.KEY_TIME_ELAPSED_SINCE_ADD_NEW_ORDER_IN_MILLIS] =
                        timeElapsed.toString()
                }
                mutableMap[KEY_COUPONS_COUNT] = orderDraft.value?.couponLines?.size ?: 0
            }
        )
    }

    fun onBackButtonClicked() {
        when (mode) {
            Mode.Creation -> {
                if (_orderDraft.value.isEmpty()) {
                    triggerEvent(Exit)
                } else {
                    triggerEvent(
                        ShowDialog.buildDiscardDialogEvent(
                            positiveBtnAction = { _, _ ->
                                val draft = _orderDraft.value
                                if (draft.id != 0L) {
                                    launch { orderCreateEditRepository.deleteDraftOrder(draft) }
                                }
                                triggerEvent(Exit)
                            }
                        )
                    )
                }
            }
            is Mode.Edit -> {
                triggerEvent(Exit)
            }
        }
    }

    /**
     * Monitor order changes, and update the remote draft to update price totals
     */
    private fun monitorOrderChanges() {
        viewModelScope.launch {
            val changes =
                if (mode is Mode.Edit) {
                    _orderDraft.drop(1)
                } else {
                    // When we are in the order creation flow, we need to keep the order status as auto-draft.
                    // In this way, when the draft of the created order needs to synchronize the price modifiers,
                    // the application does not send notifications or synchronize its status on other devices.
                    _orderDraft.map { order -> order.copy(status = orderCreationStatus) }
                }
            syncStrategy.syncOrderChanges(changes, retryOrderDraftUpdateTrigger)
                .collect { updateStatus ->
                    when (updateStatus) {
                        OrderUpdateStatus.PendingDebounce ->
                            viewState = viewState.copy(willUpdateOrderDraft = true, showOrderUpdateSnackbar = false)
                        OrderUpdateStatus.Ongoing ->
                            viewState = viewState.copy(willUpdateOrderDraft = false, isUpdatingOrderDraft = true)
                        is OrderUpdateStatus.Failed -> {
                            trackOrderSyncFailed(updateStatus.throwable)
                            viewState = viewState.copy(isUpdatingOrderDraft = false, showOrderUpdateSnackbar = true)
                        }
                        is OrderUpdateStatus.Succeeded -> {
                            viewState = viewState.copy(
                                isUpdatingOrderDraft = false,
                                showOrderUpdateSnackbar = false,
                                isEditable = isOrderEditable(updateStatus),
                                multipleLinesContext = determineMultipleLinesContext(updateStatus.order)
                            )
                            _orderDraft.updateAndGet { currentDraft ->
                                if (mode is Mode.Creation) {
                                    // Once the order is synced, revert the auto-draft status and keep
                                    // the user's selected one
                                    updateStatus.order.copy(status = currentDraft.status)
                                } else {
                                    updateStatus.order
                                }
                            }.also {
                                updateCouponButtonVisibility(it)
                            }
                        }
                    }
                }
        }
    }

    private fun isOrderEditable(updateStatus: OrderUpdateStatus.Succeeded) =
        updateStatus.order.isEditable || mode is Mode.Creation

    private fun trackOrderCreationFailure(it: Throwable) {
        tracker.track(
            ORDER_CREATION_FAILED,
            mapOf(
                KEY_ERROR_CONTEXT to this::class.java.simpleName,
                KEY_ERROR_TYPE to (it as? WooException)?.error?.type?.name,
                KEY_ERROR_DESC to it.message
            )
        )
    }

    private fun trackCreateOrderButtonClick() {
        tracker.track(
            ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                KEY_STATUS to _orderDraft.value.status,
                KEY_PRODUCT_COUNT to products.value?.count(),
                KEY_HAS_CUSTOMER_DETAILS to _orderDraft.value.billingAddress.hasInfo(),
                KEY_HAS_FEES to _orderDraft.value.feesLines.isNotEmpty(),
                KEY_HAS_SHIPPING_METHOD to _orderDraft.value.shippingLines.isNotEmpty()
            )
        )
    }

    private fun trackOrderSyncFailed(throwable: Throwable) {
        tracker.track(
            stat = AnalyticsEvent.ORDER_SYNC_FAILED,
            properties = mapOf(KEY_FLOW to flow),
            errorContext = this::class.java.simpleName,
            errorType = (throwable as? WooException)?.error?.type?.name,
            errorDescription = (throwable as? WooException)?.error?.message
        )
    }

    fun onShippingEdited(amount: BigDecimal, name: String) {
        tracker.track(
            ORDER_SHIPPING_METHOD_ADD,
            mapOf(KEY_FLOW to flow)
        )

        _orderDraft.update { draft ->
            val shipping: List<ShippingLine> = draft.shippingLines.mapIndexed { index, shippingLine ->
                if (index == 0) {
                    shippingLine.copy(total = amount, methodTitle = name)
                } else {
                    shippingLine
                }
            }.ifEmpty {
                listOf(ShippingLine(methodId = "other", total = amount, methodTitle = name))
            }

            draft.copy(shippingLines = shipping)
        }
    }

    fun onShippingRemoved() {
        tracker.track(
            ORDER_SHIPPING_METHOD_REMOVE,
            mapOf(KEY_FLOW to flow)
        )
        _orderDraft.update { draft ->
            draft.copy(
                shippingLines = draft.shippingLines.mapIndexed { index, shippingLine ->
                    if (index == 0) {
                        // Setting methodId to null will remove the shipping line in core
                        shippingLine.copy(methodId = null)
                    } else {
                        shippingLine
                    }
                }
            )
        }
    }

    fun onFeeEdited(feeValue: BigDecimal) {
        tracker.track(
            ORDER_FEE_ADD,
            mapOf(KEY_FLOW to flow)
        )

        _orderDraft.update { draft ->
            val fees: List<Order.FeeLine> = draft.feesLines.mapIndexed { index, feeLine ->
                if (index == 0) {
                    feeLine.copy(total = feeValue)
                } else {
                    feeLine
                }
            }.ifEmpty {
                listOf(
                    Order.FeeLine.EMPTY.copy(
                        name = ORDER_CUSTOM_FEE_NAME,
                        total = feeValue
                    )
                )
            }

            draft.copy(feesLines = fees)
        }
    }

    fun onFeeRemoved() {
        tracker.track(
            ORDER_FEE_REMOVE,
            mapOf(KEY_FLOW to flow)
        )
        _orderDraft.update { draft ->
            draft.copy(
                feesLines = draft.feesLines.mapIndexed { index, feeLine ->
                    if (index == 0) {
                        feeLine.copy(name = null)
                    } else {
                        feeLine
                    }
                }
            )
        }
    }

    fun onCouponEntered(couponCode: String?) {
        _orderDraft.update { draft ->
            val couponLines = if (couponCode.isNullOrEmpty()) {
                trackCouponRemoved()
                emptyList()
            } else {
                trackCouponAdded()
                listOf(Order.CouponLine(code = couponCode))
            }
            draft.copy(couponLines = couponLines)
        }
    }

    private fun trackCouponAdded() {
        tracker.track(ORDER_COUPON_ADD, mapOf(KEY_FLOW to flow))
    }

    private fun trackCouponRemoved() {
        tracker.track(ORDER_COUPON_REMOVE, mapOf(KEY_FLOW to flow))
    }

    @Parcelize
    data class ViewState(
        val isProgressDialogShown: Boolean = false,
        val willUpdateOrderDraft: Boolean = false,
        val isUpdatingOrderDraft: Boolean = false,
        val showOrderUpdateSnackbar: Boolean = false,
        val isCouponButtonEnabled: Boolean = false,
        val isEditable: Boolean = true,
        val multipleLinesContext: MultipleLinesContext = MultipleLinesContext.None
    ) : Parcelable {
        @IgnoredOnParcel
        val canCreateOrder: Boolean = !willUpdateOrderDraft && !isUpdatingOrderDraft && !showOrderUpdateSnackbar

        @IgnoredOnParcel
        val isIdle: Boolean = !isUpdatingOrderDraft && !willUpdateOrderDraft
    }

    sealed class Mode : Parcelable {
        @Parcelize
        object Creation : Mode()

        @Parcelize
        data class Edit(val orderId: Long) : Mode()
    }

    sealed class MultipleLinesContext : Parcelable {
        @Parcelize
        object None : MultipleLinesContext()

        @Parcelize
        data class Warning(
            val header: String,
            val explanation: String,
        ) : MultipleLinesContext()
    }
}

data class OnAddingProductViaScanningFailed(
    val message: Int,
    val retry: View.OnClickListener,
) : Event()

data class ProductUIModel(
    val item: Order.Item,
    val imageUrl: String,
    val isStockManaged: Boolean,
    val stockQuantity: Double,
    val stockStatus: ProductStockStatus
)

enum class ScanningSource(val source: String) {
    ORDER_CREATION("order_creation"),
    ORDER_LIST("order_list")
}

enum class ProductAddedVia(val addedVia: String) {
    MANUALLY("manually"),
    SCANNING("scanning")
}

private fun ModelProduct.isVariable() =
    productType == ProductType.VARIABLE ||
        productType == ProductType.VARIABLE_SUBSCRIPTION ||
        productType == ProductType.VARIATION

fun Order.Item.isSynced() = this.itemId != 0L
