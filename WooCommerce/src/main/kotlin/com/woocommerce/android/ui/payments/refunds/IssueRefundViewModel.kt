package com.woocommerce.android.ui.payments.refunds

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
import com.woocommerce.android.extensions.calculateTotal
import com.woocommerce.android.extensions.calculateTotals
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
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.InputValidationState.TOO_HIGH
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.InputValidationState.TOO_LOW
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.InputValidationState.VALID
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.HideValidationError
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.OpenUrl
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowNumberPicker
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundConfirmation
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowRefundSummary
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.IssueRefundEvent.ShowValidationError
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.RefundType.AMOUNT
import com.woocommerce.android.ui.payments.refunds.IssueRefundViewModel.RefundType.ITEMS
import com.woocommerce.android.ui.payments.refunds.RefundFeeListAdapter.FeeRefundListItem
import com.woocommerce.android.ui.payments.refunds.RefundProductListAdapter.ProductRefundListItem
import com.woocommerce.android.ui.payments.refunds.RefundShippingListAdapter.ShippingRefundListItem
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
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCGatewayStore
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.set
import kotlin.math.min
import org.wordpress.android.fluxc.utils.sumBy as sumByBigDecimal

@HiltViewModel
@Suppress("LargeClass") // TODO Refactor this class in a follow up PR
class IssueRefundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val dispatchers: CoroutineDispatchers,
    currencyFormatter: CurrencyFormatter,
    private val orderStore: WCOrderStore,
    private val wooStore: WooCommerceStore,
    private val selectedSite: SelectedSite,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider,
    private val orderDetailRepository: OrderDetailRepository,
    private val gatewayStore: WCGatewayStore,
    private val refundStore: WCRefundStore,
    private val paymentChargeRepository: PaymentChargeRepository,
    private val orderMapper: OrderMapper,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
) : ScopedViewModel(savedState) {
    companion object {
        private const val DEFAULT_DECIMAL_PRECISION = 2
        private const val REFUND_METHOD_MANUAL = "manual"
        private const val SELECTED_QUANTITIES_KEY = "selected_quantities_key"
    }

    private val _refundItems = MutableLiveData<List<ProductRefundListItem>>()
    val refundItems: LiveData<List<ProductRefundListItem>> = _refundItems

    private val _refundShippingLines = MutableLiveData<List<ShippingRefundListItem>>()
    val refundShippingLines: LiveData<List<ShippingRefundListItem>> = _refundShippingLines

    private val _refundFeeLines = MutableLiveData<List<FeeRefundListItem>>()
    val refundFeeLines: LiveData<List<FeeRefundListItem>> = _refundFeeLines

    private val areAllItemsSelected: Boolean
        get() = refundItems.value?.all { it.quantity == it.availableRefundQuantity } ?: false

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

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val refundByItemsStateLiveData = LiveDataDelegate(
        savedState,
        RefundByItemsViewState(),
        onChange = { _, new ->
            updateRefundTotal(new.grandTotalRefund)
        }
    )

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val refundByAmountStateLiveData = LiveDataDelegate(
        savedState,
        RefundByAmountViewState(),
        onChange = { _, new ->
            updateRefundTotal(new.enteredAmount)
        }
    )

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    private val productsRefundLiveData = LiveDataDelegate(savedState, ProductsRefundViewState())

    private var commonState by commonStateLiveData
    private var refundByAmountState by refundByAmountStateLiveData
    private var refundByItemsState by refundByItemsStateLiveData
    private var refundSummaryState by refundSummaryStateLiveData
    private var productsRefundState by productsRefundLiveData

    private val order: Order
    private val refunds: List<Refund>
    private val allShippingLineIds: List<Long>
    private val refundableShippingLineIds: List<Long> /* Shipping lines that haven't been refunded */
    private val allFeeLineIds: List<Long>
    private val refundableFeeLineIds: List<Long> /* Fees lines that haven't been refunded */

    private val maxRefund: BigDecimal
    private val maxQuantities: Map<Long, Float>
    private val formatCurrency: (BigDecimal) -> String
    private val gateway: PaymentGateway
    private var cardType = PaymentMethodType.CARD_PRESENT
    private val arguments: RefundsArgs by savedState.navArgs()

    private val selectedQuantities: MutableMap<Long, Int> by lazy {
        val quantities = savedState.get<MutableMap<Long, Int>>(SELECTED_QUANTITIES_KEY) ?: mutableMapOf()
        savedState[SELECTED_QUANTITIES_KEY] = quantities
        quantities
    }

    private var refundJob: Job? = null
    val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    init {
        order = runBlocking { loadOrder(arguments.orderId) }
        allShippingLineIds = order.shippingLines.map { it.itemId }
        allFeeLineIds = order.feesLines.map { it.id }
        refunds = refundStore.getAllRefunds(selectedSite.get(), arguments.orderId).map { it.toAppModel() }
        formatCurrency = currencyFormatter.buildBigDecimalFormatter(order.currency)
        maxRefund = order.total - order.refundTotal
        maxQuantities = refunds.getMaxRefundQuantities(order.items)
            .map { (id, quantity) -> id to quantity }
            .toMap()
        gateway = loadPaymentGateway()
        refundableShippingLineIds = getRefundableShippingLineIds()
        refundableFeeLineIds = getRefundableFeeLineIds()

        initRefundByAmountState()
        initRefundByItemsState()
        initRefundSummaryState()
    }

    private suspend fun loadOrder(orderId: Long): Order =
        requireNotNull(orderStore.getOrderByIdAndSite(orderId, selectedSite.get())?.let { orderMapper.toAppModel(it) })

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
        fun getRefundNotice(): String? {
            val refundOptions = mutableListOf<String>()
            // Inform user that multiple shipping lines can only be refunded in wp-admin.
            if (refundableShippingLineIds.size > 1) {
                val shipping = resourceProvider.getString(R.string.multiple_shipping).lowercase(Locale.getDefault())
                refundOptions.add(shipping)
            }
            return if (refundOptions.isNotEmpty()) {
                val and = resourceProvider.getString(R.string.and).lowercase(Locale.getDefault())
                val options = refundOptions.joinToString(lastSeparator = " $and ")
                return resourceProvider.getString(R.string.order_refunds_shipping_refund_variable_notice, options)
            } else {
                null
            }
        }

        if (refundByItemsStateLiveData.hasInitialValue) {
            refundByItemsState = refundByItemsState.copy(
                currency = order.currency,
                subtotal = formatCurrency(BigDecimal.ZERO),
                taxes = formatCurrency(BigDecimal.ZERO),
                shippingSubtotal = formatCurrency(order.shippingTotal),
                shippingTaxes = formatCurrency(order.shippingLines.sumByBigDecimal { it.totalTax }),
                feesSubtotal = formatCurrency(order.feesTotal),
                feesTaxes = formatCurrency(order.feesLines.sumByBigDecimal { it.totalTax }),
                formattedProductsRefund = formatCurrency(BigDecimal.ZERO),
                formattedShippingRefundTotal = formatCurrency(BigDecimal.ZERO),
                formattedFeesRefundTotal = formatCurrency(BigDecimal.ZERO),
                refundNotice = getRefundNotice(),

                // We only support refunding an Order with one shipping refund for now.
                // In the future, to support multiple shipping refund, we can replace this
                // with refundableShippingLineIds.isNotEmpty()
                isShippingRefundAvailable = refundableShippingLineIds.size == 1,
                isFeesRefundAvailable = refundableFeeLineIds.isNotEmpty(),
            )
        }

        val items = order.items.map {
            val maxQuantity = maxQuantities[it.itemId] ?: 0f
            val selectedQuantity = min(selectedQuantities[it.itemId] ?: 0, maxQuantity.toInt())
            ProductRefundListItem(
                orderItem = it,
                maxQuantity = maxQuantity,
                quantity = selectedQuantity,
                subtotal = formatCurrency(BigDecimal.ZERO),
                taxes = formatCurrency(BigDecimal.ZERO)
            )
        }
        updateRefundItems(items)

        /* Grab all shipping lines listed in the Order, but remove those that are already refunded previously) */
        val shippingLines = order.shippingLines
            .map { ShippingRefundListItem(it) }
            .filter { refundableShippingLineIds.contains(it.shippingLine.itemId) }
        _refundShippingLines.value = shippingLines

        /* Grab all fees lines listed in the Order, but remove those that are already refunded previously) */
        val feeLines = order.feesLines
            .map { FeeRefundListItem(it) }
            .filter { refundableFeeLineIds.contains(it.feeLine.id) }
        _refundFeeLines.value = feeLines

        if (orderContainsOnlyCustomAmounts()) {
            refundByItemsState = refundByItemsState.copy(
                isFeesMainSwitchChecked = true,
                isFeesRefundAvailable = true
            )
        }

        if (productsRefundLiveData.hasInitialValue) {
            val decimals = wooStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
                ?: DEFAULT_DECIMAL_PRECISION

            productsRefundState = productsRefundState.copy(
                currency = order.currency,
                decimals = decimals
            )
        }
    }

    private fun orderContainsOnlyCustomAmounts(): Boolean {
        return order.items.isEmpty() && order.shippingLines.isEmpty() && order.feesLines.isNotEmpty()
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

    private fun loadPaymentGateway(): PaymentGateway {
        val paymentGateway = gatewayStore.getGateway(selectedSite.get(), order.paymentMethod)?.toAppModel()
        return if (paymentGateway != null && paymentGateway.isEnabled) {
            paymentGateway
        } else {
            PaymentGateway(methodTitle = REFUND_METHOD_MANUAL)
        }
    }

    fun onNextButtonTappedFromItems() {
        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_REFUND_TYPE to ITEMS.name,
                AnalyticsTracker.KEY_ORDER_ID to order.id
            )
        )

        showRefundSummary()
    }

    fun onNextButtonTappedFromAmounts() {
        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_REFUND_TYPE to AMOUNT.name,
                AnalyticsTracker.KEY_ORDER_ID to order.id
            )
        )

        if (isInputValid()) {
            showRefundSummary()
        } else {
            showValidationState()
        }
    }

    fun onOpenStoreAdminLinkClicked() {
        triggerEvent(OpenUrl(selectedSite.get().adminUrlOrDefault))
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
                                (commonState.refundTotal isEqualTo maxRefund).toString(),
                            AnalyticsTracker.KEY_REFUND_TYPE to commonState.refundType.name,
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
        val result = async(dispatchers.io) {
            return@async when (commonState.refundType) {
                ITEMS -> {
                    val allItems = mutableListOf<WCRefundItem>()
                    refundItems.value?.let {
                        it.forEach { item -> allItems.add(item.toDataModel()) }
                    }

                    val selectedShipping = refundShippingLines.value?.filter {
                        refundByItemsState.selectedShippingLines
                            ?.contains(it.shippingLine.itemId)
                            ?: false
                    }
                    selectedShipping?.forEach { allItems.add(it.toDataModel()) }

                    val selectedFees = refundFeeLines.value?.filter {
                        refundByItemsState.selectedFeeLines
                            ?.contains(it.feeLine.id)
                            ?: false
                    }
                    selectedFees?.forEach { allItems.add(it.toDataModel()) }

                    refundStore.createItemsRefund(
                        selectedSite.get(),
                        order.id,
                        refundSummaryState.refundReason ?: "",
                        true,
                        gateway.supportsRefunds,
                        items = allItems
                    )
                }
                AMOUNT -> {
                    refundStore.createAmountRefund(
                        selectedSite.get(),
                        order.id,
                        commonState.refundTotal,
                        refundSummaryState.refundReason ?: "",
                        gateway.supportsRefunds
                    )
                }
            }
        }
        return result.await()
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

    fun onRefundIssued(reason: String) {
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
        _refundItems.value?.firstOrNull { it.orderItem.itemId == uniqueId }?.let {
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

    fun onProductsRefundAmountChanged(newAmount: BigDecimal) {
        refundByItemsState = refundByItemsState.copy(
            productsRefund = newAmount,
            formattedProductsRefund = formatCurrency(newAmount)
        )
    }

    fun onRefundQuantityChanged(uniqueId: Long, newQuantity: Int) {
        val newItems = getUpdatedItemList(uniqueId, newQuantity)
        updateRefundItems(newItems)

        selectedQuantities[uniqueId] = newQuantity

        val (subtotal, taxes) = newItems.calculateTotals()
        val productsRefund = min(max(subtotal + taxes, BigDecimal.ZERO), maxRefund)

        val selectButtonTitle = if (areAllItemsSelected) {
            resourceProvider.getString(R.string.order_refunds_items_select_none)
        } else {
            resourceProvider.getString(R.string.order_refunds_items_select_all)
        }

        refundByItemsState = refundByItemsState.copy(
            productsRefund = productsRefund,
            formattedProductsRefund = formatCurrency(productsRefund),
            taxes = formatCurrency(taxes),
            subtotal = formatCurrency(subtotal),
            selectButtonTitle = selectButtonTitle
        )
    }

    private fun getUpdatedItemList(uniqueId: Long, newQuantity: Int): MutableList<ProductRefundListItem> {
        val newItems = mutableListOf<ProductRefundListItem>()
        _refundItems.value?.forEach {
            if (it.orderItem.itemId == uniqueId) {
                // Update the quantity
                var newItem = it.copy(quantity = newQuantity, maxQuantity = maxQuantities[uniqueId] ?: 0f)

                // Update the subtotal and taxes based on the new quantity
                val (subtotal, taxes) = newItem.calculateTotal()
                newItem = newItem.copy(subtotal = formatCurrency(subtotal), taxes = formatCurrency(taxes))

                newItems.add(newItem)
            } else {
                newItems.add(it)
            }
        }
        return newItems
    }

    fun onSelectButtonTapped() {
        if (areAllItemsSelected) {
            _refundItems.value?.forEach {
                onRefundQuantityChanged(it.orderItem.itemId, 0)
            }
        } else {
            _refundItems.value?.forEach {
                onRefundQuantityChanged(it.orderItem.itemId, it.availableRefundQuantity)
            }
        }

        analyticsTrackerWrapper.track(
            CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_ORDER_ID to order.id)
        )
    }

    private fun updateRefundItems(items: List<ProductRefundListItem>) {
        _refundItems.value = items.filter { it.maxQuantity > 0 }

        val selectedItems = items.sumOf { it.quantity }
        refundByItemsState = refundByItemsState.copy(
            selectedItemsHeader = resourceProvider.getString(
                R.string.order_refunds_items_selected,
                selectedItems
            )
        )
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
        if (isChecked) {
            val shippingRefund = calculatePartialShippingTotal(allShippingLineIds)

            refundByItemsState = refundByItemsState.copy(
                shippingRefund = shippingRefund,
                formattedShippingRefundTotal = formatCurrency(shippingRefund),
                isShippingMainSwitchChecked = true,
                selectedShippingLines = allShippingLineIds
            )
        } else {
            refundByItemsState = refundByItemsState.copy(
                shippingRefund = 0.toBigDecimal(),
                formattedShippingRefundTotal = formatCurrency(0.toBigDecimal()),
                isShippingMainSwitchChecked = false,
                selectedShippingLines = emptyList()
            )
        }
    }

    fun onFeesRefundMainSwitchChanged(isChecked: Boolean) {
        if (isChecked) {
            val feesRefund = calculatePartialFeesTotal(allFeeLineIds)

            refundByItemsState = refundByItemsState.copy(
                feesRefund = feesRefund,
                formattedFeesRefundTotal = formatCurrency(feesRefund),
                isFeesMainSwitchChecked = true,
                selectedFeeLines = allFeeLineIds
            )
        } else {
            refundByItemsState = refundByItemsState.copy(
                feesRefund = 0.toBigDecimal(),
                formattedFeesRefundTotal = formatCurrency(0.toBigDecimal()),
                isFeesMainSwitchChecked = false,
                selectedFeeLines = emptyList()
            )
        }
    }

    fun onShippingLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        val list = refundByItemsState.selectedShippingLines?.toMutableList()
        if (list != null) {
            if (isChecked && !list.contains(itemId)) {
                list += itemId
            } else {
                list -= itemId
            }

            refundByItemsState.selectedShippingLines?.filter { it != itemId }

            val newShippingRefundTotal = calculatePartialShippingTotal(list)

            refundByItemsState = refundByItemsState.copy(
                selectedShippingLines = list,
                shippingSubtotal = formatCurrency(calculatePartialShippingSubtotal(list)),
                shippingTaxes = formatCurrency(calculatePartialShippingTaxes(list)),
                shippingRefund = newShippingRefundTotal,
                formattedShippingRefundTotal = formatCurrency(newShippingRefundTotal),
            )
        }
    }

    fun onFeeLineSwitchChanged(isChecked: Boolean, itemId: Long) {
        val list = refundByItemsState.selectedFeeLines?.toMutableList()
        if (list != null) {
            if (isChecked && !list.contains(itemId)) {
                list += itemId
            } else {
                list -= itemId
            }

            refundByItemsState.selectedFeeLines?.filter { it != itemId }

            val newFeesRefundTotal = calculatePartialFeesTotal(list)

            refundByItemsState = refundByItemsState.copy(
                selectedFeeLines = list,
                feesSubtotal = formatCurrency(calculatePartialFeesSubtotal(list)),
                feesTaxes = formatCurrency(calculatePartialFeesTaxes(list)),
                feesRefund = newFeesRefundTotal,
                formattedFeesRefundTotal = formatCurrency(newFeesRefundTotal)
            )
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

    private fun getRefundableShippingLineIds(): List<Long> {
        val availableShippingLines = allShippingLineIds.toMutableList()
        refunds.forEach {
            it.shippingLines.forEach { shippingLine ->
                if (availableShippingLines.contains(shippingLine.itemId)) {
                    availableShippingLines -= shippingLine.itemId
                }
            }
        }
        return availableShippingLines
    }

    private fun getRefundableFeeLineIds(): List<Long> {
        val availableFeeLines = allFeeLineIds.toMutableList()
        refunds.forEach {
            it.feeLines.forEach { feeLine ->
                if (availableFeeLines.contains(feeLine.id)) {
                    availableFeeLines -= feeLine.id
                }
            }
        }
        return availableFeeLines
    }

    private fun calculatePartialShippingSubtotal(selectedShippingLinesId: List<Long>): BigDecimal {
        return order.shippingLines
            .filter { it.itemId in selectedShippingLinesId }
            .sumByBigDecimal { it.total }
    }

    private fun calculatePartialFeesSubtotal(selectedFeeLinesId: List<Long>): BigDecimal {
        return order.feesLines
            .filter { it.id in selectedFeeLinesId }
            .sumByBigDecimal { it.total }
    }

    private fun calculatePartialShippingTaxes(selectedShippingLinesId: List<Long>): BigDecimal {
        return order.shippingLines
            .filter { it.itemId in selectedShippingLinesId }
            .sumByBigDecimal { it.totalTax }
    }

    private fun calculatePartialFeesTaxes(selectedFeeLinesId: List<Long>): BigDecimal {
        return order.feesLines
            .filter { it.id in selectedFeeLinesId }
            .sumByBigDecimal { it.totalTax }
    }

    private fun calculatePartialShippingTotal(selectedShippingLinesId: List<Long>): BigDecimal {
        return calculatePartialShippingSubtotal(selectedShippingLinesId)
            .add(calculatePartialShippingTaxes(selectedShippingLinesId))
    }

    private fun calculatePartialFeesTotal(selectedFeeLinesId: List<Long>): BigDecimal {
        return calculatePartialFeesSubtotal(selectedFeeLinesId)
            .add(calculatePartialFeesTaxes(selectedFeeLinesId))
    }

    private fun prepareTracksEventsDetails(exception: WooException) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to exception.error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to exception.error.message
    )

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
    @Suppress("ForbiddenComment")
    data class RefundByItemsViewState(
        val currency: String? = null,
        val productsRefund: BigDecimal = BigDecimal.ZERO,
        val formattedProductsRefund: String? = null,
        val subtotal: String? = null,
        val taxes: String? = null,
        val feesSubtotal: String? = null,
        val feesTaxes: String? = null,
        val feesRefund: BigDecimal = BigDecimal.ZERO,
        val formattedFeesRefundTotal: String? = null,
        val isFeesRefundAvailable: Boolean? = null,
        val isFeesMainSwitchChecked: Boolean = feesRefund > BigDecimal.ZERO,
        val selectedFeeLines: List<Long>? = null,
        val shippingSubtotal: String? = null,
        val shippingTaxes: String? = null,
        val shippingRefund: BigDecimal = BigDecimal.ZERO,
        val formattedShippingRefundTotal: String? = null,
        val isShippingRefundAvailable: Boolean? = null,
        val isShippingMainSwitchChecked: Boolean = shippingRefund > BigDecimal.ZERO,
        val selectedShippingLines: List<Long>? = null,
        val selectedItemsHeader: String? = null,
        val selectButtonTitle: String? = null,
        val refundNotice: String? = null
    ) : Parcelable {
        val grandTotalRefund: BigDecimal
            get() = max(productsRefund + shippingRefund + feesRefund, BigDecimal.ZERO)

        val isNextButtonEnabled: Boolean
            get() = grandTotalRefund > BigDecimal.ZERO

        // TODO: @Ruttkay Issue: https://github.com/woocommerce/woocommerce-android/issues/6895
        @Suppress("PROPERTY_WONT_BE_SERIALIZED")
        val isRefundNoticeVisible = !refundNotice.isNullOrEmpty()
    }

    @Parcelize
    data class RefundSummaryViewState(
        val isFormEnabled: Boolean? = null,
        val previouslyRefunded: String? = null,
        val refundAmount: String? = null,
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
        val screenTitle: String? = null,
        val refundType: RefundType = ITEMS
    ) : Parcelable

    sealed class IssueRefundEvent : Event() {
        data class ShowValidationError(val message: String) : IssueRefundEvent()
        data class ShowNumberPicker(val refundItem: ProductRefundListItem) : IssueRefundEvent()
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
        data class NavigateToCardReaderScreen(val orderId: Long, val refundAmount: BigDecimal) : IssueRefundEvent()
    }

    enum class PaymentMethodType(val paymentMethodType: String) {
        CARD_PRESENT("card_present"),
        INTERAC_PRESENT("interac_present");

        companion object {
            fun fromValue(paymentMethodType: String?): PaymentMethodType {
                return values().firstOrNull { it.paymentMethodType == paymentMethodType } ?: CARD_PRESENT
            }
        }
    }
}
