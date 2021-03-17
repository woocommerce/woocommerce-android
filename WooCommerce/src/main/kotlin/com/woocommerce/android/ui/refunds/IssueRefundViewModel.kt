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
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED
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
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.ui.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
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

    private val areAllItemsSelected: Boolean
        get() = refundItems.value?.all { it.quantity == it.maxQuantity } ?: false

    final val commonStateLiveData = LiveDataDelegate(savedState, CommonViewState())
    final val refundSummaryStateLiveData = LiveDataDelegate(savedState, RefundSummaryViewState())
    final val refundByItemsStateLiveData = LiveDataDelegate(savedState, RefundByItemsViewState(), onChange = { _, new ->
        updateRefundTotal(new.productsRefund)
    })
    final val productsRefundLiveData = LiveDataDelegate(savedState, ProductsRefundViewState())

    private var commonState by commonStateLiveData
    private var refundByItemsState by refundByItemsStateLiveData
    private var refundSummaryState by refundSummaryStateLiveData
    private var productsRefundState by productsRefundLiveData

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

    private var refundJob: Job? = null
    final val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    init {
        order = loadOrder(arguments.orderId)
        refunds = refundStore.getAllRefunds(selectedSite.get(), arguments.orderId).map { it.toAppModel() }

        formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
        maxRefund = order.total - order.refundTotal
        maxQuantities = refunds.getMaxRefundQuantities(order.items)
        gateway = loadPaymentGateway()

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

    private fun initRefundByItemsState() {
        if (refundByItemsStateLiveData.hasInitialValue) {
            refundByItemsState = refundByItemsState.copy(
                    currency = order.currency,
                    subtotal = formatCurrency(BigDecimal.ZERO),
                    taxes = formatCurrency(BigDecimal.ZERO),
                    shippingSubtotal = formatCurrency(order.shippingTotal),
                    feesTotal = formatCurrency(order.feesTotal),
                    formattedProductsRefund = formatCurrency(BigDecimal.ZERO),
                    isShippingRefundVisible = order.shippingTotal > BigDecimal.ZERO,
                    isFeesVisible = order.feesTotal > BigDecimal.ZERO,
                    isShippingNoticeVisible = true,
                    isNextButtonEnabled = false
            )
        }

        val items = order.items.map {
            val maxQuantity = maxQuantities[it.uniqueId] ?: 0
            val selectedQuantity = min(selectedQuantities[it.uniqueId] ?: 0, maxQuantity)
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

//    fun onRefundItemsShippingSwitchChanged(isChecked: Boolean) {
//        refundByItemsState = if (isChecked) {
//            refundByItemsState.copy(isShippingRefundVisible = true)
//        } else {
//            refundByItemsState.copy(isShippingRefundVisible = false)
//        }
//    }

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

        refundByItemsState = refundByItemsState.copy(
                productsRefund = productsRefund,
                formattedProductsRefund = formatCurrency(productsRefund),
                taxes = formatCurrency(taxes),
                subtotal = formatCurrency(subtotal),
                isNextButtonEnabled = _refundItems.value?.any { it.quantity > 0 } ?: false,
                selectButtonTitle = selectButtonTitle
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

    enum class RefundType {
        ITEMS
    }

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
        val feesTotal: String? = null,
        val shippingTaxes: String? = null,
        val isShippingRefundVisible: Boolean? = null,
        val isFeesVisible: Boolean? = null,
        val isShippingNoticeVisible: Boolean? = null,
        val selectedItemsHeader: String? = null,
        val selectButtonTitle: String? = null
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
        data class ShowNumberPicker(val refundItem: RefundListItem) : IssueRefundEvent()
        data class ShowRefundConfirmation(
            val title: String,
            val message: String,
            val confirmButtonTitle: String
        ) : IssueRefundEvent()
        data class ShowRefundSummary(val refundType: RefundType) : IssueRefundEvent()
        data class OpenUrl(val url: String) : IssueRefundEvent()
    }

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<IssueRefundViewModel>
}
