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
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import java.math.RoundingMode

@Suppress("LongParameterList")
@HiltViewModel(assistedFactory = WooPosRefundViewModel.Factory::class)
class WooPosRefundViewModel @AssistedInject constructor(
    @Assisted private val orderId: Long,
    private val ordersDataSource: WooPosOrdersDataSource,
    private val retrieveOrderRefunds: WooPosRetrieveOrderRefunds,
    private val getRefundableItems: WooPosGetRefundableItems,
    private val groupRefundItems: WooPosGroupRefundItems,
    private val buildRefundV4LineItems: WooPosBuildRefundV4LineItems,
    private val refundPreview: WooPosRefundPreview,
    private val v4RefundAvailabilityCache: WooPosV4RefundAvailabilityCache,
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
    private var previewJob: Job? = null
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

            // Store settings (currency decimals, tax rounding mode) are only needed to calculate
            // refund totals locally for the v3 fallback. When v4 is available the server returns the
            // totals via the preview/create endpoints, so we defer fetching these settings until the
            // moment we actually fall back to local calculation (see [ensureLocalCalculationSettings]).
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

            showRefundableItems(order, refundableItems, paymentMethodResult.getOrThrow())
        }
    }

    private fun showRefundableItems(
        order: Order,
        refundableItems: List<WooPosRefundableItem>,
        paymentMethod: String,
    ) {
        // The item-selection step does not display aggregate totals, so we build the content without
        // computing them. Totals are resolved on "Continue": from the server (v4 preview) or, on a
        // v4-unavailable store, calculated locally at that point (see [continueToReview]).
        _state.value = buildContentShell(
            order = order,
            refundableItems = refundableItems,
            paymentMethod = paymentMethod,
        )
        viewModelScope.launch {
            analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundFlowStarted)
        }
    }

    private fun buildContentShell(
        order: Order,
        refundableItems: List<WooPosRefundableItem>,
        paymentMethod: String,
        selectedItemIds: Set<String> = refundableItems.map { it.uniqueId }.toSet()
    ): WooPosRefundState.Content {
        val selectedItems = refundableItems.filter { it.uniqueId in selectedItemIds }
        val allItemIds = refundableItems.map { it.uniqueId }.toSet()
        val zero = PriceUtils.formatCurrency(BigDecimal.ZERO, order.currency, currencyFormatter)

        return WooPosRefundState.Content(
            orderId = order.id,
            orderNumber = "#${order.number}",
            currency = order.currency,
            refundableItems = refundableItems,
            selectedItemIds = selectedItemIds,
            allItemsSelected = selectedItemIds.containsAll(allItemIds),
            itemsCount = selectedItems.size,
            subtotal = BigDecimal.ZERO,
            taxes = BigDecimal.ZERO,
            total = BigDecimal.ZERO,
            formattedSubtotal = zero,
            formattedTaxes = zero,
            formattedTotal = zero,
            paymentMethod = paymentMethod,
            step = WooPosRefundState.Content.RefundStep.SelectItems
        )
    }

    /**
     * Fetches the store settings required for local refund calculation (currency decimals and tax
     * rounding mode) unless already cached. Only the v3 fallback path needs these.
     */
    private suspend fun ensureLocalCalculationSettings(): Boolean {
        if (cachedNumberOfDecimalPoints == null && fetchSiteSettings().isFailure) return false
        if (cachedTaxRoundAtSubtotal == null && fetchTaxRoundAtSubtotal().isFailure) return false
        return true
    }

    private fun WooPosRefundState.Content.withLocalTotals(
        order: Order,
        selectedItems: List<WooPosRefundableItem>,
        numberOfDecimalPoints: Int,
        taxRoundAtSubtotal: Boolean,
    ): WooPosRefundState.Content {
        val subtotal = calculateRefundSubtotal(selectedItems, numberOfDecimalPoints)
        val taxes = calculateRefundTax(selectedItems, order, numberOfDecimalPoints, taxRoundAtSubtotal)
        val total = (subtotal + taxes).setScale(numberOfDecimalPoints, RoundingMode.HALF_UP)
        return copy(
            subtotal = subtotal,
            taxes = taxes,
            total = total,
            formattedSubtotal = PriceUtils.formatCurrency(subtotal, currency, currencyFormatter),
            formattedTaxes = PriceUtils.formatCurrency(taxes, currency, currencyFormatter),
            formattedTotal = PriceUtils.formatCurrency(total, currency, currencyFormatter),
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
                val contentState = contentStateBeforeRefund
                if (contentState != null) {
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
        cancelPreview()
        _state.value = WooPosRefundState.Loading
        loadingJob?.cancel()
    }

    private fun handleContentStateEvent(event: WooPosRefundUIEvent, currentState: WooPosRefundState.Content) {
        when (event) {
            is WooPosRefundUIEvent.ItemSelectionToggled -> handleItemSelection(currentState, event.uniqueId)
            WooPosRefundUIEvent.SelectAllToggled -> handleSelectAllToggled(currentState)
            WooPosRefundUIEvent.RetryPreview -> continueToReview(currentState)
            WooPosRefundUIEvent.ContinueToReviewClicked -> continueToReview(currentState)
            WooPosRefundUIEvent.BackToSelectItemsClicked ->
                _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.SelectItems)
            is WooPosRefundUIEvent.OnRefundReasonChanged ->
                _state.value = currentState.copy(refundReason = event.reason)
            WooPosRefundUIEvent.ContinueToConfirmRefundClicked ->
                _state.value = currentState.copy(step = WooPosRefundState.Content.RefundStep.ConfirmRefund)
            WooPosRefundUIEvent.BackToConfirmRefundClicked ->
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

    private fun cancelPreview() {
        previewJob?.cancel()
        previewJob = null
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
        // Toggling selection only updates which items are selected — totals are not shown on the
        // selection step and are resolved on "Continue", so no calculation happens here.
        // A preview in flight for the previous selection is now stale: cancel it and clear the
        // loading/failed flags, otherwise its result is dropped by the stale-selection guard and
        // the Continue button would stay stuck in the loading state.
        cancelPreview()
        val allItemIds = currentState.refundableItems.map { it.uniqueId }.toSet()
        _state.value = currentState.copy(
            selectedItemIds = newSelectedIds,
            allItemsSelected = newSelectedIds.containsAll(allItemIds),
            itemsCount = currentState.refundableItems.count { it.uniqueId in newSelectedIds },
            isPreviewLoading = false,
            previewFailed = false,
        )
    }

    /**
     * Triggered when the user commits their selection by tapping "Continue". Fetches the
     * server-calculated totals (v4) and, on success, advances to the review step showing those
     * authoritative totals. On a v4-unavailable store the locally-calculated totals already in
     * [currentState] stand and we advance immediately. On a preview error the user stays on the
     * selection step with a retry affordance.
     */
    private fun continueToReview(currentState: WooPosRefundState.Content) {
        if (currentState.selectedItemIds.isEmpty()) return
        previewJob?.cancel()

        val siteId = selectedSite.get().siteId
        val selectedItems = currentState.refundableItems.filter { it.uniqueId in currentState.selectedItemIds }

        if (v4RefundAvailabilityCache.isV4Available(siteId) == false) {
            // v4 is known unavailable for this store: skip the probe and resolve totals locally.
            _state.value = currentState.copy(isPreviewLoading = true, previewFailed = false)
            previewJob = viewModelScope.launch {
                advanceToReviewWithLocalTotals(currentState, selectedItems)
            }
            return
        }

        _state.value = currentState.copy(isPreviewLoading = true, previewFailed = false)

        previewJob = viewModelScope.launch {
            val lineItems = buildRefundV4LineItems(selectedItems)
            when (val result = refundPreview(currentState.orderId, lineItems)) {
                is WooPosRefundPreview.Result.ServerCalculated ->
                    setContentIfMatchingSelection(currentState.selectedItemIds) {
                        it.applyServerPreview(result.preview)
                            .copy(step = WooPosRefundState.Content.RefundStep.ReviewRefund)
                    }

                WooPosRefundPreview.Result.FallbackToLocal ->
                    advanceToReviewWithLocalTotals(currentState, selectedItems)

                WooPosRefundPreview.Result.Error ->
                    setContentIfMatchingSelection(currentState.selectedItemIds) {
                        it.copy(isPreviewLoading = false, previewFailed = true)
                    }
            }
        }
    }

    /**
     * Resolves refund totals locally (the v3 path), fetching the required store settings on demand,
     * then advances to the review step. Surfaces a preview failure if the settings can't be fetched.
     */
    private suspend fun advanceToReviewWithLocalTotals(
        currentState: WooPosRefundState.Content,
        selectedItems: List<WooPosRefundableItem>,
    ) {
        val order = currentOrder
        if (order == null || !ensureLocalCalculationSettings()) {
            setContentIfMatchingSelection(currentState.selectedItemIds) {
                it.copy(isPreviewLoading = false, previewFailed = true)
            }
            return
        }
        val numberOfDecimalPoints = checkNotNull(cachedNumberOfDecimalPoints)
        val taxRoundAtSubtotal = checkNotNull(cachedTaxRoundAtSubtotal)
        setContentIfMatchingSelection(currentState.selectedItemIds) {
            it.withLocalTotals(order, selectedItems, numberOfDecimalPoints, taxRoundAtSubtotal)
                .copy(
                    step = WooPosRefundState.Content.RefundStep.ReviewRefund,
                    isPreviewLoading = false,
                    previewFailed = false,
                )
        }
    }

    /**
     * Applies [transform] to the current Content state only if the selection still matches — guards
     * against a stale preview result landing after the user changed the selection.
     */
    private fun setContentIfMatchingSelection(
        expectedSelection: Set<String>,
        transform: (WooPosRefundState.Content) -> WooPosRefundState.Content,
    ) {
        val current = _state.value as? WooPosRefundState.Content ?: return
        if (current.selectedItemIds != expectedSelection) return
        _state.value = transform(current)
    }

    private fun WooPosRefundState.Content.applyServerPreview(
        preview: WCRefundPreview
    ): WooPosRefundState.Content {
        return copy(
            subtotal = preview.subtotal,
            taxes = preview.tax,
            total = preview.total,
            maxRefundable = preview.maxRefundable,
            formattedSubtotal = PriceUtils.formatCurrency(preview.subtotal, currency, currencyFormatter),
            formattedTaxes = PriceUtils.formatCurrency(preview.tax, currency, currencyFormatter),
            formattedTotal = PriceUtils.formatCurrency(preview.total, currency, currencyFormatter),
            isPreviewLoading = false,
            previewFailed = false,
        )
    }

    private fun processRefund(contentState: WooPosRefundState.Content) {
        viewModelScope.launch {
            if (refundJob?.isActive == true || contentState.step.isNonCancelable()) {
                return@launch
            }

            cancelPreview()
            contentStateBeforeRefund = contentState
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

            val selectedItems = contentState.refundableItems.filter { it.uniqueId in contentState.selectedItemIds }
            val request = buildSubmissionRequest(order, contentState, selectedItems) ?: run {
                _state.value = WooPosRefundState.Error(
                    message = resourceProvider.getString(R.string.error_generic),
                    errorType = WooPosRefundState.Error.ErrorType.Processing
                )
                return@launch
            }

            submitRefund(contentState = contentState, request = request)
        }
    }

    /**
     * Builds the submission request. When v4 is available the server computes monetary values, so we
     * send only the simplified line items — no client-calculated amount and no local item grouping
     * (which would require the store's currency settings). Otherwise the v3 request is built from the
     * locally-grouped items, reusing the currency decimals already fetched for the local calculation.
     */
    private fun buildSubmissionRequest(
        order: Order,
        contentState: WooPosRefundState.Content,
        selectedItems: List<WooPosRefundableItem>,
    ): WooPosRefundSubmissionRequest? {
        if (v4RefundAvailabilityCache.isV4Available(selectedSite.get().siteId) == true) {
            return WooPosRefundSubmissionRequest(
                order = order,
                refundAmount = contentState.total,
                refundReason = contentState.refundReason,
                refundItems = emptyList(),
                v4LineItems = buildRefundV4LineItems(selectedItems),
            )
        }

        val numberOfDecimalPoints = cachedNumberOfDecimalPoints
            ?: wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyDecimalNumber
            ?: run {
                WooLog.e(WooLog.T.POS, "WooPosRefund: failed to read currencyDecimalNumber for v3 refund")
                return null
            }
        return WooPosRefundSubmissionRequest(
            order = order,
            refundAmount = contentState.total,
            refundReason = contentState.refundReason,
            refundItems = groupRefundItems(selectedItems, order, numberOfDecimalPoints),
        )
    }

    private fun submitRefund(
        contentState: WooPosRefundState.Content,
        request: WooPosRefundSubmissionRequest,
    ) {
        refundJob?.cancel()
        refundJob = viewModelScope.launch {
            refundSubmissionProcessor.submit(request).collect { submissionState ->
                handleRefundSubmissionState(
                    contentState = contentState,
                    request = request,
                    submissionState = submissionState,
                )
            }
        }
    }

    private suspend fun handleRefundSubmissionState(
        contentState: WooPosRefundState.Content,
        request: WooPosRefundSubmissionRequest,
        submissionState: WooPosRefundSubmissionState,
    ) {
        when (submissionState) {
            WooPosRefundSubmissionState.Processing -> {
                _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.Processing)
            }

            WooPosRefundSubmissionState.PreparingReader -> {
                _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.PreparingReader)
            }

            WooPosRefundSubmissionState.ReaderConnectionRequired -> {
                handleReaderConnectionRequired(contentState, request)
            }

            is WooPosRefundSubmissionState.WaitingForCard -> {
                _state.value = contentState.copy(
                    step = WooPosRefundState.Content.RefundStep.ReadyForRefund(
                        cardReaderHint = submissionState.cardReaderHint,
                        isDismissBlocked = submissionState.isDismissBlocked,
                    )
                )
            }

            WooPosRefundSubmissionState.ProcessingReaderRefund -> {
                _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.ProcessingRefund)
            }

            WooPosRefundSubmissionState.NotifyingStore -> {
                _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.NotifyingStore)
            }

            WooPosRefundSubmissionState.Success -> {
                handleRefundSubmissionSuccess(contentState, request)
            }

            is WooPosRefundSubmissionState.Failure -> {
                handleRefundSubmissionFailure(submissionState)
            }
        }
    }

    private fun handleReaderConnectionRequired(
        contentState: WooPosRefundState.Content,
        request: WooPosRefundSubmissionRequest,
    ) {
        pendingReaderConnectionRefund = PendingReaderConnectionRefund(
            contentState = contentState,
            request = request,
        )
        _state.value = contentState.copy(step = WooPosRefundState.Content.RefundStep.ReaderDisconnected)
        resumePendingRefundIfReaderConnected()
    }

    private suspend fun handleRefundSubmissionSuccess(
        contentState: WooPosRefundState.Content,
        request: WooPosRefundSubmissionRequest,
    ) {
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

    private suspend fun handleRefundSubmissionFailure(
        submissionState: WooPosRefundSubmissionState.Failure,
    ) {
        analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundProcessingFailed)
        _state.value = WooPosRefundState.Error(
            message = submissionState.message,
            errorType = WooPosRefundState.Error.ErrorType.Processing,
            canRetry = submissionState.canRetry,
        )
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
