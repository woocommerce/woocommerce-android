package com.woocommerce.android.ui.payments.refunds

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_NOTE_ADD_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_NOTE_ADD_SUCCESS
import com.woocommerce.android.analytics.AnalyticsEvent.REFUND_CREATE
import com.woocommerce.android.analytics.AnalyticsEvent.REFUND_CREATE_FAILED
import com.woocommerce.android.analytics.AnalyticsEvent.REFUND_CREATE_SUCCESS
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.extensions.isCashPayment
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.joinToString
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.model.PaymentGateway
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.getMaxRefundQuantities
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.max
import com.woocommerce.android.util.min
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableListStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.*
import javax.inject.Inject
import org.wordpress.android.fluxc.utils.sumBy as sumByBigDecimal

@HiltViewModel
@Suppress("LargeClass") // TODO Refactor this class in a follow up PR
class IssueRefundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository,
    private val gatewayStore: WCGatewayStore,
    private val refundStore: WCRefundStore,
    private val paymentChargeRepository: PaymentChargeRepository,
    private val orderMapper: OrderMapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val coroutineDispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState) {
    companion object {
        private const val REFUND_METHOD_MANUAL = "manual"
    }

    private val orderFlow: SharedFlow<Order> = flow {
        val order = requireNotNull(
            orderStore.getOrderByIdAndSite(arguments.orderId, selectedSite.get())?.let { orderMapper.toAppModel(it) }
        )
        emit(order)
    }.shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val refundItems = savedState.getNullableListStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = ProductRefundListItem::class.java,
        key = "refundItems"
    )
    private val productsRefundSection = combine(
        refundItems.filterNotNull(),
        orderFlow // Ensure the order is loaded before preparing this section as the order is needed for formatting
    ) { items, _ ->
        prepareProductsRefundSection(items)
    }.shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val isFeesMainSwitchChecked = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "isFeesMainSwitchChecked"
    )
    private val refundFeeLines = savedState.getNullableListStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = FeeRefundListItem::class.java,
        key = "refundFeeLines"
    )
    private val feesRefundSection = combine(
        isFeesMainSwitchChecked,
        refundFeeLines.filterNotNull(),
        orderFlow.map { it.refundableFeeLineIds }
    ) { isFeesMainSwitchChecked, feeLines, refundableFeeLineIds ->
        prepareFeesRefundSection(
            isFeesMainSwitchChecked = isFeesMainSwitchChecked,
            feeLines = feeLines,
            refundableFeeLineIds = refundableFeeLineIds
        )
    }

    private val isShippingMainSwitchChecked = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = false,
        key = "isShippingMainSwitchChecked"
    )
    private val refundShippingLines = savedState.getNullableListStateFlow(
        scope = viewModelScope,
        initialValue = null,
        clazz = ShippingRefundListItem::class.java,
        key = "refundShippingLines"
    )
    private val shippingRefundSection = combine(
        isShippingMainSwitchChecked,
        refundShippingLines.filterNotNull(),
        orderFlow.map { it.refundableShippingLineIds }
    ) { isShippingMainSwitchChecked, shippingLines, refundableShippingLineIds ->
        prepareShippingRefundSection(shippingLines, refundableShippingLineIds, isShippingMainSwitchChecked)
    }

    private val refundNotice = orderFlow.map { order -> prepareRefundNotice(order) }

    val refundByItemsStateLiveData = combine(
        orderFlow,
        productsRefundSection,
        shippingRefundSection,
        feesRefundSection,
        refundNotice
    ) { order, productsSection, shippingSection, feesSection, refundNotice ->
        RefundByItemsViewState(
            currency = order.currency,
            productsSection = productsSection,
            feesSection = feesSection,
            shippingSection = shippingSection,
            refundNotice = refundNotice,
            maxRefund = order.maxRefund
        )
    }
        .onEach { updateRefundTotal(it.grandTotalRefund) }
        .asLiveData()

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val commonStateLiveData = LiveDataDelegate(savedState, CommonViewState())

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val refundSummaryStateLiveData = LiveDataDelegate(savedState, RefundSummaryViewState())

    private var commonState by commonStateLiveData
    private var refundSummaryState by refundSummaryStateLiveData

    private val order: Order
        get() = requireNotNull(orderFlow.replayCache.firstOrNull()) {
            "Please ensure that this property is not accessed before the order is loaded."
        }

    private val refunds: List<Refund>
    private val Order.allFeeLineIds: List<Long>
        get() = feesLines.map { it.id }
    private val Order.refundableFeeLineIds: List<Long>
        get() = allFeeLineIds.filterNot { feeId -> refunds.any { it.id == feeId } }

    private val Order.allShippingLineIds: List<Long>
        get() = shippingLines.map { it.itemId }
    private val Order.refundableShippingLineIds: List<Long>
        get() = allShippingLineIds.filterNot { shippingId ->
            refunds.any { it.id == shippingId }
        }
    private val Order.maxRefund: BigDecimal
        get() = total - refundTotal

    private val Order.containsOnlyCustomAmounts: Boolean
        get() = items.isEmpty() && shippingLines.isEmpty() && feesLines.isNotEmpty()

    private val formatCurrency: (BigDecimal) -> String
        get() = currencyFormatter.buildBigDecimalFormatter(order.currency)
    private lateinit var gateway: PaymentGateway
    private var cardType = PaymentMethodType.CARD_PRESENT
    private val arguments: RefundsArgs by savedState.navArgs()

    private var refundJob: Job? = null
    val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    init {
        refunds = refundStore.getAllRefunds(selectedSite.get(), arguments.orderId).map { it.toAppModel() }

        viewModelScope.launch {
            gateway = loadPaymentGateway()
            initRefundItems()
            initRefundSummaryState()
        }
    }

    private fun initRefundItems() {
        if (refundItems.value != null) {
            return
        }
        viewModelScope.launch {
            val order = orderFlow.first()
            val maxQuantities = refunds.getMaxRefundQuantities(order.items)
                .map { (id, quantity) -> id to quantity }
                .toMap()

            val items = order.items.mapNotNull {
                val maxQuantity = maxQuantities[it.itemId] ?: return@mapNotNull null
                ProductRefundListItem(
                    orderItem = it,
                    maxQuantity = maxQuantity,
                    quantity = 0,
                    subtotal = BigDecimal.ZERO,
                    taxes = emptyList()
                )
            }

            refundItems.value = items.filter { it.maxQuantity > 0 }

            /* Grab all shipping lines listed in the Order, but remove those that are already refunded previously) */
            val shippingLines = order.shippingLines
                .filter { order.refundableShippingLineIds.contains(it.itemId) }
                .map { ShippingRefundListItem(it, isSelected = true) }
            refundShippingLines.value = shippingLines

            /* Grab all fees lines listed in the Order, but remove those that are already refunded previously) */
            val feeLines = order.feesLines
                .filter { order.refundableFeeLineIds.contains(it.id) }
                .map { FeeRefundListItem(it, isSelected = true) }
            refundFeeLines.value = feeLines

            if (order.containsOnlyCustomAmounts) {
                isFeesMainSwitchChecked.value = true
            }
        }
    }

    private fun prepareProductsRefundSection(
        items: List<ProductRefundListItem>
    ): ProductsRefundSection {
        val totalRefund = items.sumByBigDecimal { it.total }

        return ProductsRefundSection(
            refundItems = items,
            productsRefund = totalRefund,
            formattedProductsRefund = formatCurrency(totalRefund),
            selectedItemsHeader = resourceProvider.getString(
                R.string.order_refunds_items_selected,
                items.sumOf { it.quantity }
            ),
            selectButtonTitle = if (items.areAllItemsSelected()) {
                resourceProvider.getString(R.string.order_refunds_items_select_none)
            } else {
                resourceProvider.getString(R.string.order_refunds_items_select_all)
            }
        )
    }

    private fun prepareFeesRefundSection(
        isFeesMainSwitchChecked: Boolean,
        feeLines: List<FeeRefundListItem>,
        refundableFeeLineIds: List<Long>,
    ): FeesRefundSection {
        val selectedFees = feeLines.takeIf { isFeesMainSwitchChecked }.orEmpty().filter { it.isSelected }
        val totalRefund = selectedFees.sumOf { it.total }

        return FeesRefundSection(
            feeRefundLines = feeLines,
            isFeesRefundAvailable = refundableFeeLineIds.isNotEmpty(),
            isFeesMainSwitchChecked = isFeesMainSwitchChecked,
            feesRefund = totalRefund,
            feesSubtotalFormatted = formatCurrency(selectedFees.sumByBigDecimal { it.feeLine.total }),
            feesTaxesFormatted = formatCurrency(selectedFees.sumByBigDecimal { it.feeLine.totalTax }),
            feesRefundTotalFormatted = formatCurrency(totalRefund),
        )
    }

    private fun prepareShippingRefundSection(
        shippingLines: List<ShippingRefundListItem>,
        refundableShippingLineIds: List<Long>,
        isShippingMainSwitchChecked: Boolean
    ): ShippingRefundSection {
        val selectedShipping = shippingLines.takeIf { isShippingMainSwitchChecked }.orEmpty().filter { it.isSelected }
        val totalRefund = selectedShipping.sumOf { it.total }
        return ShippingRefundSection(
            shippingRefundLines = shippingLines,
            // We only support refunding an Order with one shipping refund for now.
            // In the future, to support multiple shipping refund, we can replace this
            // with refundableShippingLineIds.isNotEmpty()
            isShippingRefundAvailable = refundableShippingLineIds.size == 1,
            isShippingMainSwitchChecked = isShippingMainSwitchChecked,
            shippingRefund = totalRefund,
            shippingSubtotalFormatted = formatCurrency(selectedShipping.sumByBigDecimal { it.shippingLine.total }),
            shippingTaxesFormatted = formatCurrency(selectedShipping.sumByBigDecimal { it.shippingLine.totalTax }),
            shippingRefundTotalFormatted = formatCurrency(totalRefund)
        )
    }

    private fun prepareRefundNotice(order: Order): String? {
        val refundOptions = mutableListOf<String>()
        // Inform user that multiple shipping lines can only be refunded in wp-admin.
        if (order.refundableShippingLineIds.size > 1) {
            val shipping = resourceProvider.getString(R.string.multiple_shipping).lowercase(Locale.getDefault())
            refundOptions.add(shipping)
        }

        return if (refundOptions.isNotEmpty()) {
            val and = resourceProvider.getString(R.string.and).lowercase(Locale.getDefault())
            val options = refundOptions.joinToString(lastSeparator = " $and ")
            resourceProvider.getString(R.string.order_refunds_shipping_refund_variable_notice, options)
        } else {
            null
        }
    }

    private fun updateRefundTotal(amount: BigDecimal) {
        commonState = commonState.copy(
            refundTotal = amount,
            screenTitle = resourceProvider.getString(
                R.string.order_refunds_title_with_amount, formatCurrency(amount)
            )
        )
    }

    private fun initRefundSummaryState() {
        if (refundSummaryStateLiveData.hasInitialValue) {
            val manualRefundMethod = resourceProvider.getString(R.string.order_refunds_manual_refund)
            if (!order.paymentMethod.isCashPayment && (!gateway.isEnabled || !gateway.supportsRefunds)) {
                val paymentTitle = if (gateway.title.isNotBlank()) {
                    resourceProvider.getString(R.string.order_refunds_method, manualRefundMethod, gateway.title)
                } else {
                    manualRefundMethod
                }
                updateRefundSummaryState(paymentTitle, isMethodDescriptionVisible = true)
            } else {
                enrichRefundMethodWithCardDetails(gateway.title.ifBlank { manualRefundMethod })
            }
        }
    }

    private suspend fun loadPaymentGateway(): PaymentGateway = withContext(coroutineDispatchers.io) {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), orderFlow.first().paymentMethod)?.toAppModel()
        return@withContext if (paymentGateway != null && paymentGateway.isEnabled) {
            paymentGateway
        } else {
            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    fun onNextButtonTappedFromItems() {
        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id
            )
        )

        showRefundSummary()
    }

    fun onOpenStoreAdminLinkClicked() {
        triggerEvent(OpenUrl(selectedSite.get().adminUrlOrDefault))
    }

    private fun showRefundSummary() {
        refundSummaryState = refundSummaryState.copy(
            isFormEnabled = true,
            previouslyRefunded = formatCurrency(order.refundTotal),
            refundAmount = commonState.refundTotal,
            refundAmountFormatted = formatCurrency(commonState.refundTotal)
        )

        triggerEvent(ShowRefundSummary)
    }

    fun onRefundConfirmed(wasConfirmed: Boolean) {
        if (wasConfirmed) {
            if (networkStatus.isConnected()) {
                refundJob = launch {
                    refundSummaryState = refundSummaryState.copy(
                        isFormEnabled = false
                    )
                    if (isInteracRefund()) {
                        triggerEvent(IssueRefundEvent.NavigateToCardReaderScreen(order.id, commonState.refundTotal))
                    } else {
                        triggerEvent(
                            ShowSnackbar(
                                R.string.order_refunds_amount_refund_progress_message,
                                arrayOf(formatCurrency(commonState.refundTotal))
                            )
                        )
                        refund()
                    }

                    analyticsTrackerWrapper.track(
                        REFUND_CREATE,
                        mapOf(
                            AnalyticsTracker.KEY_ORDER_ID to order.id,
                            AnalyticsTracker.KEY_REFUND_IS_FULL to
                                (commonState.refundTotal isEqualTo order.maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_METHOD to gateway.methodTitle,
                            AnalyticsTracker.KEY_AMOUNT to commonState.refundTotal.toString()
                        )
                    )
                    refundSummaryState = refundSummaryState.copy(isFormEnabled = true)
                }
            } else {
                triggerEvent(ShowSnackbar(R.string.offline_error))
            }
        }
    }

    private fun isInteracRefund() = cardType == PaymentMethodType.INTERAC_PRESENT

    /*
       This method does the actual refund in case of non-interac refund. In case of Interac refund, the actual
       refund happens on the client-side and this method updates the WCPay backend about the refund success status and
       does not process the refund itself.

       For non-Interac refund -> Process the refund (Entire refund logic lives in the backend)
       For Interac refund -> Update the backend of the successful refund. The actual refund happens on the client-side
     */
    fun refund() {
        triggerUIMessageIfRefundIsInterac()
        launch {
            val result = initiateRefund()
            if (result.isError) {
                trackRefundError(result)
                triggerUIMessage()
            } else {
                trackRefundSuccess(result)
                updateRefundSummaryStateWithOrderNote()
                triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_successful))
                triggerEvent(Exit)
            }
        }
    }

    private fun triggerUIMessageIfRefundIsInterac() {
        if (isInteracRefund()) {
            triggerEvent(ShowSnackbar(R.string.card_reader_interac_refund_notifying_backend_about_successful_refund))
        }
    }

    private suspend fun initiateRefund(): WooResult<WCRefundModel> {
        val allItems = mutableListOf<RefundRequestItem>()
        refundItems.value?.let {
            it.forEach { item -> allItems.add(item.toDataModel()) }
        }

        val selectedShipping = if (isShippingMainSwitchChecked.value) {
            refundShippingLines.value?.filter { it.isSelected }
        } else {
            emptyList()
        }
        selectedShipping?.forEach { allItems.add(it.toDataModel()) }

        val selectedFees = if (isFeesMainSwitchChecked.value) {
            refundFeeLines.value?.filter { it.isSelected }
        } else {
            emptyList()
        }
        selectedFees?.forEach { allItems.add(it.toDataModel()) }

        return refundStore.createItemsRefund(
            site = selectedSite.get(),
            orderId = order.id,
            amount = refundSummaryState.refundAmount,
            reason = refundSummaryState.refundReason ?: "",
            restockItems = true,
            autoRefund = gateway.supportsRefunds,
            items = allItems
        )
    }

    private fun trackRefundError(result: WooResult<WCRefundModel>) {
        analyticsTrackerWrapper.track(
            REFUND_CREATE_FAILED,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id,
                AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                AnalyticsTracker.KEY_ERROR_TYPE to result.error.type.toString(),
                AnalyticsTracker.KEY_ERROR_DESC to result.error.message
            )
        )
    }

    private fun triggerUIMessage() {
        if (isInteracRefund()) {
            triggerEvent(
                ShowSnackbar(
                    R.string.card_reader_interac_refund_notifying_backend_about_successful_refund_failed
                )
            )
        } else {
            triggerEvent(ShowSnackbar(R.string.order_refunds_amount_refund_error))
        }
    }

    private fun trackRefundSuccess(result: WooResult<WCRefundModel>) {
        analyticsTrackerWrapper.track(
            REFUND_CREATE_SUCCESS,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id,
                AnalyticsTracker.KEY_ID to result.model?.id
            )
        )
    }

    private suspend fun updateRefundSummaryStateWithOrderNote() {
        refundSummaryState.refundReason?.let { reason ->
            if (reason.isNotBlank()) {
                addOrderNote(reason)
            }
        }
    }

    private suspend fun addOrderNote(reason: String) {
        val note = OrderNote(note = reason, isCustomerNote = false)
        orderDetailRepository.addOrderNote(order.id, note).fold(
            onSuccess = {
                analyticsTrackerWrapper.track(ORDER_NOTE_ADD_SUCCESS)
            },
            onFailure = {
                analyticsTrackerWrapper.track(
                    ORDER_NOTE_ADD_FAILED,
                    prepareTracksEventsDetails(it as WooException)
                )
            }
        )
    }

    fun onRefundIssued(reason: String) = viewModelScope.launch {
        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_ORDER_ID to order.id
            )
        )

        refundSummaryState = refundSummaryState.copy(
            refundReason = reason
        )

        triggerEvent(
            ShowRefundConfirmation(
                resourceProvider.getString(
                    R.string.order_refunds_title_with_amount,
                    formatCurrency(commonState.refundTotal)
                ),
                resourceProvider.getString(R.string.order_refunds_confirmation),
                resourceProvider.getString(R.string.order_refunds_refund)
            )
        )
    }

    fun onRefundQuantityTapped(uniqueId: Long) {
        refundItems.value?.firstOrNull { it.orderItem.itemId == uniqueId }?.let {
            triggerEvent(ShowNumberPicker(it))
        }

        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED,
            mapOf(AnalyticsTracker.KEY_ORDER_ID to order.id)
        )
    }

    /**
     * Checks if the refund summary button label should be enabled. If the max length for the text field is
     * surpassed, the button should be disabled until the text is brought within the maximum length.
     */
    fun onRefundSummaryTextChanged(maxLength: Int, currLength: Int) {
        refundSummaryState = refundSummaryState.copy(isSummaryTextTooLong = currLength > maxLength)
    }

    fun onRefundQuantityChanged(uniqueId: Long, newQuantity: Int) {
        val newItems = getUpdatedItemList(uniqueId, newQuantity)
        refundItems.value = newItems
    }

    private fun getUpdatedItemList(uniqueId: Long, newQuantity: Int): MutableList<ProductRefundListItem> {
        val newItems = mutableListOf<ProductRefundListItem>()
        refundItems.value?.forEach {
            if (it.orderItem.itemId == uniqueId) {
                // Update the quantity
                var newItem = it.copy(quantity = newQuantity)

                // Update the subtotal and taxes based on the new quantity
                newItem = newItem.copy(
                    subtotal = newItem.calculateTotalSubtotal(),
                    totalTax = newItem.calculateTotalTaxes(),
                    taxes = newItem.calculateTaxesList()
                )

                newItems.add(newItem)
            } else {
                newItems.add(it)
            }
        }
        return newItems
    }

    fun onSelectButtonTapped() {
        launch {
            if (productsRefundSection.first().allItemsSelected) {
                refundItems.value?.forEach {
                    onRefundQuantityChanged(it.orderItem.itemId, 0)
                }
            } else {
                refundItems.value?.forEach {
                    onRefundQuantityChanged(it.orderItem.itemId, it.availableRefundQuantity)
                }
            }

            analyticsTrackerWrapper.track(
                CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to order.id)
            )
        }
    }

    fun onShippingRefundMainSwitchChanged(isChecked: Boolean) {
        isShippingMainSwitchChecked.value = isChecked
    }

    fun onFeesRefundMainSwitchChanged(isChecked: Boolean) {
        isFeesMainSwitchChecked.value = isChecked
    }

    fun onShippingLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        refundShippingLines.update {
            it?.map { shippingLine ->
                if (shippingLine.shippingLine.itemId == itemId) {
                    shippingLine.copy(isSelected = isChecked)
                } else {
                    shippingLine
                }
            }
        }
    }

    fun onFeeLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        refundFeeLines.update {
            it?.map { feeLine ->
                if (feeLine.feeLine.id == itemId) {
                    feeLine.copy(isSelected = isChecked)
                } else {
                    feeLine
                }
            }
        }
    }

    private fun enrichRefundMethodWithCardDetails(refundMethod: String) {
        val chargeId = order.chargeId
        if (chargeId != null) {
            loadCardDetails(chargeId, refundMethod)
        } else {
            updateRefundSummaryState(refundMethod, isMethodDescriptionVisible = false)
        }
    }

    private fun loadCardDetails(chargeId: String, refundMethod: String) {
        launch {
            refundSummaryState = refundSummaryState.copy(isFetchingCardData = true)
            val result = paymentChargeRepository.fetchCardDataUsedForOrderPayment(chargeId)
            refundSummaryState = refundSummaryState.copy(isFetchingCardData = false)
            when (result) {
                is PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Success -> {
                    cardType = PaymentMethodType.fromValue(result.paymentMethodType ?: "card_present")
                    val refundMethodWithCard = result.run {
                        val brand = result.cardBrand.orEmpty().replaceFirstChar { it.uppercase() }
                        val last4 = result.cardLast4.orEmpty()
                        "$refundMethod ($brand **** $last4)"
                    }
                    updateRefundSummaryState(refundMethodWithCard, isMethodDescriptionVisible = false)
                }

                PaymentChargeRepository.CardDataUsedForOrderPaymentResult.Error -> {
                    cardType = PaymentMethodType.CARD_PRESENT
                    updateRefundSummaryState(refundMethod, isMethodDescriptionVisible = false)
                }
            }
        }
    }

    private fun updateRefundSummaryState(refundMethod: String, isMethodDescriptionVisible: Boolean) {
        refundSummaryState = refundSummaryState.copy(
            refundMethod = refundMethod,
            isMethodDescriptionVisible = isMethodDescriptionVisible
        )
    }

    private fun prepareTracksEventsDetails(exception: WooException) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to exception.error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to exception.error.message
    )

    data class RefundByItemsViewState(
        val currency: String,
        val productsSection: ProductsRefundSection,
        val feesSection: FeesRefundSection,
        val shippingSection: ShippingRefundSection,
        val refundNotice: String?,
        val maxRefund: BigDecimal
    ) {
        val grandTotalRefund: BigDecimal
            get() {
                val refundTotal = max(
                    productsSection.productsRefund + shippingSection.shippingRefund + feesSection.feesRefund,
                    BigDecimal.ZERO
                )
                return min(refundTotal, maxRefund)
            }

        val isNextButtonEnabled: Boolean
            get() = grandTotalRefund > BigDecimal.ZERO

        val isRefundNoticeVisible
            get() = !refundNotice.isNullOrEmpty()
    }

    data class ProductsRefundSection(
        val refundItems: List<ProductRefundListItem>,
        val productsRefund: BigDecimal,
        val selectedItemsHeader: String,
        val formattedProductsRefund: String,
        val selectButtonTitle: String
    ) {
        val allItemsSelected
            get() = refundItems.areAllItemsSelected()
    }

    data class FeesRefundSection(
        val feeRefundLines: List<FeeRefundListItem>,
        val isFeesRefundAvailable: Boolean,
        val isFeesMainSwitchChecked: Boolean,
        val feesRefund: BigDecimal,
        val feesSubtotalFormatted: String,
        val feesTaxesFormatted: String,
        val feesRefundTotalFormatted: String,
    )

    data class ShippingRefundSection(
        val shippingRefundLines: List<ShippingRefundListItem>,
        val isShippingRefundAvailable: Boolean,
        val isShippingMainSwitchChecked: Boolean,
        val shippingRefund: BigDecimal,
        val shippingSubtotalFormatted: String,
        val shippingTaxesFormatted: String,
        val shippingRefundTotalFormatted: String,
    )

    @Parcelize
    data class RefundSummaryViewState(
        val isFormEnabled: Boolean? = null,
        val previouslyRefunded: String? = null,
        val refundAmount: BigDecimal? = null,
        val refundAmountFormatted: String? = null,
        val refundMethod: String? = null,
        val refundReason: String? = null,
        val isMethodDescriptionVisible: Boolean? = null,
        val isSummaryTextTooLong: Boolean = false,
        val isFetchingCardData: Boolean = false,
    ) : Parcelable {
        val isSubmitButtonEnabled: Boolean
            get() = !isSummaryTextTooLong && !isFetchingCardData
    }

    @Parcelize
    data class CommonViewState(
        val refundTotal: BigDecimal = BigDecimal.ZERO,
        val screenTitle: String? = null
    ) : Parcelable

    sealed class IssueRefundEvent : Event() {
        data class ShowNumberPicker(val refundItem: ProductRefundListItem) : IssueRefundEvent()
        data class ShowRefundConfirmation(
            val title: String,
            val message: String,
            val confirmButtonTitle: String
        ) : IssueRefundEvent()

        data object ShowRefundSummary : IssueRefundEvent()
        data class OpenUrl(val url: String) : IssueRefundEvent()
        data class NavigateToCardReaderScreen(val orderId: Long, val refundAmount: BigDecimal) : IssueRefundEvent()
    }

    enum class PaymentMethodType(val paymentMethodType: String) {
        CARD_PRESENT("card_present"),
        INTERAC_PRESENT("interac_present");

        companion object {
            fun fromValue(paymentMethodType: String?): PaymentMethodType {
                return entries.firstOrNull { it.paymentMethodType == paymentMethodType } ?: CARD_PRESENT
            }
        }
    }
}

private fun List<ProductRefundListItem>.areAllItemsSelected(): Boolean {
    return all { it.quantity == it.availableRefundQuantity }
}
