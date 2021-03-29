package com.woocommerce.android.ui.refunds

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_PRODUCT_AMOUNT_DIALOG_OPENED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_TAB_CHANGED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.REFUND_CREATE
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.REFUND_CREATE_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.REFUND_CREATE_SUCCESS
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.extensions.calculateTotals
import com.woocommerce.android.extensions.isCashPayment
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.getMaxRefundQuantities
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toItem
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.HideValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundAmountDialog
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowValidationError
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType.AMOUNT
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.RefundType.ITEMS
import com.woocommerce.android.ui.refunds.RefundProductListAdapter.RefundListItem
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.max
import com.woocommerce.android.util.min
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.utils.sumBy as sumByBigDecimal
import java.math.BigDecimal
import kotlin.math.min

class IssueRefundViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    currencyFormatter: CurrencyFormatter,
    private val orderStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository,
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

    private val _refundShippingLines = MutableLiveData<List<RefundListItem>>()
    val refundShippingLines: LiveData<List<RefundListItem>> = _refundShippingLines

    private val areAllItemsSelected: Boolean
        get() = refundItems.value?.all { it.quantity == it.maxQuantity } ?: false

    final val commonStateLiveData = LiveDataDelegate(savedState, CommonViewState())
    final val refundSummaryStateLiveData = LiveDataDelegate(savedState, RefundSummaryViewState())
    final val refundByItemsStateLiveData = LiveDataDelegate(savedState, RefundByItemsViewState(), onChange = { _, new ->
        updateRefundTotal(new.grandTotalRefund)
    })
    final val refundByAmountStateLiveData = LiveDataDelegate(
        savedState,
        RefundByAmountViewState(),
        onChange = { _, new ->
            updateRefundTotal(new.enteredAmount)
        }
    )
    final val productsRefundLiveData = LiveDataDelegate(savedState, ProductsRefundViewState())

    private var commonState by commonStateLiveData
    private var refundByAmountState by refundByAmountStateLiveData
    private var refundByItemsState by refundByItemsStateLiveData
    private var refundSummaryState by refundSummaryStateLiveData
    private var productsRefundState by productsRefundLiveData

    private val order: Order
    private val refunds: List<Refund>
    private val selectedShippingLineIds: List<Long>

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

    private var refundJob: Job? = null
    final val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    init {
        order = loadOrder(arguments.orderId)
        refunds = refundStore.getAllRefunds(selectedSite.get(), arguments.orderId).map { it.toAppModel() }
        selectedShippingLineIds = order.shippingLines.map { it.itemId }

        formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
        maxRefund = order.total - order.refundTotal
        maxQuantities = refunds.getMaxRefundQuantities(order.items)
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
                    ),
                    isNextButtonEnabled = false
            )
        }
    }

    private fun initRefundByItemsState() {
        if (refundByItemsStateLiveData.hasInitialValue) {
            refundByItemsState = refundByItemsState.copy(
                    currency = order.currency,
                    subtotal = formatCurrency(BigDecimal.ZERO),
                    taxes = formatCurrency(BigDecimal.ZERO),
                    shippingSubtotal = formatCurrency(order.shippingTotal),
                    shippingTaxes = formatCurrency(order.shippingLines.sumByBigDecimal { it.totalTax }),
                    feesTotal = formatCurrency(order.feesTotal),
                    formattedProductsRefund = formatCurrency(BigDecimal.ZERO),
                    isShippingRefundAvailable = order.shippingTotal > BigDecimal.ZERO,
                    isFeesVisible = order.feesTotal > BigDecimal.ZERO,
                    isNextButtonEnabled = false,
                    selectedShippingLines = selectedShippingLineIds,
                    formattedShippingRefundTotal = formatCurrency(BigDecimal.ZERO)
            )
        }

        val items = order.items.map {
            val maxQuantity = maxQuantities[it.uniqueId] ?: 0
            val selectedQuantity = min(selectedQuantities[it.uniqueId] ?: 0, maxQuantity)
            RefundListItem(it, maxQuantity, selectedQuantity)
        }
        updateRefundItems(items)

        val shippingLines = order.shippingLines.map {
            RefundListItem(it.toItem(), 1, 1)
        }
        updateRefundShippingLines(shippingLines)

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

            if (!order.paymentMethod.isCashPayment && (!gateway.isEnabled || !gateway.supportsRefunds)) {
                paymentTitle = if (gateway.title.isNotBlank())
                    resourceProvider.getString(R.string.order_refunds_method, manualRefundMethod, gateway.title)
                else
                    manualRefundMethod
                isManualRefund = true
            } else {
                paymentTitle = if (gateway.title.isNotBlank()) gateway.title else manualRefundMethod
                isManualRefund = false
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

    fun onOpenStoreAdminLinkClicked() {
        triggerEvent(OpenUrl(selectedSite.get().adminUrl))
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

    fun onRefundConfirmed(wasConfirmed: Boolean) {
        if (wasConfirmed) {
            if (networkStatus.isConnected()) {
                refundJob = launch {
                    refundSummaryState = refundSummaryState.copy(
                            isFormEnabled = false
                    )

                    triggerEvent(
                            ShowSnackbar(
                                    R.string.order_refunds_amount_refund_progress_message,
                                    arrayOf(formatCurrency(commonState.refundTotal))
                            )
                    )

                    AnalyticsTracker.track(
                            REFUND_CREATE, mapOf(
                                AnalyticsTracker.KEY_ORDER_ID to order.remoteId,
                                AnalyticsTracker.KEY_REFUND_IS_FULL to
                                        (commonState.refundTotal isEqualTo maxRefund).toString(),
                                AnalyticsTracker.KEY_REFUND_TYPE to commonState.refundType.name,
                                AnalyticsTracker.KEY_REFUND_METHOD to gateway.methodTitle,
                                AnalyticsTracker.KEY_REFUND_AMOUNT to commonState.refundTotal.toString()
                        )
                    )

                    val resultCall = async(dispatchers.io) {
                        return@async when (commonState.refundType) {
                            ITEMS -> {
                                // TODO: Include selected shipping lines in `items`
                                refundStore.createItemsRefund(
                                        selectedSite.get(),
                                        order.remoteId,
                                        refundSummaryState.refundReason ?: "",
                                        true,
                                        gateway.supportsRefunds,
                                        refundItems.value?.map { it.toDataModel() }
                                                ?: emptyList()
                                )
                            }
                            AMOUNT -> {
                                refundStore.createAmountRefund(
                                        selectedSite.get(),
                                        order.remoteId,
                                        commonState.refundTotal,
                                        refundSummaryState.refundReason ?: "",
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
                                AnalyticsTracker.KEY_ID to result.model?.id)
                        )

                        refundSummaryState.refundReason?.let { reason ->
                            if (reason.isNotBlank()) {
                                val note = OrderNote(note = reason, isCustomerNote = false)
                                orderDetailRepository.addOrderNote(order.identifier, order.remoteId, note)
                            }
                        }

                        triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_successful))
                        triggerEvent(Exit)
                    }

                    refundSummaryState = refundSummaryState.copy(isFormEnabled = true)
                }
            } else {
                triggerEvent(ShowSnackbar(R.string.offline_error))
            }
        }
    }

    fun onRefundIssued(reason: String) {
        AnalyticsTracker.track(CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED, mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.remoteId
        ))

        refundSummaryState = refundSummaryState.copy(
                refundReason = reason
        )

        triggerEvent(ShowRefundConfirmation(
                resourceProvider.getString(
                        R.string.order_refunds_title_with_amount,
                        formatCurrency(commonState.refundTotal)
                ),
                resourceProvider.getString(R.string.order_refunds_confirmation),
                resourceProvider.getString(R.string.order_refunds_refund))
        )
    }

    fun onRefundQuantityTapped(uniqueId: Long) {
        _refundItems.value?.firstOrNull { it.orderItem.uniqueId == uniqueId }?.let {
            triggerEvent(ShowNumberPicker(it))
        }

        AnalyticsTracker.track(
                CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to order.remoteId)
        )
    }

    /**
     * Checks if the refund summary button label should be enabled. If the max length for the text field is
     * surpassed, the button should be disabled until the text is brought within the maximum length.
     */
    fun onRefundSummaryTextChanged(maxLength: Int, currLength: Int) {
        refundSummaryState = refundSummaryState.copy(isSubmitButtonEnabled = currLength <= maxLength)
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
        val shippingRefund = refundByItemsState.shippingRefund
        refundByItemsState = refundByItemsState.copy(
                productsRefund = newAmount,
                formattedProductsRefund = formatCurrency(newAmount),
                isNextButtonEnabled = newAmount > BigDecimal.ZERO,
                grandTotalRefund = newAmount + shippingRefund
        )
    }

    fun onRefundQuantityChanged(uniqueId: Long, newQuantity: Int) {
        val newItems = getUpdatedItemList(uniqueId, newQuantity)
        updateRefundItems(newItems)

        selectedQuantities[uniqueId] = newQuantity

        val (subtotal, taxes) = newItems.calculateTotals()
        val productsRefund = min(max(subtotal + taxes, BigDecimal.ZERO), maxRefund)

        val selectButtonTitle = if (areAllItemsSelected)
                resourceProvider.getString(R.string.order_refunds_items_select_none)
            else
                resourceProvider.getString(R.string.order_refunds_items_select_all)

        var grandTotal = productsRefund
        if (refundByItemsState.isShippingMainSwitchChecked == true) {
            grandTotal = grandTotal.add(refundByItemsState.shippingRefund)
        }

        refundByItemsState = refundByItemsState.copy(
                productsRefund = productsRefund,
                formattedProductsRefund = formatCurrency(productsRefund),
                taxes = formatCurrency(taxes),
                subtotal = formatCurrency(subtotal),
                isNextButtonEnabled = _refundItems.value?.any { it.quantity > 0 } ?: false,
                selectButtonTitle = selectButtonTitle,
                grandTotalRefund = grandTotal
        )
    }

    private fun getUpdatedItemList(uniqueId: Long, newQuantity: Int): MutableList<RefundListItem> {
        val newItems = mutableListOf<RefundListItem>()
        _refundItems.value?.forEach {
            if (it.orderItem.uniqueId == uniqueId) {
                newItems.add(
                        it.copy(
                                quantity = newQuantity,
                                maxQuantity = maxQuantities[uniqueId] ?: 0
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
                onRefundQuantityChanged(it.orderItem.uniqueId, 0)
            }
        } else {
            _refundItems.value?.forEach {
                onRefundQuantityChanged(it.orderItem.uniqueId, it.maxQuantity)
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

    private fun updateRefundShippingLines(items: List<RefundListItem>) {
        _refundShippingLines.value = items.filter { it.maxQuantity > 0 }
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

    fun onShippingRefundMainSwitchChanged(isChecked: Boolean) {
        val productsRefund = refundByItemsState.productsRefund

        if (isChecked) {
            val shippingRefund = calculatePartialShippingTotal(refundByItemsState.selectedShippingLines!!)

            refundByItemsState = refundByItemsState.copy(
                shippingRefund = shippingRefund,
                formattedShippingRefundTotal = formatCurrency(shippingRefund),
                grandTotalRefund = productsRefund.add(shippingRefund),
                isShippingMainSwitchChecked = true
            )
        } else {
            refundByItemsState = refundByItemsState.copy(
                shippingRefund = 0.toBigDecimal(),
                formattedShippingRefundTotal = formatCurrency(0.toBigDecimal()),
                grandTotalRefund = productsRefund,
                isShippingMainSwitchChecked = false
            )
        }
    }

    fun onShippingLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        val list = refundByItemsState.selectedShippingLines?.toMutableList()
        if (list != null) {
            if (isChecked) list += itemId else list -= itemId

            val newShippingRefundTotal = calculatePartialShippingTotal(list)
            val productsRefund = refundByItemsState.productsRefund

            refundByItemsState = refundByItemsState.copy(
                selectedShippingLines = list,
                shippingSubtotal = formatCurrency(calculatePartialShippingSubtotal(list)),
                shippingTaxes = formatCurrency(calculatePartialShippingTaxes(list)),
                shippingRefund = newShippingRefundTotal,
                formattedShippingRefundTotal = formatCurrency(newShippingRefundTotal),
                grandTotalRefund = productsRefund.add(newShippingRefundTotal)
            )
        }
    }

    private fun calculatePartialShippingSubtotal(selectedShippingLinesId: List<Long>): BigDecimal {
        if (selectedShippingLinesId.isEmpty())
            return 0.toBigDecimal()
        else
            return order.shippingLines
                .filter { selectedShippingLinesId.contains(it.itemId) }
                .map { it.total }
                .reduce { acc, subtotal -> return acc + subtotal }
    }

    private fun calculatePartialShippingTaxes(selectedShippingLinesId: List<Long>): BigDecimal {
        if (selectedShippingLinesId.isEmpty())
            return 0.toBigDecimal()
        else
            return order.shippingLines
                .filter { selectedShippingLinesId.contains(it.itemId) }
                .map { it.totalTax }
                .reduce { acc, subtotal -> return acc + subtotal }
    }

    private fun calculatePartialShippingTotal(selectedShippingLinesId: List<Long>): BigDecimal {
        return calculatePartialShippingSubtotal(selectedShippingLinesId)
            .add(calculatePartialShippingTaxes(selectedShippingLinesId))
    }

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
        val feesTotal: String? = null,
        val shippingSubtotal: String? = null,
        val shippingTaxes: String? = null,
        val shippingRefund: BigDecimal = BigDecimal.ZERO,
        val formattedShippingRefundTotal: String? = null,
        val isShippingRefundAvailable: Boolean? = null,
        val isShippingMainSwitchChecked: Boolean? = null,
        val selectedShippingLines: List<Long>? = null,
        val isFeesVisible: Boolean? = null,
        val selectedItemsHeader: String? = null,
        val selectButtonTitle: String? = null,
        val grandTotalRefund: BigDecimal = BigDecimal.ZERO /* for now it's productsRefund + shippingRefund */
    ) : Parcelable {
        val totalRefund: BigDecimal
            get() = max(productsRefund + shippingRefund, BigDecimal.ZERO)
    }

    @Parcelize
    data class RefundSummaryViewState(
        val isFormEnabled: Boolean? = null,
        val isSubmitButtonEnabled: Boolean? = null,
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
        data class ShowRefundConfirmation(
            val title: String,
            val message: String,
            val confirmButtonTitle: String
        ) : IssueRefundEvent()
        data class ShowRefundSummary(val refundType: RefundType) : IssueRefundEvent()
        data class ShowRefundAmountDialog(
            val refundAmount: BigDecimal,
            val maxRefund: BigDecimal,
            val message: String
        ) : IssueRefundEvent()
        data class OpenUrl(val url: String) : IssueRefundEvent()
        object HideValidationError : IssueRefundEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<IssueRefundViewModel>
}
