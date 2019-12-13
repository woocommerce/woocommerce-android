package com.woocommerce.android.ui.refunds

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_PRODUCT_AMOUNT_DIALOG_OPENED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_TAB_CHANGED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.REFUND_CREATE
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.REFUND_CREATE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.REFUND_CREATE_SUCCESS
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.util.max
import com.woocommerce.android.ui.orders.notes.OrderNoteRepository
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.HideValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundAmountDialog
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType.AMOUNT
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType.ITEMS
import com.woocommerce.android.ui.refunds.RefundProductListAdapter.RefundListItem
import com.woocommerce.android.util.min
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.min

@OpenClassOnDebug
class IssueRefundViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    currencyFormatter: CurrencyFormatter,
    private val orderStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val noteRepository: OrderNoteRepository,
    private val gatewayStore: WCGatewayStore,
    private val refundStore: WCRefundStore
) : ScopedViewModel(savedState, dispatchers) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val REFUND_METHOD_MANUAL = "manual"
        private const val SELECTED_QUANTITIES_KEY = "selected_quantities_key"
    }

    private val _refundItems = MutableLiveData<List<RefundListItem>>()
    final val refundItems: LiveData<List<RefundListItem>> = _refundItems

    private val areAllItemsSelected: Boolean
        get() = refundItems.value?.all { it.quantity == it.maxQuantity } ?: false

    final val commonStateLiveData = LiveDataDelegate(savedState, CommonViewState())
    final val refundSummaryStateLiveData = LiveDataDelegate(savedState, RefundSummaryViewState())
    final val refundByItemsStateLiveData = LiveDataDelegate(savedState, RefundByItemsViewState(), onChange = {
        updateRefundTotal(it.productsRefund)
    })
    final val refundByAmountStateLiveData = LiveDataDelegate(savedState, RefundByAmountViewState(), onChange = {
        updateRefundTotal(it.enteredAmount)
    })
    final val productsRefundLiveData = LiveDataDelegate(savedState, ProductsRefundViewState())

    private var commonState by commonStateLiveData
    private var refundByAmountState by refundByAmountStateLiveData
    private var refundByItemsState by refundByItemsStateLiveData
    private var refundSummaryState by refundSummaryStateLiveData
    private var productsRefundState by productsRefundLiveData
    private var refundContinuation: Continuation<Boolean>? = null

    private val order: Order
    private val refunds: List<Refund>

    private val maxRefund: BigDecimal
    private val maxQuantities: Map<Long, Int>
    private val formatCurrency: (BigDecimal) -> String
    private val gateway: PaymentGateway
    private val arguments: RefundsArgs by savedState.navArgs()

    private val selectedQuantities: MutableMap<Long, Int> by lazy {
        val quantities = savedState.get<MutableMap<Long, Int>>(SELECTED_QUANTITIES_KEY) ?: mutableMapOf()
        savedState[SELECTED_QUANTITIES_KEY] = quantities
        quantities
    }

    init {
        order = loadOrder(arguments.orderId)
        refunds = refundStore.getAllRefunds(selectedSite.get(), arguments.orderId).map { it.toAppModel() }

        formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
        maxRefund = order.total - order.refundTotal
        maxQuantities = getMaxQuantities()
        gateway = loadPaymentGateway()

        initRefundByAmountState()
        initRefundByItemsState()
        initRefundSummaryState()
    }

    private fun loadOrder(orderId: Long): Order =
        requireNotNull(orderStore.getOrderByIdentifier(OrderIdentifier(selectedSite.get().id, orderId))?.toAppModel())

    private fun updateRefundTotal(amount: BigDecimal) {
        commonState = commonState.copy(
                refundTotal = amount,
                screenTitle = resourceProvider.getString(
                        R.string.order_refunds_title_with_amount, formatCurrency(amount)
                )
        )
    }

    private fun initRefundByAmountState() {
        if (refundByAmountStateLiveData.hasInitialValue) {
            val decimals = wooStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
                    ?: DEFAULT_DECIMAL_PRECISION

            refundByAmountState = refundByAmountState.copy(
                    currency = order.currency,
                    decimals = decimals,
                    availableForRefund = resourceProvider.getString(
                            R.string.order_refunds_available_for_refund,
                            formatCurrency(maxRefund)
                    )
            )
        }
    }

    private fun initRefundByItemsState() {
        if (refundByItemsStateLiveData.hasInitialValue) {
            refundByItemsState = refundByItemsState.copy(
                    currency = order.currency,
                    subtotal = formatCurrency(BigDecimal.ZERO),
                    taxes = formatCurrency(BigDecimal.ZERO),
                    formattedProductsRefund = formatCurrency(BigDecimal.ZERO),
                    isShippingRefundVisible = false
            )
        }

        val items = order.items.map {
            val maxQuantity = maxQuantities[it.productId] ?: 0
            val selectedQuantity = min(selectedQuantities[it.productId] ?: 0, maxQuantity)
            RefundListItem(it, maxQuantity, selectedQuantity)
        }
        updateRefundItems(items)

        if (productsRefundLiveData.hasInitialValue) {
            val decimals = wooStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
                    ?: DEFAULT_DECIMAL_PRECISION

            productsRefundState = productsRefundState.copy(
                    currency = order.currency,
                    decimals = decimals
            )
        }
    }

    private fun initRefundSummaryState() {
        if (refundSummaryStateLiveData.hasInitialValue) {
            val manualRefundMethod = resourceProvider.getString(R.string.order_refunds_manual_refund)
            val paymentTitle: String
            val isManualRefund: Boolean
            if (gateway.isEnabled) {
                paymentTitle = if (gateway.supportsRefunds)
                    gateway.title
                else
                    "$manualRefundMethod via ${gateway.title}"
                isManualRefund = !gateway.supportsRefunds
            } else {
                paymentTitle = gateway.title
                isManualRefund = true
            }

            refundSummaryState = refundSummaryState.copy(
                    refundMethod = paymentTitle,
                    isMethodDescriptionVisible = isManualRefund
            )
        }
    }

    private fun loadPaymentGateway(): PaymentGateway {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        return if (paymentGateway != null && paymentGateway.isEnabled) {
            paymentGateway
        } else {
            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    fun onNextButtonTappedFromItems() {
        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
                mapOf(
                        AnalyticsTracker.KEY_REFUND_TYPE to ITEMS.name,
                        AnalyticsTracker.KEY_ORDER_ID to order.remoteId
                )
        )

        showRefundSummary()
    }

    fun onNextButtonTappedFromAmounts() {
        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
                mapOf(
                        AnalyticsTracker.KEY_REFUND_TYPE to AMOUNT.name,
                        AnalyticsTracker.KEY_ORDER_ID to order.remoteId
                )
        )

        if (isInputValid()) {
            showRefundSummary()
        } else {
            showValidationState()
        }
    }

    fun onRefundItemsShippingSwitchChanged(isChecked: Boolean) {
        refundByItemsState = if (isChecked) {
            refundByItemsState.copy(isShippingRefundVisible = true)
        } else {
            refundByItemsState.copy(isShippingRefundVisible = false)
        }
    }

    private fun showRefundSummary() {
        refundSummaryState = refundSummaryState.copy(
                isFormEnabled = true,
                previouslyRefunded = formatCurrency(order.refundTotal),
                refundAmount = formatCurrency(commonState.refundTotal)
        )

        triggerEvent(ShowRefundSummary(commonState.refundType))
    }

    fun onManualRefundAmountChanged(amount: BigDecimal) {
        if (refundByAmountState.enteredAmount != amount) {
            refundByAmountState = refundByAmountState.copy(enteredAmount = amount)
            showValidationState()
        }
    }

    fun onRefundConfirmed(reason: String) {
        AnalyticsTracker.track(CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED, mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.remoteId
        ))

        if (networkStatus.isConnected()) {
            triggerEvent(
                    ShowSnackbar(
                            R.string.order_refunds_amount_refund_progress_message,
                            arrayOf(formatCurrency(commonState.refundTotal)),
                            undoAction = {
                                AnalyticsTracker.track(
                                        CREATE_ORDER_REFUND_SUMMARY_UNDO_BUTTON_TAPPED,
                                        mapOf(AnalyticsTracker.KEY_ORDER_ID to order.remoteId)
                                )
                                refundContinuation?.resume(true)
                            })
            )

            launch {
                refundSummaryState = refundSummaryState.copy(
                        isFormEnabled = false,
                        refundReason = reason
                )

                // pause here until the snackbar is dismissed to allow for undo action
                val wasRefundCanceled = waitForCancellation()
                if (!wasRefundCanceled) {
                    triggerEvent(ShowSnackbar(
                            R.string.order_refunds_amount_refund_confirmation_message,
                            arrayOf(formatCurrency(commonState.refundTotal))
                        )
                    )

                    AnalyticsTracker.track(REFUND_CREATE, mapOf(
                            AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                            AnalyticsTracker.KEY_REFUND_IS_FULL to
                                    (commonState.refundTotal isEqualTo maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_TYPE to commonState.refundType.name,
                            AnalyticsTracker.KEY_REFUND_METHOD to gateway.methodTitle,
                            AnalyticsTracker.KEY_REFUND_AMOUNT to commonState.refundTotal.toString()
                    ))

                    val resultCall = async(dispatchers.io) {
                        return@async when (commonState.refundType) {
                            ITEMS -> {
                                refundStore.createItemsRefund(
                                        selectedSite.get(),
                                        order.remoteId,
                                        reason,
                                        true,
                                        gateway.supportsRefunds,
                                        refundItems.value?.map { it.toDataModel() } ?: emptyList()
                                )
                            }
                            AMOUNT -> {
                                refundStore.createAmountRefund(
                                        selectedSite.get(),
                                        order.remoteId,
                                        commonState.refundTotal,
                                        reason,
                                        gateway.supportsRefunds
                                )
                            }
                        }
                    }

                    val result = resultCall.await()
                    if (result.isError) {
                        AnalyticsTracker.track(
                                REFUND_CREATE_FAILED, mapOf(
                                AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                                AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.toString(),
                                AnalyticsTracker.KEY_ERROR_DESC to result.error.message)
                        )

                        triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_error))
                    } else {
                        AnalyticsTracker.track(
                                REFUND_CREATE_SUCCESS, mapOf(
                                AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                                AnalyticsTracker.KEY_ID to result.model?.id
                        ))

                        if (reason.isNotBlank()) {
                            noteRepository.createOrderNote(order.identifier, reason, false)
                        }

                        triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_successful))
                        triggerEvent(Exit)
                    }
                }
                refundSummaryState = refundSummaryState.copy(isFormEnabled = true)
            }
        } else {
            triggerEvent(ShowSnackbar(R.string.offline_error))
        }
    }

    fun onProceedWithRefund() {
        refundContinuation?.resume(false)
    }

    fun onRefundQuantityTapped(productId: Long) {
        _refundItems.value?.firstOrNull { it.product.productId == productId }?.let {
            triggerEvent(ShowNumberPicker(it))
        }

        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to order.remoteId)
        )
    }

    // will be used in the future
    fun onProductRefundAmountTapped() {
        triggerEvent(ShowRefundAmountDialog(
                refundByItemsState.productsRefund,
                maxRefund,
                resourceProvider.getString(R.string.order_refunds_available_for_refund, formatCurrency(maxRefund))
        ))

        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_PRODUCT_AMOUNT_DIALOG_OPENED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to order.remoteId)
        )
    }

    fun onProductsRefundAmountChanged(newAmount: BigDecimal) {
        refundByItemsState = refundByItemsState.copy(
                productsRefund = newAmount,
                formattedProductsRefund = formatCurrency(newAmount),
                isNextButtonEnabled = newAmount > BigDecimal.ZERO
        )
    }

    fun onRefundQuantityChanged(productId: Long, newQuantity: Int) {
        val newItems = getUpdatedItemList(productId, newQuantity)
        updateRefundItems(newItems)

        selectedQuantities[productId] = newQuantity

        val (taxes, subtotal) = calculateTotals(newItems)
        val productsRefund = min(max(subtotal + taxes, BigDecimal.ZERO), maxRefund)

        val selectButtonTitle = if (areAllItemsSelected)
                resourceProvider.getString(R.string.order_refunds_items_select_none)
            else
                resourceProvider.getString(R.string.order_refunds_items_select_all)

        refundByItemsState = refundByItemsState.copy(
                productsRefund = productsRefund,
                formattedProductsRefund = formatCurrency(productsRefund),
                taxes = formatCurrency(taxes),
                subtotal = formatCurrency(subtotal),
                isNextButtonEnabled = productsRefund > BigDecimal.ZERO,
                selectButtonTitle = selectButtonTitle
        )
    }

    private fun calculateTotals(newItems: MutableList<RefundListItem>): Pair<BigDecimal, BigDecimal> {
        var taxes = BigDecimal.ZERO
        var subtotal = BigDecimal.ZERO
        newItems.forEach { item ->
            val quantity = item.quantity.toBigDecimal()
            subtotal += quantity.times(item.product.price)

            val singleItemTax = item.product.totalTax.divide(
                    item.product.quantity.toBigDecimal(),
                    HALF_UP
            )
            taxes += quantity.times(singleItemTax)
        }
        return Pair(taxes, subtotal)
    }

    private fun getUpdatedItemList(productId: Long, newQuantity: Int): MutableList<RefundListItem> {
        val newItems = mutableListOf<RefundListItem>()
        _refundItems.value?.forEach {
            if (it.product.productId == productId) {
                newItems.add(
                        it.copy(
                                quantity = newQuantity,
                                maxQuantity = maxQuantities[productId] ?: 0
                        )
                )
            } else {
                newItems.add(it)
            }
        }
        return newItems
    }

    fun onSelectButtonTapped() {
        if (areAllItemsSelected) {
            _refundItems.value?.forEach {
                onRefundQuantityChanged(it.product.productId, 0)
            }
        } else {
            _refundItems.value?.forEach {
                onRefundQuantityChanged(it.product.productId, it.maxQuantity)
            }
        }

        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to order.remoteId)
        )
    }

    // TODO: Temporarily unused; it will be used again in a future release - do not remove
    @Suppress("unused")
    fun onRefundTabChanged(type: RefundType) {
        val refundAmount = when (type) {
            ITEMS -> refundByItemsState.totalRefund
            AMOUNT -> refundByAmountState.enteredAmount
        }
        commonState = commonState.copy(refundType = type)
        updateRefundTotal(refundAmount)

        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_TAB_CHANGED,
                mapOf(
                        AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                        AnalyticsTracker.KEY_TYPE to type.name
                )
        )
    }

    private fun updateRefundItems(items: List<RefundListItem>) {
        _refundItems.value = items.filter { it.maxQuantity > 0 }

        val selectedItems = items.sumBy { it.quantity }
        refundByItemsState = refundByItemsState.copy(
                selectedItemsHeader = resourceProvider.getString(
                    R.string.order_refunds_items_selected,
                    selectedItems
                )
        )
    }

    // calculate the max quantity for each item by subtracting the number of already-refunded items
    private fun getMaxQuantities(): Map<Long, Int> {
        val map = mutableMapOf<Long, Int>()
        val groupedRefunds = refunds.flatMap { it.items }.groupBy { it.productId }
        order.items.map { item ->
            map[item.productId] = item.quantity - (groupedRefunds[item.productId]?.sumBy { it.quantity } ?: 0)
        }
        return map
    }

    private suspend fun waitForCancellation(): Boolean {
        val wasRefundCanceled = suspendCoroutine<Boolean> {
            refundContinuation = it
        }
        refundContinuation = null
        return wasRefundCanceled
    }

    private fun validateInput(): InputValidationState {
        return when {
            refundByAmountState.enteredAmount > maxRefund -> return TOO_HIGH
            refundByAmountState.enteredAmount isEqualTo BigDecimal.ZERO -> TOO_LOW
            else -> VALID
        }
    }

    private fun showValidationState() {
        refundByAmountState = when (validateInput()) {
            TOO_HIGH -> {
                triggerEvent(ShowValidationError(resourceProvider.getString(R.string.order_refunds_refund_high_error)))
                refundByAmountState.copy(isNextButtonEnabled = false)
            }
            TOO_LOW -> {
                triggerEvent(ShowValidationError(resourceProvider.getString(R.string.order_refunds_refund_zero_error)))
                refundByAmountState.copy(isNextButtonEnabled = false)
            }
            VALID -> {
                triggerEvent(HideValidationError)
                refundByAmountState.copy(isNextButtonEnabled = true)
            }
        }
    }

    private fun isInputValid() = validateInput() == VALID

    private enum class InputValidationState {
        TOO_HIGH,
        TOO_LOW,
        VALID
    }

    enum class RefundType {
        ITEMS,
        AMOUNT
    }

    @Parcelize
    data class RefundByAmountViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION,
        val availableForRefund: String? = null,
        val isNextButtonEnabled: Boolean? = null,
        val enteredAmount: BigDecimal = BigDecimal.ZERO
    ) : Parcelable

    @Parcelize
    data class ProductsRefundViewState(
        val currency: String? = null,
        val decimals: Int = DEFAULT_DECIMAL_PRECISION
    ) : Parcelable

    @Parcelize
    data class RefundByItemsViewState(
        val currency: String? = null,
        val isNextButtonEnabled: Boolean? = null,
        val productsRefund: BigDecimal = BigDecimal.ZERO,
        val formattedProductsRefund: String? = null,
        val subtotal: String? = null,
        val taxes: String? = null,
        val shippingRefund: BigDecimal = BigDecimal.ZERO,
        val formattedShippingRefund: String? = null,
        val shippingSubtotal: String? = null,
        val shippingTaxes: String? = null,
        val isShippingRefundVisible: Boolean? = null,
        val selectedItemsHeader: String? = null,
        val selectButtonTitle: String? = null
    ) : Parcelable {
        val totalRefund: BigDecimal
            get() = max(productsRefund + shippingRefund, BigDecimal.ZERO)
    }

    @Parcelize
    data class RefundSummaryViewState(
        val isFormEnabled: Boolean? = null,
        val previouslyRefunded: String? = null,
        val refundAmount: String? = null,
        val refundMethod: String? = null,
        val refundReason: String? = null,
        val isMethodDescriptionVisible: Boolean? = null
    ) : Parcelable

    @Parcelize
    data class CommonViewState(
        val refundTotal: BigDecimal = BigDecimal.ZERO,
        val screenTitle: String? = null,
        val refundType: RefundType = ITEMS
    ) : Parcelable

    sealed class IssueRefundEvent : Event() {
        data class ShowValidationError(val message: String) : IssueRefundEvent()
        data class ShowNumberPicker(val refundItem: RefundListItem) : IssueRefundEvent()
        data class ShowRefundSummary(val refundType: RefundType) : IssueRefundEvent()
        data class ShowRefundAmountDialog(
            val refundAmount: BigDecimal,
            val maxRefund: BigDecimal,
            val message: String
        ) : IssueRefundEvent()
        object HideValidationError : IssueRefundEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<IssueRefundViewModel>
}
