package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.cardreader.connection.CardReaderStatus.Connected
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.cardreader.WooPosCardReaderFacade
import com.woocommerce.android.ui.woopos.common.data.WooPosRetrieveOrderRefunds
import com.woocommerce.android.ui.woopos.orders.WooPosGetPaymentMethod
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersDataSource
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.PriceUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.RoundingMode

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = WooPosRefundViewModel.Factory::class)
class WooPosRefundViewModel @AssistedInject constructor(
    @Assisted private val orderId: Long,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val getRefundableItems: WooPosGetRefundableItems,
    private val groupRefundItems: WooPosGroupRefundItems,
    private val calculateRefundSubtotal: WooPosCalculateRefundSubtotal,
    private val calculateRefundTax: WooPosCalculateRefundTax,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val getPaymentMethod: WooPosGetPaymentMethod,
    private val refundSubmissionProcessor: WooPosRefundSubmissionProcessor,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val cardReaderFacade: WooPosCardReaderFacade,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(orderId: Long): WooPosRefundViewModel
    }

    private val _state = MutableStateFlow<WooPosRefundState>(WooPosRefundState.Loading)
    val state: StateFlow<WooPosRefundState> = _state.asStateFlow()

    private var currentOrder: Order? = null
    private var loadingJob: Job? = null
    private var cachedNumberOfDecimalPoints: Int? = null
    private var cachedTaxRoundAtSubtotal: Boolean? = null
    private var contentStateBeforeRefund: WooPosRefundState.Content? = null
    private var refundJob: Job? = null
    private var backendNotificationRetryRequest: WooPosRefundSubmissionRequest? = null
    private var pendingReaderConnectionRefund: PendingReaderConnectionRefund? = null

    init {
        observeReaderConnectionForPendingRefund()
    }

    private suspend fun fetchSiteSettings(): Result<Int> {
        val siteSettingsResult = wooCommerceStore.fetchSiteGeneralSettings(selectedSite.get())
        if (siteSettingsResult.isError || siteSettingsResult.model == null) {
            WooLog.d(WooLog.T.POS, "Failed to fetch site settings")
            return Result.failure(Exception("Failed to fetch site settings"))
        }
        val siteSettings = checkNotNull(siteSettingsResult.model) {
            "siteSettings.model should not be null after null check"
        }
        cachedNumberOfDecimalPoints = siteSettings.currencyDecimalNumber
        return Result.success(siteSettings.currencyDecimalNumber)
    }

    private suspend fun fetchTaxRoundAtSubtotal(): Result<Boolean> {
        val taxRoundAtSubtotalResult = wooCommerceStore.fetchSiteSettingsTaxRoundAtSubtotal(selectedSite.get())
        if (taxRoundAtSubtotalResult.isError || taxRoundAtSubtotalResult.model == null) {
            WooLog.d(WooLog.T.POS, "Failed to fetch tax round at subtotal setting")
            return Result.failure(Exception("Failed to fetch tax round at subtotal setting"))
        }
        val taxRoundAtSubtotal = checkNotNull(taxRoundAtSubtotalResult.model) {
            "taxRoundAtSubtotalResult.model should not be null after null check"
        }
        cachedTaxRoundAtSubtotal = taxRoundAtSubtotal
        return Result.success(taxRoundAtSubtotal)
    }

    private suspend fun fetchOrderAndRefunds(): Result<Pair<Order, List<Refund>>> {
        val orderResult = ordersDataSource.refreshOrderById(orderId)
        if (orderResult.isFailure) {
            return Result.failure(Exception("Failed to refresh order"))
        }

        val order = orderResult.getOrThrow()
        currentOrder = order

        val refundsResult = retrieveOrderRefunds(order, forceRefresh = true)
        if (refundsResult.isFailure) {
            WooLog.d(WooLog.T.POS, "Failed to fetch refunds for order ${order.id}")
            return Result.failure(Exception("Failed to fetch refunds"))
        }

        val refunds = refundsResult.getOrThrow()
        return Result.success(order to refunds)
    }

    private fun loadRefundableItems() {
        if (_state.value is WooPosRefundState.Content || _state.value is WooPosRefundState.RefundSuccess) return
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _state.value = WooPosRefundState.Loading

            if (fetchSiteSettings().isFailure) {
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic),
                    errorType = WooPosRefundState.Error.ErrorType.Loading
                )
                return@launch
            }

            if (fetchTaxRoundAtSubtotal().isFailure) {
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic),
                    errorType = WooPosRefundState.Error.ErrorType.Loading
                )
                return@launch
            }

            val orderAndRefundsResult = fetchOrderAndRefunds()
            if (orderAndRefundsResult.isFailure) {
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic),
                    errorType = WooPosRefundState.Error.ErrorType.Loading
                )
                return@launch
            }

            val (order, refunds) = orderAndRefundsResult.getOrThrow()
            val refundableItems = getRefundableItems(order, refunds)

            if (refundableItems.isEmpty()) {
                _state.value = WooPosRefundState.NoRefundableItems
                return@launch
            }

            val paymentMethodResult = getPaymentMethod(order)
            if (paymentMethodResult.isFailure) {
                WooLog.e(
                    WooLog.T.POS,
                    "${paymentMethodResult.exceptionOrNull()?.message}"
                )
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic),
                    errorType = WooPosRefundState.Error.ErrorType.Loading
                )
                return@launch
            }

            _state.value = buildContentState(
                order = order,
                refundableItems = refundableItems,
                numberOfDecimalPoints = checkNotNull(cachedNumberOfDecimalPoints) {
                    "cachedNumberOfDecimalPoints should not be null when building content state"
                },
                taxRoundAtSubtotal = checkNotNull(cachedTaxRoundAtSubtotal) {
                    "cachedTaxRoundAtSubtotal should not be null when building content state"
                },
                paymentMethod = paymentMethodResult.getOrThrow()
            )
            analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundFlowStarted)
        }
    }

    private fun buildContentState(
        order: Order,
        refundableItems: List<WooPosRefundableItem>,
        numberOfDecimalPoints: Int,
        taxRoundAtSubtotal: Boolean,
        paymentMethod: String,
        selectedItemIds: Set<String> = refundableItems.map { it.uniqueId }.toSet()
    ): WooPosRefundState.Content {
        val selectedItems = refundableItems.filter { it.uniqueId in selectedItemIds }
        val allItemIds = refundableItems.map { it.uniqueId }.toSet()
        val subtotal = calculateRefundSubtotal(selectedItems, numberOfDecimalPoints)
        val taxes = calculateRefundTax(selectedItems, order, numberOfDecimalPoints, taxRoundAtSubtotal)
        val total = (subtotal + taxes).setScale(numberOfDecimalPoints, RoundingMode.HALF_UP)

        return WooPosRefundState.Content(
            orderId = order.id,
            orderNumber = "#${order.number}",
            currency = order.currency,
            refundableItems = refundableItems,
            selectedItemIds = selectedItemIds,
            allItemsSelected = selectedItemIds.containsAll(allItemIds),
            itemsCount = selectedItems.size,
            subtotal = subtotal,
            taxes = taxes,
            total = total,
            formattedSubtotal = PriceUtils.formatCurrency(subtotal, order.currency, currencyFormatter),
            formattedTaxes = PriceUtils.formatCurrency(taxes, order.currency, currencyFormatter),
            formattedTotal = PriceUtils.formatCurrency(total, order.currency, currencyFormatter),
            paymentMethod = paymentMethod,
            step = WooPosRefundState.Content.RefundStep.SelectItems
        )
    }

    fun onDismissRequest(): Boolean {
        val currentState = _state.value
        val isProcessing = currentState is WooPosRefundState.Content && currentState.step.isNonCancelable()

        // Don't allow dismissal during processing to ensure user sees the result
        return !isProcessing
    }

    fun onUIEvent(event: WooPosRefundUIEvent) {
        when (event) {
            WooPosRefundUIEvent.RefundFlowOpened -> loadRefundableItems()
            WooPosRefundUIEvent.RefundFlowDismissed -> handleRefundFlowDismissed()
            WooPosRefundUIEvent.RetryLoadRefundableItems -> loadRefundableItems()
            WooPosRefundUIEvent.RetryCreateRefund -> {
                val retryRequest = backendNotificationRetryRequest
                val contentState = contentStateBeforeRefund
                if (retryRequest != null && contentState != null) {
                    submitRefund(contentState, retryRequest)
                } else if (contentState != null) {
                    processRefund(contentState)
                } else {
                    WooLog.w(
                        WooLog.T.POS,
                        "WooPosRefund: RetryCreateRefund triggered but contentStateBeforeRefund is null"
                    )
                }
            }
            WooPosRefundUIEvent.CancelRefund -> cancelRefundSubmission()
            else -> {
                val currentState = _state.value as? WooPosRefundState.Content ?: return
                handleContentStateEvent(event, currentState)
            }
        }
    }

    private fun handleRefundFlowDismissed() {
        val currentState = _state.value
        if (currentState is WooPosRefundState.Content &&
            !currentState.step.isNonCancelable()
        ) {
            val refundStep = when (currentState.step) {
                WooPosRefundState.Content.RefundStep.SelectItems -> "select_items"
                WooPosRefundState.Content.RefundStep.ReviewRefund -> "review_refund"
                WooPosRefundState.Content.RefundStep.ConfirmRefund -> "confirm_refund"
                WooPosRefundState.Content.RefundStep.PreparingReader -> "preparing_reader"
                WooPosRefundState.Content.RefundStep.ReaderDisconnected -> "reader_disconnected"
                is WooPosRefundState.Content.RefundStep.ReadyForRefund -> "ready_for_refund"
                WooPosRefundState.Content.RefundStep.Processing,
                WooPosRefundState.Content.RefundStep.ProcessingRefund,
                WooPosRefundState.Content.RefundStep.NotifyingStore ->
                    error("Non-cancelable step should be unreachable in handleRefundFlowDismissed")
            }

            viewModelScope.launch {
                analyticsTracker.track(
                    WooPosAnalyticsEvent.Event.RefundFlowAborted(refundStep = refundStep)
                )
            }
        }

        cancelRefundSubmission()
        _state.value = WooPosRefundState.Loading
        loadingJob?.cancel()
    }

    private fun handleContentStateEvent(event: WooPosRefundUIEvent, currentState: WooPosRefundState.Content) {
        when (event) {
            is WooPosRefundUIEvent.ItemSelectionToggled -> handleItemSelection(currentState, event.uniqueId)
            WooPosRefundUIEvent.SelectAllToggled -> handleSelectAllToggled(currentState)
            WooPosRefundUIEvent.ContinueToReviewClicked -> {
                if (currentState.selectedItemIds.isNotEmpty()) {
                    _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ReviewRefund)
                }
            }
            WooPosRefundUIEvent.BackToSelectItemsClicked ->
                _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.SelectItems)
            is WooPosRefundUIEvent.OnRefundReasonChanged ->
                _state.value = currentState.copy(refundReason = event.reason)
            WooPosRefundUIEvent.ContinueToConfirmRefundClicked ->
                _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ConfirmRefund)
            WooPosRefundUIEvent.BackToReviewClicked ->
                _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ReviewRefund)
            WooPosRefundUIEvent.ConnectReaderClicked -> handleConnectReaderClicked()
            WooPosRefundUIEvent.OnRefundConfirmed -> {
                trackConfirmRefundTapped(currentState)
                processRefund(currentState)
            }
            WooPosRefundUIEvent.RefundFlowDismissed,
            WooPosRefundUIEvent.RefundFlowOpened,
            WooPosRefundUIEvent.RetryLoadRefundableItems,
            WooPosRefundUIEvent.RetryCreateRefund,
            WooPosRefundUIEvent.CancelRefund -> Unit
        }
    }

    private fun handleConnectReaderClicked() {
        resumePendingRefundIfReaderConnected()
    }

    private fun trackConfirmRefundTapped(currentState: WooPosRefundState.Content) {
        val allItemIds = currentState.refundableItems.map { it.uniqueId }.toSet()
        val refundType = if (currentState.selectedItemIds.containsAll(allItemIds)) "full" else "partial"
        val hasReason = currentState.refundReason.isNotBlank()

        viewModelScope.launch {
            analyticsTracker.track(
                WooPosAnalyticsEvent.Event.RefundConfirmTapped(
                    refundType = refundType,
                    hasReason = hasReason
                )
            )
        }
    }

    private fun handleItemSelection(currentState: WooPosRefundState.Content, uniqueId: String) {
        val newSelectedIds = if (uniqueId in currentState.selectedItemIds) {
            currentState.selectedItemIds - uniqueId
        } else {
            currentState.selectedItemIds + uniqueId
        }
        recalculateRefundState(currentState, newSelectedIds)
    }

    private fun handleSelectAllToggled(currentState: WooPosRefundState.Content) {
        val allItemIds = currentState.refundableItems.map { it.uniqueId }.toSet()
        val isDeselecting = currentState.selectedItemIds.containsAll(allItemIds)
        val newSelectedIds = if (isDeselecting) {
            emptySet()
        } else {
            allItemIds
        }
        trackSelectAllToggled(isDeselecting)
        recalculateRefundState(currentState, newSelectedIds)
    }

    private fun trackSelectAllToggled(isDeselecting: Boolean) {
        viewModelScope.launch {
            analyticsTracker.track(
                WooPosAnalyticsEvent.Event.RefundSelectAllTapped(
                    action = if (isDeselecting) "deselected" else "selected"
                )
            )
        }
    }

    private fun recalculateRefundState(currentState: WooPosRefundState.Content, newSelectedIds: Set<String>) {
        val order = checkNotNull(currentOrder) {
            "currentOrder should not be null when recalculating refund state"
        }
        val numberOfDecimalPoints = checkNotNull(cachedNumberOfDecimalPoints) {
            "cachedNumberOfDecimalPoints should not be null when recalculating refund state"
        }
        val taxRoundAtSubtotal = checkNotNull(cachedTaxRoundAtSubtotal) {
            "cachedTaxRoundAtSubtotal should not be null when recalculating refund state"
        }

        _state.value = buildContentState(
            order = order,
            refundableItems = currentState.refundableItems,
            numberOfDecimalPoints = numberOfDecimalPoints,
            taxRoundAtSubtotal = taxRoundAtSubtotal,
            paymentMethod = currentState.paymentMethod,
            selectedItemIds = newSelectedIds
        ).copy(step = currentState.step)
    }

    private fun processRefund(contentState: WooPosRefundState.Content) {
        viewModelScope.launch {
            if (refundJob?.isActive == true || contentState.step.isNonCancelable()) {
                return@launch
            }

            contentStateBeforeRefund = contentState
            backendNotificationRetryRequest = null
            _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.Processing)

            analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundProcessingStarted)

            val order = currentOrder ?: run {
                WooLog.e(
                    WooLog.T.POS,
                    "WooPosRefund: currentOrder is null during processRefund"
                )
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic),
                    errorType = WooPosRefundState.Error.ErrorType.Processing
                )
                return@launch
            }

            val numberOfDecimalPoints =
                wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber ?: run {
                    WooLog.e(
                        WooLog.T.POS,
                        "WooPosRefund: failed to read site settings currencyDecimalNumber from DB"
                    )
                    _state.value = WooPosRefundState.Error(
                        message = resourceProvider.getString(R.string.error_generic),
                        errorType = WooPosRefundState.Error.ErrorType.Processing
                    )
                    return@launch
                }
            val selectedItems = contentState.refundableItems.filter { it.uniqueId in contentState.selectedItemIds }
            val refundItems = groupRefundItems(selectedItems, order, numberOfDecimalPoints)

            submitRefund(
                contentState = contentState,
                request = WooPosRefundSubmissionRequest(
                    order = order,
                    refundAmount = contentState.total,
                    refundReason = contentState.refundReason,
                    refundItems = refundItems,
                )
            )
        }
    }

    @Suppress("LongMethod")
    private fun submitRefund(
        contentState: WooPosRefundState.Content,
        request: WooPosRefundSubmissionRequest,
    ) {
        refundJob?.cancel()
        refundJob = viewModelScope.launch {
            refundSubmissionProcessor.submit(request).collect { submissionState ->
                when (submissionState) {
                    WooPosRefundSubmissionState.Processing -> {
                        _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.Processing)
                    }

                    WooPosRefundSubmissionState.PreparingReader -> {
                        _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.PreparingReader)
                    }

                    WooPosRefundSubmissionState.ReaderConnectionRequired -> {
                        pendingReaderConnectionRefund = PendingReaderConnectionRefund(
                            contentState = contentState,
                            request = request,
                        )
                        _state.value = contentState.copy(
                            step = WooPosRefundState.Content.RefundStep.ReaderDisconnected
                        )
                        resumePendingRefundIfReaderConnected()
                    }

                    is WooPosRefundSubmissionState.WaitingForCard -> {
                        _state.value = contentState.copy(
                            step = WooPosRefundState.Content.RefundStep.ReadyForRefund(
                                submissionState.cardReaderHint
                            )
                        )
                    }

                    WooPosRefundSubmissionState.ProcessingReaderRefund -> {
                        _state.value = contentState.copy(
                            step = WooPosRefundState.Content.RefundStep.ProcessingRefund
                        )
                    }

                    WooPosRefundSubmissionState.NotifyingStore -> {
                        _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.NotifyingStore)
                    }

                    WooPosRefundSubmissionState.Success -> {
                        backendNotificationRetryRequest = null
                        analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundProcessingSuccess)
                        val receiptSentMessage = request.order.billingAddress.email
                            .takeIf { it.isNotBlank() }
                            ?.let { email ->
                                resourceProvider.getString(R.string.woopos_receipt_sent_to_customer, email)
                            }
                        _state.value = WooPosRefundState.RefundSuccess(
                            orderId = contentState.orderId,
                            orderNumber = contentState.orderNumber,
                            refundedAmount = contentState.formattedTotal,
                            paymentMethod = contentState.paymentMethod,
                            receiptSentMessage = receiptSentMessage
                        )
                    }

                    is WooPosRefundSubmissionState.Failure -> {
                        backendNotificationRetryRequest = if (
                            submissionState.retryBackendNotificationOnly &&
                            submissionState.canRetry
                        ) {
                            request.copy(cardRefundAlreadySucceeded = true)
                        } else {
                            null
                        }
                        analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundProcessingFailed)
                        _state.value = WooPosRefundState.Error(
                            message = submissionState.message,
                            errorType = WooPosRefundState.Error.ErrorType.Processing,
                            canRetry = submissionState.canRetry,
                        )
                    }
                }
            }
        }
    }

    private fun cancelRefundSubmission() {
        refundJob?.cancel()
        refundJob = null
        pendingReaderConnectionRefund = null
    }

    private fun observeReaderConnectionForPendingRefund() {
        viewModelScope.launch {
            cardReaderFacade.readerStatus.collect { readerStatus ->
                if (readerStatus is Connected) {
                    resumePendingRefundIfReaderConnected()
                }
            }
        }
    }

    private fun resumePendingRefundIfReaderConnected() {
        if (cardReaderFacade.readerStatus.value !is Connected) return
        val pending = pendingReaderConnectionRefund ?: return
        pendingReaderConnectionRefund = null
        submitRefund(
            contentState = pending.contentState,
            request = pending.request,
        )
    }

    private data class PendingReaderConnectionRefund(
        val contentState: WooPosRefundState.Content,
        val request: WooPosRefundSubmissionRequest,
    )
}
