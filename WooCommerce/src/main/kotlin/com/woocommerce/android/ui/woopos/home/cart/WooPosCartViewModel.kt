package com.woocommerce.android.ui.woopos.home.cart

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.modifier.BarcodeInputDetector
import com.woocommerce.android.ui.woopos.common.data.WooPosGetCouponById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.common.data.WooPosGetVariationById
import com.woocommerce.android.ui.woopos.common.data.WooPosVariation
import com.woocommerce.android.ui.woopos.common.data.WooPosVariationMapper
import com.woocommerce.android.ui.woopos.common.data.getNameForPOS
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosSearchByIdentifier
import com.woocommerce.android.ui.woopos.common.data.searchbyidentifier.WooPosSearchByIdentifierResult
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.common.util.WooPosSoundHelper
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.CouponsRemoved
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.OnNewTransactionStarted
import com.woocommerce.android.ui.woopos.home.ParentToChildrenEvent
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.home.WooPosParentToChildrenEventReceiver
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Coupon.CouponValidationState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState.Product
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.CHECKOUT
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.EDITABLE
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartStatus.EMPTY
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.util.WooPosGetCachedStoreCurrency
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.BackToCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CheckoutTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ClearCartTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.CustomAmountSubmitted
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.EmptyCartSetUpScannerTapped
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.InteractionWithCustomerStarted
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent.Event.ItemRemovedFromCart
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTrackingDataKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosBarcodeEventTracker
import com.woocommerce.android.ui.woopos.util.format.WooPosCouponsFormatter
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

@Suppress("LargeClass")
@HiltViewModel
class WooPosCartViewModel @Inject constructor(
    private val childrenToParentEventSender: WooPosChildrenToParentEventSender,
    private val parentToChildrenEventReceiver: WooPosParentToChildrenEventReceiver,
    private val getProductById: WooPosGetProductById,
    private val getCouponById: WooPosGetCouponById,
    private val couponFormatter: WooPosCouponsFormatter,
    private val getVariationsById: WooPosGetVariationById,
    private val resourceProvider: ResourceProvider,
    private val formatPrice: WooPosFormatPrice,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsTrackingDataKeeper: WooPosAnalyticsTrackingDataKeeper,
    private val updateCartItemsWithChanges: WooPosCartItemsUpdater,
    private val customAmountCartHandler: WooPosCustomAmountCartHandler,
    private val getCachedStoreCurrency: WooPosGetCachedStoreCurrency,
    private val searchByIdentifier: WooPosSearchByIdentifier,
    private val wooPosLogWrapper: WooPosLogWrapper,
    private val soundHelper: WooPosSoundHelper,
    private val barcodeEventTracker: WooPosBarcodeEventTracker,
    private val variationMapper: WooPosVariationMapper,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = WooPosCartState(),
        key = "cartViewState"
    )

    val state: LiveData<WooPosCartState> = _state
        .asLiveData()
        .map { updateCartStatusDependingOnItems(it).also { newState -> updateAnalyticsData(newState) } }
        .map { updateToolbarState(it) }
        .map { updateStateDependingOnCartStatus(it) }

    private val itemNumberProvider = AtomicInteger(getInitialValueOrHighestUsedItemNumberAfterProcessDeath())

    init {
        listenEventsFromParent()
        viewModelScope.launch {
            soundHelper.preloadBarcodeScanFailure()
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundHelper.onCleanup()
    }

    fun onUIEvent(event: WooPosCartUIEvent) {
        when (event) {
            is WooPosCartUIEvent.CheckoutClicked -> {
                goToTotals()
            }

            is WooPosCartUIEvent.ItemRemovedFromCart -> {
                removeItemsFromCart(setOf(event.item), WooPosAnalyticsEventConstant.CartSource.CART)
            }

            is WooPosCartUIEvent.EditCustomAmountClicked -> {
                sendEventToParent(ChildToParentEvent.CustomAmountDialogRequested(editing = event.item))
            }

            WooPosCartUIEvent.BackClicked -> {
                val currentState = _state.value
                if (currentState.cartStatus == EDITABLE) {
                    return
                }
                _state.value = currentState.copy(cartStatus = EDITABLE)

                sendEventToParent(ChildToParentEvent.BackFromCheckoutToCartClicked)
                viewModelScope.launch {
                    analyticsTracker.track(BackToCartTapped)
                }
            }

            WooPosCartUIEvent.ClearAllClicked -> {
                viewModelScope.launch { analyticsTracker.track(ClearCartTapped) }
                val currentState = _state.value
                _state.value = currentState.copy(
                    body = WooPosCartState.Body.Empty
                )
            }

            is WooPosCartUIEvent.OnBarcodeEvent -> {
                onBarcodeEvent(event.result)
            }

            WooPosCartUIEvent.BarcodeSetupClicked -> {
                viewModelScope.launch {
                    analyticsTracker.track(EmptyCartSetUpScannerTapped)
                }
                sendEventToParent(ChildToParentEvent.SetupBarcodeScannerClicked)
            }
        }
    }

    private fun removeItemsFromCart(
        items: Set<WooPosCartItemViewState>,
        source: WooPosAnalyticsEventConstant.CartSource
    ) {
        val currentState = _state.value
        _state.value = if (currentState.body.amountOfItems == items.size) {
            currentState.copy(body = WooPosCartState.Body.Empty)
        } else {
            currentState.copy(
                body = (currentState.body as WooPosCartState.Body.WithItems)
                    .copy(itemsInCart = currentState.body.itemsInCart - items)
            )
        }
        viewModelScope.launch {
            items.forEach { item ->
                analyticsTracker.track(ItemRemovedFromCart(item = item, source = source))
            }
        }
    }

    private fun goToTotals() {
        val itemClickedDataList = getCartItemsDataList()
        sendEventToParent(ChildToParentEvent.CheckoutClicked(itemClickedDataList))
        _state.value = _state.value.copy(cartStatus = CHECKOUT)
        trackCheckoutTapped(
            itemClickedDataList.filterIsInstance<WooPosItemsViewModel.ItemClickedData.Product>().size,
            itemClickedDataList.filterIsInstance<WooPosItemsViewModel.ItemClickedData.Coupon>().size
        )
    }

    private fun getCartItemsDataList(): List<WooPosItemsViewModel.ItemClickedData> {
        val itemClickedDataList = (_state.value.body as? WooPosCartState.Body.WithItems)?.itemsInCart?.mapNotNull {
            when (it) {
                is Product.Simple -> WooPosItemsViewModel.ItemClickedData.Product.Simple(it.id)
                is Product.Variation -> WooPosItemsViewModel.ItemClickedData.Product.Variation(
                    productId = it.id,
                    id = it.variationId
                )

                is WooPosCartItemViewState.Coupon -> WooPosItemsViewModel.ItemClickedData.Coupon(it.id, it.name)
                is WooPosCartItemViewState.CustomAmount -> WooPosItemsViewModel.ItemClickedData.CustomAmount(
                    id = it.itemNumber.toLong(),
                    name = it.name,
                    amount = it.amount,
                    isTaxable = it.isTaxable,
                )
                is WooPosCartItemViewState.Loading -> null
                is WooPosCartItemViewState.Error -> null
            }
        } ?: emptyList()
        return itemClickedDataList
    }

    private fun trackCheckoutTapped(productsInCart: Int, couponsInCart: Int) {
        viewModelScope.launch {
            analyticsTracker.track(CheckoutTapped(productsInCart, couponsInCart))
        }
    }

    private fun listenEventsFromParent() {
        viewModelScope.launch {
            parentToChildrenEventReceiver.events.collect { event ->
                when (event) {
                    is ParentToChildrenEvent.BackFromCheckoutToCartClicked -> handleBackFromCheckoutToCartClicked()

                    is ParentToChildrenEvent.ItemClickedInItemsList -> handleItemClickedInItemsSelector(event)

                    is ParentToChildrenEvent.OrderSuccessfullyPaid -> clearCart()

                    is ParentToChildrenEvent.OrderCreated -> onOrderCreated(event)

                    is ParentToChildrenEvent.SearchEvent.RecentSearchSelected,
                    is ParentToChildrenEvent.CheckoutClicked,
                    is ParentToChildrenEvent.SearchEvent.ChangedQuery,
                    ParentToChildrenEvent.SearchEvent.Finished,
                    ParentToChildrenEvent.SearchEvent.Started,
                    ParentToChildrenEvent.RefreshProductList,
                    is ParentToChildrenEvent.CouponsRemoved,
                    is ParentToChildrenEvent.SettingsEvent -> Unit

                    is ParentToChildrenEvent.CouponsValidationFailed -> {
                        onCouponsValidationFails()
                    }

                    is ParentToChildrenEvent.MissingVariationEvent -> {
                        markVariationAsUnknown(event.variationId)
                    }

                    is ParentToChildrenEvent.RemoveCouponsClicked -> {
                        removeCouponsFromCart()
                    }

                    is ParentToChildrenEvent.RemoveProductsClicked -> {
                        removeNotFoundProductsFromCart(event)
                    }

                    is ParentToChildrenEvent.BarcodeEvent -> {
                        onBarcodeEvent(event.result)
                    }

                    is ParentToChildrenEvent.ProductsRemoved -> Unit

                    is ParentToChildrenEvent.CustomAmountSubmitted -> handleCustomAmountSubmitted(event)

                    is ParentToChildrenEvent.ShowCustomAmountForm -> Unit
                }
            }
        }
    }

    private suspend fun handleCustomAmountSubmitted(event: ParentToChildrenEvent.CustomAmountSubmitted) {
        val result = customAmountCartHandler.applySubmittedToCart(
            currentBody = _state.value.body,
            event = event,
            nextItemNumber = { getItemNumber() },
        )
        when (result) {
            is WooPosCustomAmountCartHandler.SubmittedResult.Edited -> {
                val currentBody = _state.value.body as? WooPosCartState.Body.WithItems ?: return
                _state.value = _state.value.copy(body = currentBody.copy(itemsInCart = result.updatedItems))
                analyticsTracker.track(
                    CustomAmountSubmitted(CustomAmountSubmitted.Mode.EDIT, event.isTaxable)
                )
            }

            is WooPosCustomAmountCartHandler.SubmittedResult.Added -> {
                handleNewTransactionIfNeeded()
                updateCartItem(result.newItem)
                analyticsTracker.track(
                    CustomAmountSubmitted(CustomAmountSubmitted.Mode.ADD, event.isTaxable)
                )
                analyticsTracker.track(WooPosAnalyticsEvent.Event.ItemAddedToCart(item = result.newItem))
            }
        }
    }

    private suspend fun onOrderCreated(event: ParentToChildrenEvent.OrderCreated) {
        val body = _state.value.body as? WooPosCartState.Body.WithItems ?: return
        val result = updateCartItemsWithChanges(
            itemsInCart = body.itemsInCart,
            updatedProducts = event.data.updatedProducts,
            updatedCoupons = event.data.updatedCoupons,
        )
        if (result.productsChanged || result.couponsChanged) {
            _state.value = _state.value.copy(
                body = body.copy(
                    itemsInCart = result.updatedItems,
                ),
            )
        }
        if (result.productsChanged) {
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.ToastMessageDisplayed(
                    message = resourceProvider.getString(R.string.woopos_cart_changes_in_the_cart)
                )
            )
            childrenToParentEventSender.sendToParent(
                ChildToParentEvent.RefreshProductList
            )
        }
    }

    private fun markVariationAsUnknown(variationId: Long) {
        val body = _state.value.body as? WooPosCartState.Body.WithItems ?: return
        _state.value = _state.value.copy(
            body = body.copy(
                itemsInCart = body.itemsInCart.map {
                    if ((it as? Product.Variation)?.variationId == variationId) {
                        it.copy(productDoesNotExist = true)
                    } else { it }
                },
            ),
        )
    }

    private fun onCouponsValidationFails() {
        val currentState = _state.value
        val body = currentState.body as? WooPosCartState.Body.WithItems ?: return

        val updatedItems = body.itemsInCart.map { item ->
            if (item is WooPosCartItemViewState.Coupon) {
                item.copy(validationState = CouponValidationState.Invalid)
            } else {
                item
            }
        }

        _state.value = currentState.copy(
            body = body.copy(itemsInCart = updatedItems)
        )
    }

    private fun removeCouponsFromCart() {
        val cartBody = _state.value.body as? WooPosCartState.Body.WithItems
        cartBody?.itemsInCart
            ?.filterIsInstance<WooPosCartItemViewState.Coupon>()
            ?.toSet()?.let { couponsToRemove ->
                removeItemsFromCart(couponsToRemove, WooPosAnalyticsEventConstant.CartSource.ERROR)
            }
        sendEventToParent(CouponsRemoved(getCartItemsDataList()))
    }

    private fun removeNotFoundProductsFromCart(event: ParentToChildrenEvent.RemoveProductsClicked) {
        val cartBody = _state.value.body as? WooPosCartState.Body.WithItems ?: return

        val itemsToRemove = cartBody.itemsInCart
            .filterIsInstance<Product>()
            .filter { product ->
                when (product) {
                    is Product.Simple -> product.id in event.productIdsToRemove
                    is Product.Variation -> product.variationId in event.productIdsToRemove
                }
            }
            .toSet()

        removeItemsFromCart(itemsToRemove, WooPosAnalyticsEventConstant.CartSource.ERROR)

        sendEventToParent(ChildToParentEvent.ProductsRemoved(getCartItemsDataList()))
    }

    private fun handleBackFromCheckoutToCartClicked() {
        val currentState = _state.value
        val newCartStatus = EDITABLE
        when (val body = currentState.body) {
            is WooPosCartState.Body.WithItems -> {
                _state.value = currentState.copy(
                    cartStatus = newCartStatus,
                    body = body.copy(itemsInCart = removeFormattedDiscountFromCoupons(body))
                )
            }

            else -> _state.value = currentState.copy(cartStatus = newCartStatus)
        }
    }

    private fun handleItemClickedInItemsSelector(event: ParentToChildrenEvent.ItemClickedInItemsList) {
        viewModelScope.launch {
            val itemClicked = async {
                when (event.itemData) {
                    is WooPosItemsViewModel.ItemClickedData.Product.Simple ->
                        handleSimpleProductClicked(event.itemData.id)

                    is WooPosItemsViewModel.ItemClickedData.Product.Variation ->
                        handleVariationClicked(event.itemData.productId, event.itemData.id)

                    is WooPosItemsViewModel.ItemClickedData.Coupon ->
                        handleCouponClicked(event.itemData.id)

                    is WooPosItemsViewModel.ItemClickedData.VariableProduct -> null

                    is WooPosItemsViewModel.ItemClickedData.CustomAmount -> null
                }
            }

            if (_state.value.body == WooPosCartState.Body.Empty) {
                analyticsTracker.track(InteractionWithCustomerStarted)
            }

            itemClicked.await()?.let {
                updateCartItem(it)
            }
            event.eventForTracking?.let {
                analyticsTracker.track(it)
            }
        }
    }

    private suspend fun handleSimpleProductClicked(productId: Long): WooPosCartItemViewState {
        val product = getProductById(productId) ?: error("Product not found: $productId")

        val itemNumber = getItemNumber()
        return product.toCartListItem(itemNumber)
    }

    private suspend fun handleVariationClicked(productId: Long, variationId: Long): WooPosCartItemViewState {
        val product = getProductById(productId) ?: error("Product not found: $productId")
        val itemNumber = getItemNumber()
        val productVariation = getVariationsById(productId, variationId) ?: error("Variation not found: $variationId")
        return productVariation.toCartListItem(itemNumber, product)
    }

    private suspend fun handleCouponClicked(couponId: Long): WooPosCartItemViewState {
        val coupon = getCouponById(couponId)!!
        return WooPosCartItemViewState.Coupon(
            itemNumber = getItemNumber(),
            id = couponId,
            name = coupon.code ?: "",
            summary = couponFormatter.formatSummary(coupon, getCachedStoreCurrency()),
            validationState = CouponValidationState.Unknown
        )
    }

    private fun clearCart() {
        _state.value = WooPosCartState()
    }

    private suspend fun handleNewTransactionIfNeeded() {
        if (_state.value.body == WooPosCartState.Body.Empty) {
            childrenToParentEventSender.sendToParent(OnNewTransactionStarted)
            analyticsTracker.track(InteractionWithCustomerStarted)
        }
    }

    private fun onBarcodeEvent(result: BarcodeInputDetector.BarcodeResult) {
        if (_state.value.cartStatus !in listOf(EDITABLE, EMPTY)) {
            return
        }

        viewModelScope.launch {
            barcodeEventTracker.trackBarcodeEvent(result)

            when (result) {
                is BarcodeInputDetector.BarcodeResult.Success -> {
                    processBarcodeSuccess(result.barcode)
                }

                is BarcodeInputDetector.BarcodeResult.Error -> {
                    processBarcodeError(result)
                }
            }
        }
    }

    private suspend fun processBarcodeSuccess(barcode: String) {
        handleNewTransactionIfNeeded()
        val itemNumber = getItemNumber()

        WooPosCartItemViewState.Loading(itemNumber = itemNumber, name = barcode).let { loadingItem ->
            analyticsTracker.track(WooPosAnalyticsEvent.Event.ItemAddedToCart(item = loadingItem))
            updateCartItem(loadingItem)
        }

        val searchResult = searchByIdentifier(barcode)

        if (!isItemStillInCart(itemNumber)) {
            wooPosLogWrapper.w("Item no longer in the cart. Ignoring barcode scan result.")
            return
        }

        val cartItem = searchResult.mapToCartItem(identifier = barcode, itemNumber = itemNumber)
        if (cartItem is WooPosCartItemViewState.Error) {
            soundHelper.playBarcodeScanFailure()
        }

        analyticsTracker.track(WooPosAnalyticsEvent.Event.ItemAddedToCart(item = cartItem))
        updateCartItem(cartItem)
    }

    private suspend fun processBarcodeError(result: BarcodeInputDetector.BarcodeResult.Error) {
        handleNewTransactionIfNeeded()
        val itemNumber = getItemNumber()
        val errorMessage = when (result.failureReason) {
            BarcodeInputDetector.FailureReason.TOO_SHORT -> {
                resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_too_short)
            }

            BarcodeInputDetector.FailureReason.NO_TERMINATOR -> {
                resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_no_terminator)
            }
        }

        val errorItem = WooPosCartItemViewState.Error(
            itemNumber = itemNumber,
            name = result.barcode,
            message = errorMessage
        )

        soundHelper.playBarcodeScanFailure()
        analyticsTracker.track(WooPosAnalyticsEvent.Event.ItemAddedToCart(item = errorItem))
        updateCartItem(errorItem)
    }

    private fun isItemStillInCart(itemNumber: Int): Boolean {
        val body = _state.value.body
        return when (body) {
            is WooPosCartState.Body.Empty -> false
            is WooPosCartState.Body.WithItems -> body.itemsInCart.any { it.itemNumber == itemNumber }
        }
    }

    private fun getItemNumber(): Int {
        return when (_state.value.body) {
            is WooPosCartState.Body.Empty -> 1
            is WooPosCartState.Body.WithItems -> itemNumberProvider.incrementAndGet()
        }
    }

    private fun updateCartItem(newItem: WooPosCartItemViewState) {
        val currentState = _state.value

        _state.value = when (val body = currentState.body) {
            is WooPosCartState.Body.Empty -> {
                currentState.copy(body = WooPosCartState.Body.WithItems(listOf(newItem)))
            }

            is WooPosCartState.Body.WithItems -> {
                val existingItemIndex = body.itemsInCart.indexOfFirst { it.itemNumber == newItem.itemNumber }

                val updatedItemsList = if (existingItemIndex != -1) {
                    body.itemsInCart.mapIndexed { index, item ->
                        if (index == existingItemIndex) newItem else item
                    }
                } else {
                    placeItemInList(body, newItem)
                }

                currentState.copy(body = body.copy(itemsInCart = updatedItemsList))
            }
        }
    }

    private fun placeItemInList(
        currentState: WooPosCartState.Body.WithItems,
        newItem: WooPosCartItemViewState
    ): List<WooPosCartItemViewState> {
        val existingCoupons = currentState.itemsInCart.filterIsInstance<WooPosCartItemViewState.Coupon>()
        val existingCustomAmounts = currentState.itemsInCart.filterIsInstance<WooPosCartItemViewState.CustomAmount>()
        val existingOther = currentState.itemsInCart.filterNot {
            it is WooPosCartItemViewState.Coupon || it is WooPosCartItemViewState.CustomAmount
        }

        return when (newItem) {
            is WooPosCartItemViewState.Coupon -> {
                val couponAlreadyInCart = existingCoupons.any { it.id == newItem.id }
                if (couponAlreadyInCart) {
                    currentState.itemsInCart
                } else {
                    listOf(newItem) + existingCoupons + existingCustomAmounts + existingOther
                }
            }

            is WooPosCartItemViewState.CustomAmount -> {
                existingCoupons + listOf(newItem) + existingCustomAmounts + existingOther
            }

            else -> existingCoupons + existingCustomAmounts + listOf(newItem) + existingOther
        }
    }

    private fun updateToolbarState(newState: WooPosCartState): WooPosCartState {
        val itemsCount = resourceProvider.getQuantityString(
            newState.body.amountOfItems,
            default = R.string.woopos_items_in_cart_multiple,
            zero = R.string.woopos_items_in_cart_multiple,
            one = R.string.woopos_items_in_cart,
        )
        val newToolbar = when (newState.cartStatus) {
            EDITABLE -> {
                WooPosCartState.Toolbar(
                    backIconVisible = false,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = newState.body is WooPosCartState.Body.WithItems
                )
            }

            CHECKOUT -> {
                WooPosCartState.Toolbar(
                    backIconVisible = true,
                    itemsCount = itemsCount,
                    isClearAllButtonVisible = false
                )
            }

            EMPTY -> {
                WooPosCartState.Toolbar(
                    backIconVisible = false,
                    itemsCount = null,
                    isClearAllButtonVisible = false
                )
            }
        }
        return newState.copy(toolbar = newToolbar)
    }

    private fun updateStateDependingOnCartStatus(newState: WooPosCartState) =
        when (newState.cartStatus) {
            EDITABLE -> {
                val checkoutButtonState = when {
                    newState.body !is WooPosCartState.Body.WithItems -> WooPosCartState.CheckoutButtonState.Invisible
                    cartContainsLoadingOrErrorItems(newState.body) -> WooPosCartState.CheckoutButtonState.Disabled
                    cartContainsPurchasableItems(newState.body) -> WooPosCartState.CheckoutButtonState.Enabled
                    else -> WooPosCartState.CheckoutButtonState.Invisible
                }
                newState.copy(
                    areItemsRemovable = true,
                    checkoutButtonState = checkoutButtonState,
                )
            }

            CHECKOUT, EMPTY -> {
                newState.copy(
                    areItemsRemovable = false,
                    checkoutButtonState = WooPosCartState.CheckoutButtonState.Invisible,
                )
            }
        }

    private fun updateCartStatusDependingOnItems(newState: WooPosCartState): WooPosCartState =
        when (newState.body) {
            is WooPosCartState.Body.Empty -> newState.copy(cartStatus = EMPTY)
            is WooPosCartState.Body.WithItems -> newState
        }

    private fun sendEventToParent(event: ChildToParentEvent) {
        viewModelScope.launch {
            childrenToParentEventSender.sendToParent(event)
        }
    }

    private fun updateAnalyticsData(newState: WooPosCartState) {
        if (newState.body == WooPosCartState.Body.Empty) {
            analyticsTrackingDataKeeper.reset()
            analyticsTrackingDataKeeper.interactionWithCustomerStartedTimestamp = System.currentTimeMillis()
        }
    }

    private fun removeFormattedDiscountFromCoupons(body: WooPosCartState.Body.WithItems) = body.itemsInCart
        .map { item ->
            when (item) {
                is WooPosCartItemViewState.Coupon -> item.copy(validationState = CouponValidationState.Unknown)
                is Product -> item
                is WooPosCartItemViewState.CustomAmount -> item
                is WooPosCartItemViewState.Error -> item
                is WooPosCartItemViewState.Loading -> item
            }
        }

    private suspend fun WooPosProductModel.toCartListItem(itemNumber: Int): Product.Simple = Product.Simple(
        itemNumber = itemNumber,
        id = this.remoteId,
        name = name,
        description = null,
        price = formatPrice(pricing.displayPrice),
        imageUrl = firstImageUrl,
    )

    private suspend fun WooPosVariation.toCartListItem(
        itemNumber: Int,
        product: WooPosProductModel
    ): Product.Variation =
        Product.Variation(
            itemNumber = itemNumber,
            id = product.remoteId,
            variationId = this.remoteVariationId,
            name = product.name,
            description = getNameForPOS(variationMapper, product, resourceProvider),
            price = formatPrice(price),
            imageUrl = image?.source,
        )

    private fun getInitialValueOrHighestUsedItemNumberAfterProcessDeath() =
        (_state.value.body as? WooPosCartState.Body.WithItems)?.itemsInCart?.maxOfOrNull { it.itemNumber } ?: 1

    private fun cartContainsPurchasableItems(body: WooPosCartState.Body.WithItems) =
        body.itemsInCart.any { it is Product || it is WooPosCartItemViewState.CustomAmount }

    private fun cartContainsLoadingOrErrorItems(body: WooPosCartState.Body.WithItems) =
        body.itemsInCart.any { it is WooPosCartItemViewState.Loading || it is WooPosCartItemViewState.Error }

    private suspend fun WooPosSearchByIdentifierResult.mapToCartItem(
        identifier: String,
        itemNumber: Int,
    ): WooPosCartItemViewState {
        return when (this) {
            is WooPosSearchByIdentifierResult.Success -> {
                val product = this.product
                Product.Simple(
                    itemNumber = itemNumber,
                    id = product.remoteId,
                    name = product.name,
                    description = null,
                    price = formatPrice(product.pricing.displayPrice),
                    imageUrl = product.firstImageUrl
                )
            }

            is WooPosSearchByIdentifierResult.VariationSuccess -> {
                Product.Variation(
                    itemNumber = itemNumber,
                    id = variation.remoteProductId,
                    variationId = variation.remoteVariationId,
                    name = this.parentProduct.name,
                    description = variation.getNameForPOS(variationMapper, this.parentProduct, resourceProvider),
                    price = formatPrice(variation.price),
                    imageUrl = variation.image?.source
                )
            }

            is WooPosSearchByIdentifierResult.Failure -> {
                val errorMessage = when (this.error) {
                    WooPosSearchByIdentifierResult.Error.NotFound -> {
                        resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_product_not_found)
                    }

                    WooPosSearchByIdentifierResult.Error.NetworkError -> {
                        resourceProvider.getString(R.string.woopos_cart_barcode_scan_result_network_error)
                    }

                    is WooPosSearchByIdentifierResult.Error.UnknownError -> this.error.message
                    is WooPosSearchByIdentifierResult.Error.UnsupportedProduct -> {
                        resourceProvider.getString(
                            R.string.woopos_cart_barcode_scan_result_unsupported_product,
                            this.error.productName
                        )
                    }

                    is WooPosSearchByIdentifierResult.Error.ServerError -> {
                        resourceProvider.getString(
                            R.string.woopos_cart_barcode_scan_result_server_error,
                            this.error.message
                        )
                    }
                }
                WooPosCartItemViewState.Error(
                    itemNumber = itemNumber,
                    name = identifier,
                    message = errorMessage
                )
            }
        }
    }
}
