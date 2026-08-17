package com.woocommerce.android.ui.payments.refunds

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.getMaxRefundQuantities
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.max
import com.woocommerce.android.util.min
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableListStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import org.wordpress.android.fluxc.utils.sumBy as sumByBigDecimal

@HiltViewModel
class IssueRefundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    refundStore: WCRefundStore,
    private val orderMapper: OrderMapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    private val orderFlow: SharedFlow<Order> = flow {
        val order = requireNotNull(
            orderStore.getOrderByIdAndSite(arguments.orderId, selectedSite.get())?.let { orderMapper.toAppModel(it) }
        )
        emit(order)
    }.shareIn(viewModelScope, started = SharingStarted.Lazily, replay = 1)

    private val refundsFlow: SharedFlow<List<Refund>> = flow {
        emit(refundStore.getAllRefunds(selectedSite.get(), arguments.orderId).map { it.toAppModel() })
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
        orderFlow,
        refundsFlow
    ) { isFeesMainSwitchChecked, feeLines, order, refunds ->
        prepareFeesRefundSection(
            isFeesMainSwitchChecked = isFeesMainSwitchChecked,
            feeLines = feeLines,
            refundableFeeLineIds = order.refundableFeeLineIds(refunds)
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
        orderFlow,
        refundsFlow
    ) { isShippingMainSwitchChecked, shippingLines, order, refunds ->
        prepareShippingRefundSection(
            shippingLines,
            order.refundableShippingLineIds(refunds),
            isShippingMainSwitchChecked
        )
    }

    private val refundNotice = combine(orderFlow, refundsFlow) { order, refunds -> prepareRefundNotice(order, refunds) }

    val viewState = combine(
        orderFlow,
        productsRefundSection,
        shippingRefundSection,
        feesRefundSection,
        refundNotice
    ) { order, productsSection, shippingSection, feesSection, refundNotice ->
        IssueRefundViewState(
            currency = order.currency,
            productsSection = productsSection,
            feesSection = feesSection,
            shippingSection = shippingSection,
            refundNotice = refundNotice,
            maxRefund = order.maxRefund
        )
    }.asLiveData()

    private val order: Order
        get() = requireNotNull(orderFlow.replayCache.firstOrNull()) {
            "Please ensure that this property is not accessed before the order is loaded."
        }

    private val Order.allFeeLineIds: List<Long>
        get() = feesLines.map { it.id }
    private fun Order.refundableFeeLineIds(refunds: List<Refund>): List<Long> =
        allFeeLineIds.filterNot { feeId ->
            refunds.any { refund -> refund.feeLines.any { it.id == feeId } }
        }

    private val Order.allShippingLineIds: List<Long>
        get() = shippingLines.map { it.itemId }
    private fun Order.refundableShippingLineIds(refunds: List<Refund>): List<Long> =
        allShippingLineIds.filterNot { shippingId ->
            refunds.any { refund -> refund.shippingLines.any { it.itemId == shippingId } }
        }

    private val Order.containsOnlyCustomAmounts: Boolean
        get() = items.isEmpty() && shippingLines.isEmpty() && feesLines.isNotEmpty()

    private val formatCurrency: (BigDecimal) -> String
        get() = currencyFormatter.buildBigDecimalFormatter(order.currency)
    private val arguments: RefundsArgs by savedState.navArgs()

    val IssueRefundViewState.screenTitle: String
        get() = resourceProvider.getString(
            R.string.order_refunds_title_with_amount,
            formatCurrency(this.grandTotalRefund)
        )

    init {
        viewModelScope.launch {
            initRefundItems()
        }
    }

    private fun initRefundItems() {
        if (refundItems.value != null) {
            return
        }
        viewModelScope.launch {
            val order = orderFlow.first()
            val refunds = refundsFlow.first()
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

            /* Grab all shipping lines listed in the Order, but remove those that are already refunded previously */
            val shippingLines = order.shippingLines
                .filter { order.refundableShippingLineIds(refunds).contains(it.itemId) }
                .map { ShippingRefundListItem(it, isSelected = true) }
            refundShippingLines.value = shippingLines

            /* Grab all fees lines listed in the Order, but remove those that are already refunded previously */
            val feeLines = order.feesLines
                .filter { order.refundableFeeLineIds(refunds).contains(it.id) }
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

    private fun prepareRefundNotice(order: Order, refunds: List<Refund>): String? {
        return if (order.refundableShippingLineIds(refunds).size > 1) {
            resourceProvider.getString(
                R.string.order_refunds_shipping_refund_variable_notice,
                resourceProvider.getString(R.string.multiple_shipping).lowercase(Locale.getDefault())
            )
        } else {
            null
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
        triggerEvent(Event.LaunchUrlInAuthenticatedWebView(selectedSite.get().adminUrlOrDefault))
    }

    private fun showRefundSummary() {
        val productItems = refundItems.value.orEmpty()
        val shippingLines = refundShippingLines.value.takeIf { isShippingMainSwitchChecked.value }.orEmpty()
            .filter { it.isSelected }
        val feeLines = refundFeeLines.value.takeIf { isFeesMainSwitchChecked.value }.orEmpty()
            .filter { it.isSelected }

        triggerEvent(
            IssueRefundEvent.ShowRefundSummary(
                orderId = arguments.orderId,
                refundAmount = viewState.value!!.grandTotalRefund,
                refundItems = productItems + shippingLines + feeLines
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

    data class IssueRefundViewState(
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

    sealed class IssueRefundEvent : Event() {
        data class ShowNumberPicker(val refundItem: ProductRefundListItem) : IssueRefundEvent()
        data class ShowRefundConfirmation(
            val title: String,
            val message: String,
            val confirmButtonTitle: String
        ) : IssueRefundEvent()

        data class ShowRefundSummary(
            val orderId: Long,
            val refundAmount: BigDecimal,
            val refundItems: List<RefundItem>
        ) : IssueRefundEvent()
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
