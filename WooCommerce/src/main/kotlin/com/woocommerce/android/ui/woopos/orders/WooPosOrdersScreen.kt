package com.woocommerce.android.ui.woopos.orders

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosEmptyScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreenButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosPaginationErrorIndicator
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInput
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToolbar
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIcons
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.rememberRetained
import com.woocommerce.android.ui.woopos.emailreceipt.EMAIL_RECEIPT_SENT
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetails
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsViewModel
import com.woocommerce.android.ui.woopos.orders.details.refund.REFUND_REASON_RESULT_KEY
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosIssueRefundScreen
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundDetailsDialog
import com.woocommerce.android.ui.woopos.orders.list.WooPosOrdersListState
import com.woocommerce.android.ui.woopos.orders.list.WooPosOrdersListViewModel
import com.woocommerce.android.ui.woopos.orders.list.WooPosScreenType
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

val WOO_POS_ORDERS_TOOLBAR_HEIGHT = 56.dp

/**
 * The orders toolbar is shown only when no search is active and no refund is in progress. While a
 * refund is being issued the flow is presented full screen with its own header, so the orders
 * toolbar must stay hidden regardless of single- vs dual-pane mode.
 */
internal fun shouldShowOrdersToolbar(
    searchInputState: WooPosSearchInputState,
    isIssuingRefund: Boolean,
): Boolean = searchInputState is WooPosSearchInputState.Closed && !isIssuingRefund

@Composable
fun WooPosOrdersScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
    backStackEntry: NavBackStackEntry,
) {
    val isPhoneLayout = LocalContext.current.isWooPosPhoneLayout()
    val listViewModel: WooPosOrdersListViewModel = hiltViewModel()
    val detailViewModel: WooPosOrderDetailsViewModel = hiltViewModel()

    val listState by listViewModel.state.collectAsState()
    val detailState by detailViewModel.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val refreshErrorMessage = stringResource(R.string.woopos_orders_details_refresh_error)
    LaunchedEffect(Unit) {
        detailViewModel.refreshFailedEvent.collect {
            snackbarHostState.showSnackbar(refreshErrorMessage)
        }
    }

    val emailReceiptSent = backStackEntry.savedStateHandle
        .getStateFlow(EMAIL_RECEIPT_SENT, false)
        .collectAsState()

    LaunchedEffect(emailReceiptSent.value) {
        if (emailReceiptSent.value) {
            detailViewModel.onBackFromSuccessfullySendingEmailReceipt()
            backStackEntry.savedStateHandle[EMAIL_RECEIPT_SENT] = false
        }
    }

    val refundReasonResult = backStackEntry.savedStateHandle
        .getStateFlow<String?>(REFUND_REASON_RESULT_KEY, null)
        .collectAsState()

    LaunchedEffect(refundReasonResult.value) {
        if (refundReasonResult.value != null) {
            backStackEntry.savedStateHandle.remove<String>(REFUND_REASON_RESULT_KEY)
        }
    }

    var detailPaneIssueRefundOrderId by rememberSaveable { mutableStateOf<Long?>(null) }
    var detailPaneIssueRefundInstanceId by rememberSaveable { mutableIntStateOf(0) }
    var detailPaneIssueRefundDismissRequestToken by rememberSaveable { mutableIntStateOf(0) }
    var detailPaneIssueRefundHasPendingChanges by rememberSaveable { mutableStateOf(false) }
    var pendingOrderSelectionAfterRefundDismiss by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingOrderSelectionConfirmation by rememberSaveable { mutableStateOf<Long?>(null) }
    val detailPaneIssueRefundHandler = remember { WooPosDetailPaneIssueRefundHandler() }
    val handleOrdersUIEvent: (WooPosOrdersUIEvent) -> Unit = { event ->
        val issueRefundAction = (event as? WooPosOrdersUIEvent.OrderActionClicked)
            ?.action as? WooPosOrdersState.OrderAction.IssueRefund
        if (issueRefundAction != null) {
            detailPaneIssueRefundInstanceId += 1
            detailPaneIssueRefundOrderId = issueRefundAction.orderId
            detailPaneIssueRefundDismissRequestToken = 0
            detailPaneIssueRefundHasPendingChanges = false
            pendingOrderSelectionAfterRefundDismiss = null
            pendingOrderSelectionConfirmation = null
        } else {
            detailViewModel.onUIEvent(event)
        }
    }

    val handleIssueRefundDismissed = {
        val action = detailPaneIssueRefundHandler.handleIssueRefundDismissed(
            refundedOrderId = detailPaneIssueRefundOrderId,
            pendingOrderSelectionAfterRefundDismiss = pendingOrderSelectionAfterRefundDismiss,
        )
        detailPaneIssueRefundOrderId = null
        detailPaneIssueRefundHasPendingChanges = false
        detailViewModel.onBackFromIssueRefund(action.refundedOrderId)
        action.orderIdToSelect?.let { orderId ->
            listViewModel.onOrderSelected(orderId, WooPosScreenType.DualPane)
        }
        pendingOrderSelectionAfterRefundDismiss = null
    }

    val handleIssueRefundDismissRequestRejected = {
        pendingOrderSelectionAfterRefundDismiss = null
    }

    val requestRefundDismissBeforeOrderSelection: (Long) -> Unit = { orderId ->
        pendingOrderSelectionAfterRefundDismiss = orderId
        detailPaneIssueRefundDismissRequestToken += 1
    }

    val handleOrderSelected: (Long) -> Unit = if (isPhoneLayout) {
        { orderId ->
            listViewModel.onOrderSelected(orderId, WooPosScreenType.SinglePane)
            onNavigationEvent(WooPosNavigationEvent.OpenOrderDetails(orderId))
        }
    } else {
        { orderId ->
            val action = detailPaneIssueRefundHandler.handleOrderSelected(
                orderId = orderId,
                currentRefundOrderId = detailPaneIssueRefundOrderId,
                hasPendingChanges = detailPaneIssueRefundHasPendingChanges,
            )
            when (action) {
                is WooPosDetailPaneIssueRefundHandler.OrderSelectionAction.SelectOrder ->
                    listViewModel.onOrderSelected(action.orderId, WooPosScreenType.DualPane)
                WooPosDetailPaneIssueRefundHandler.OrderSelectionAction.Ignore -> Unit
                is WooPosDetailPaneIssueRefundHandler.OrderSelectionAction.ConfirmPendingSelection ->
                    pendingOrderSelectionConfirmation = action.orderId
                is WooPosDetailPaneIssueRefundHandler.OrderSelectionAction.RequestRefundDismiss ->
                    requestRefundDismissBeforeOrderSelection(action.orderId)
            }
        }
    }

    WooPosOrdersScreen(
        listState = listState,
        detailState = detailState,
        isSingleOrderMode = detailViewModel.isSingleOrderMode,
        isPhoneLayout = isPhoneLayout,
        snackbarHostState = snackbarHostState,
        scrollToTopEvent = listViewModel.scrollToTopEvent,
        onBackClicked = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
        onRefresh = listViewModel::onRefresh,
        onOrderSelected = handleOrderSelected,
        onEndOfOrdersListReached = listViewModel::onEndOfOrdersListReached,
        onPaginationErrorTryAgain = listViewModel::onPaginationErrorTryAgain,
        onSearchEvent = listViewModel::onSearchEvent,
        onSearchErrorRetry = listViewModel::onSearchErrorRetry,
        onOrdersEmptyActionClicked = listViewModel::onOrdersEmptyActionClicked,
        onOrdersLoadingErrorRetryButtonClicked = listViewModel::onOrdersLoadingErrorRetryButtonClicked,
        onUIEvent = handleOrdersUIEvent,
        onRetryDetailLoad = detailViewModel::retryLoadOrder,
        onRefundDetailsDialogDismissed = detailViewModel::onRefundDetailsDialogDismissed,
        detailPaneIssueRefundOrderId = detailPaneIssueRefundOrderId,
        detailPaneIssueRefundInstanceId = detailPaneIssueRefundInstanceId,
        detailPaneIssueRefundDismissRequestToken = detailPaneIssueRefundDismissRequestToken,
        refundReasonUpdate = refundReasonResult.value,
        onIssueRefundDismissed = handleIssueRefundDismissed,
        onIssueRefundPendingChangesChanged = { detailPaneIssueRefundHasPendingChanges = it },
        onIssueRefundDismissRequestRejected = handleIssueRefundDismissRequestRejected,
        pendingOrderSelectionConfirmation = pendingOrderSelectionConfirmation,
        onPendingOrderSelectionConfirmationDismissed = { pendingOrderSelectionConfirmation = null },
        onPendingOrderSelectionConfirmed = {
            pendingOrderSelectionConfirmation?.let { orderId ->
                pendingOrderSelectionConfirmation = null
                requestRefundDismissBeforeOrderSelection(orderId)
            }
        },
        onNavigationEvent = onNavigationEvent,
    )
}

@Composable
private fun WooPosOrdersScreen(
    listState: WooPosOrdersListState,
    detailState: WooPosOrderDetailsState,
    isSingleOrderMode: Boolean = false,
    isPhoneLayout: Boolean = false,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    scrollToTopEvent: SharedFlow<Unit>,
    onBackClicked: () -> Unit,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onOrdersEmptyActionClicked: () -> Unit,
    onOrdersLoadingErrorRetryButtonClicked: () -> Unit,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    onRefundDetailsDialogDismissed: () -> Unit,
    detailPaneIssueRefundOrderId: Long? = null,
    detailPaneIssueRefundInstanceId: Int = 0,
    detailPaneIssueRefundDismissRequestToken: Int = 0,
    refundReasonUpdate: String? = null,
    onIssueRefundDismissed: () -> Unit = {},
    onIssueRefundPendingChangesChanged: (Boolean) -> Unit = {},
    onIssueRefundDismissRequestRejected: () -> Unit = {},
    pendingOrderSelectionConfirmation: Long? = null,
    onPendingOrderSelectionConfirmationDismissed: () -> Unit = {},
    onPendingOrderSelectionConfirmed: () -> Unit = {},
    onNavigationEvent: (WooPosNavigationEvent) -> Unit = {},
) {
    BackHandler { onBackClicked() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isSingleOrderMode) {
            SingleOrderModeContent(
                detailState = detailState,
                detailPaneIssueRefundOrderId = detailPaneIssueRefundOrderId,
                detailPaneIssueRefundInstanceId = detailPaneIssueRefundInstanceId,
                detailPaneIssueRefundDismissRequestToken = detailPaneIssueRefundDismissRequestToken,
                refundReasonUpdate = refundReasonUpdate,
                onUIEvent = onUIEvent,
                onRetryDetailLoad = onRetryDetailLoad,
                onIssueRefundDismissed = onIssueRefundDismissed,
                onIssueRefundPendingChangesChanged = onIssueRefundPendingChangesChanged,
                onIssueRefundDismissRequestRejected = onIssueRefundDismissRequestRejected,
                onNavigationEvent = onNavigationEvent,
            )
        } else {
            OrdersListModeContent(
                listState = listState,
                detailState = detailState,
                isPhoneLayout = isPhoneLayout,
                detailPaneIssueRefundOrderId = detailPaneIssueRefundOrderId,
                detailPaneIssueRefundInstanceId = detailPaneIssueRefundInstanceId,
                detailPaneIssueRefundDismissRequestToken = detailPaneIssueRefundDismissRequestToken,
                refundReasonUpdate = refundReasonUpdate,
                scrollToTopEvent = scrollToTopEvent,
                onRefresh = onRefresh,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onSearchEvent = onSearchEvent,
                onSearchErrorRetry = onSearchErrorRetry,
                onOrdersEmptyActionClicked = onOrdersEmptyActionClicked,
                onOrdersLoadingErrorRetryButtonClicked = onOrdersLoadingErrorRetryButtonClicked,
                onUIEvent = onUIEvent,
                onRetryDetailLoad = onRetryDetailLoad,
                onIssueRefundDismissed = onIssueRefundDismissed,
                onIssueRefundPendingChangesChanged = onIssueRefundPendingChangesChanged,
                onIssueRefundDismissRequestRejected = onIssueRefundDismissRequestRejected,
                onNavigationEvent = onNavigationEvent,
            )
        }

        if (shouldShowOrdersToolbar(
                searchInputState = listState.searchInputState,
                isIssuingRefund = detailPaneIssueRefundOrderId != null,
            )
        ) {
            val toolbarTitle = if (isSingleOrderMode) {
                val orderNumber = (detailState as? WooPosOrderDetailsState.Loaded)
                    ?.details?.number.orEmpty()
                stringResource(R.string.woopos_order_title, orderNumber)
            } else {
                stringResource(R.string.woopos_orders_title)
            }
            WooPosToolbar(
                titleText = toolbarTitle,
                onBackClicked = onBackClicked,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            )
        }

        val loadedDetailState = detailState as? WooPosOrderDetailsState.Loaded
        if (loadedDetailState != null) {
            OrdersDialogs(
                dialogState = loadedDetailState.dialogState,
                onRefundDetailsDialogDismissed = onRefundDetailsDialogDismissed,
            )
        }

        WooPosDiscardRefundChangesDialog(
            isVisible = pendingOrderSelectionConfirmation != null,
            onDismissRequest = onPendingOrderSelectionConfirmationDismissed,
            onDiscardChanges = onPendingOrderSelectionConfirmed,
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = WooPosSpacing.Medium.value)
        )
    }
}

@Composable
private fun SingleOrderModeContent(
    detailState: WooPosOrderDetailsState,
    detailPaneIssueRefundOrderId: Long?,
    detailPaneIssueRefundInstanceId: Int,
    detailPaneIssueRefundDismissRequestToken: Int,
    refundReasonUpdate: String?,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    onIssueRefundDismissed: () -> Unit,
    onIssueRefundPendingChangesChanged: (Boolean) -> Unit,
    onIssueRefundDismissRequestRejected: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    if (detailPaneIssueRefundOrderId != null) {
        WooPosIssueRefundScreen(
            orderId = detailPaneIssueRefundOrderId,
            onNavigationEvent = onNavigationEvent,
            refundReasonUpdate = refundReasonUpdate,
            onDismissed = onIssueRefundDismissed,
            viewModelKey = "WooPosRefundViewModel:detail-pane:$detailPaneIssueRefundOrderId:" +
                detailPaneIssueRefundInstanceId,
            dismissRequestToken = detailPaneIssueRefundDismissRequestToken,
            onPendingChangesChanged = onIssueRefundPendingChangesChanged,
            onDismissRequestRejected = onIssueRefundDismissRequestRejected,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        when (detailState) {
            is WooPosOrderDetailsState.Loaded -> {
                SingleOrderDetails(detailState = detailState, onUIEvent = onUIEvent)
            }
            is WooPosOrderDetailsState.Loading -> {
                OrderDetailsLoadingPane(
                    showOrderNumber = false,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(
                            start = WooPosSpacing.Medium.value,
                            end = WooPosSpacing.Medium.value,
                            bottom = WooPosSpacing.XLarge.value
                        )
                )
            }
            is WooPosOrderDetailsState.Error -> {
                OrdersError(
                    onRetryClicked = onRetryDetailLoad,
                    modifier = Modifier.statusBarsPadding()
                )
            }
            is WooPosOrderDetailsState.Idle -> {
                // Nothing to show in single order mode when idle
            }
        }
    }
}

@Composable
private fun OrdersListModeContent(
    listState: WooPosOrdersListState,
    detailState: WooPosOrderDetailsState,
    isPhoneLayout: Boolean,
    detailPaneIssueRefundOrderId: Long?,
    detailPaneIssueRefundInstanceId: Int,
    detailPaneIssueRefundDismissRequestToken: Int,
    refundReasonUpdate: String?,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onOrdersEmptyActionClicked: () -> Unit,
    onOrdersLoadingErrorRetryButtonClicked: () -> Unit,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    onIssueRefundDismissed: () -> Unit,
    onIssueRefundPendingChangesChanged: (Boolean) -> Unit,
    onIssueRefundDismissRequestRejected: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    when (listState) {
        is WooPosOrdersListState.Content -> {
            if (isPhoneLayout) {
                OrdersListPane(
                    state = listState,
                    isSelectable = false,
                    scrollToTopEvent = scrollToTopEvent,
                    onRefresh = onRefresh,
                    isRefreshing = listState.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
                    onOrderSelected = onOrderSelected,
                    onEndOfOrdersListReached = onEndOfOrdersListReached,
                    onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                    onSearchEvent = onSearchEvent,
                    onSearchErrorRetry = onSearchErrorRetry,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceBright)
                )
            } else {
                OrdersListWithDetails(
                    listContent = listState,
                    detailState = detailState,
                    detailPaneIssueRefundOrderId = detailPaneIssueRefundOrderId,
                    detailPaneIssueRefundInstanceId = detailPaneIssueRefundInstanceId,
                    detailPaneIssueRefundDismissRequestToken = detailPaneIssueRefundDismissRequestToken,
                    refundReasonUpdate = refundReasonUpdate,
                    scrollToTopEvent = scrollToTopEvent,
                    onRefresh = onRefresh,
                    onOrderSelected = onOrderSelected,
                    onEndOfOrdersListReached = onEndOfOrdersListReached,
                    onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                    onSearchEvent = onSearchEvent,
                    onSearchErrorRetry = onSearchErrorRetry,
                    onUIEvent = onUIEvent,
                    onRetryDetailLoad = onRetryDetailLoad,
                    onIssueRefundDismissed = onIssueRefundDismissed,
                    onIssueRefundPendingChangesChanged = onIssueRefundPendingChangesChanged,
                    onIssueRefundDismissRequestRejected = onIssueRefundDismissRequestRejected,
                    onNavigationEvent = onNavigationEvent,
                )
            }
        }
        is WooPosOrdersListState.Empty -> OrdersEmpty(
            onActionClicked = onOrdersEmptyActionClicked,
            modifier = Modifier.statusBarsPadding()
        )
        is WooPosOrdersListState.Error -> OrdersError(
            onRetryClicked = onOrdersLoadingErrorRetryButtonClicked,
            modifier = Modifier.statusBarsPadding()
        )
        is WooPosOrdersListState.Loading -> WooPosOrdersLoadingScreen(
            isPhoneLayout = isPhoneLayout
        )
    }
}

@Composable
private fun OrdersDialogs(
    dialogState: WooPosOrderDetailsState.DialogState,
    onRefundDetailsDialogDismissed: () -> Unit,
) {
    val retainedDialog = rememberRetained(
        when (dialogState) {
            is WooPosOrderDetailsState.DialogState.RefundDetails -> dialogState
            WooPosOrderDetailsState.DialogState.Hidden -> null
        }
    )

    when (retainedDialog) {
        is WooPosOrderDetailsState.DialogState.RefundDetails -> {
            WooPosRefundDetailsDialog(
                dialogState = retainedDialog,
                isVisible = dialogState is WooPosOrderDetailsState.DialogState.RefundDetails,
                onDismissRequest = onRefundDetailsDialogDismissed,
            )
        }
        WooPosOrderDetailsState.DialogState.Hidden,
        null -> Unit
    }
}

@Composable
private fun WooPosDiscardRefundChangesDialog(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    onDiscardChanges: () -> Unit,
) {
    WooPosDialogWrapper(
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_refund_discard_changes_dialog_background_content_description
        ),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WooPosText(
                text = stringResource(R.string.woopos_refund_discard_changes_title),
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosText(
                text = stringResource(R.string.woopos_refund_discard_changes_message),
                style = WooPosTypography.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.XXXLarge.value))

            WooPosButton(
                text = stringResource(R.string.woopos_refund_discard_changes_discard),
                onClick = onDiscardChanges,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(WooPosSpacing.Medium.value))

            WooPosOutlinedButton(
                text = stringResource(R.string.keep_editing),
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun OrderDetailsPane(
    detailState: WooPosOrderDetailsState,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    modifier: Modifier = Modifier,
    showOrderNumber: Boolean = true,
    foldPrimaryAction: Boolean = false,
) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface)) {
        when (detailState) {
            is WooPosOrderDetailsState.Loaded -> {
                WooPosOrderDetails(
                    modifier = Modifier.fillMaxHeight(),
                    details = detailState.details,
                    showOrderNumber = showOrderNumber,
                    foldPrimaryAction = foldPrimaryAction,
                    onUIEvent = onUIEvent
                )
            }
            is WooPosOrderDetailsState.Loading -> {
                OrderDetailsLoadingPane(
                    modifier = Modifier
                        .fillMaxHeight()
                        .statusBarsPadding()
                        .padding(
                            start = WooPosSpacing.Medium.value,
                            end = WooPosSpacing.Medium.value,
                            top = WooPosSpacing.XLarge.value,
                            bottom = WooPosSpacing.XLarge.value
                        )
                )
            }
            is WooPosOrderDetailsState.Error -> {
                WooPosErrorScreen(
                    modifier = Modifier.fillMaxSize(),
                    message = stringResource(id = R.string.woopos_orders_loading_error_title),
                    reason = detailState.message,
                    primaryButton = WooPosErrorScreenButtonState(
                        text = stringResource(id = R.string.woopos_orders_loading_error_retry_button),
                        click = onRetryDetailLoad
                    )
                )
            }
            is WooPosOrderDetailsState.Idle -> {
                WooPosEmptyScreen(
                    modifier = Modifier.fillMaxSize(),
                    icon = WooPosIcons.OrdersEmpty,
                    title = stringResource(R.string.woopos_orders_no_order_selected),
                    message = "",
                    contentDescription = stringResource(R.string.woopos_orders_empty_list_image_description)
                )
            }
        }
    }
}

@Composable
private fun OrdersListWithDetails(
    listContent: WooPosOrdersListState.Content,
    detailState: WooPosOrderDetailsState,
    detailPaneIssueRefundOrderId: Long?,
    detailPaneIssueRefundInstanceId: Int,
    detailPaneIssueRefundDismissRequestToken: Int,
    refundReasonUpdate: String?,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit,
    onRetryDetailLoad: () -> Unit,
    onIssueRefundDismissed: () -> Unit,
    onIssueRefundPendingChangesChanged: (Boolean) -> Unit,
    onIssueRefundDismissRequestRejected: () -> Unit,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit,
) {
    if (detailPaneIssueRefundOrderId != null) {
        WooPosIssueRefundScreen(
            orderId = detailPaneIssueRefundOrderId,
            onNavigationEvent = onNavigationEvent,
            refundReasonUpdate = refundReasonUpdate,
            onDismissed = onIssueRefundDismissed,
            viewModelKey = "WooPosRefundViewModel:detail-pane:$detailPaneIssueRefundOrderId:" +
                detailPaneIssueRefundInstanceId,
            dismissRequestToken = detailPaneIssueRefundDismissRequestToken,
            onPendingChangesChanged = onIssueRefundPendingChangesChanged,
            onDismissRequestRejected = onIssueRefundDismissRequestRejected,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            OrdersListPane(
                state = listContent,
                scrollToTopEvent = scrollToTopEvent,
                onRefresh = onRefresh,
                isRefreshing = listContent.pullToRefreshState == WooPosPullToRefreshState.Refreshing,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onSearchEvent = onSearchEvent,
                onSearchErrorRetry = onSearchErrorRetry,
                modifier = Modifier
                    .weight(0.3f)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceBright)
            )
            OrderDetailsPane(
                detailState = detailState,
                onUIEvent = onUIEvent,
                onRetryDetailLoad = onRetryDetailLoad,
                showOrderNumber = true,
                modifier = Modifier.weight(0.7f)
            )
        }
    }
}

@Composable
private fun SingleOrderDetails(
    detailState: WooPosOrderDetailsState.Loaded,
    onUIEvent: (WooPosOrdersUIEvent) -> Unit
) {
    OrderDetailsPane(
        detailState = detailState,
        onUIEvent = onUIEvent,
        onRetryDetailLoad = {},
        showOrderNumber = false,
        foldPrimaryAction = true,
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun OrdersListPane(
    state: WooPosOrdersListState.Content,
    scrollToTopEvent: SharedFlow<Unit>,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchEvent: (WooPosSearchUIEvent) -> Unit,
    onSearchErrorRetry: () -> Unit,
    modifier: Modifier = Modifier,
    isSelectable: Boolean = true,
) {
    Column(
        modifier = modifier.statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = WooPosSpacing.Medium.value)
                .heightIn(min = WOO_POS_ORDERS_TOOLBAR_HEIGHT),
        ) {
            WooPosSearchInput(
                state = state.searchInputState,
                searchIconBackgroundColor = MaterialTheme.colorScheme.surface,
                onEvent = onSearchEvent,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }

        Spacer(modifier = Modifier.height(WooPosSpacing.Small.value))

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(
                    pullRefreshState,
                    enabled = state.pullToRefreshState != WooPosPullToRefreshState.Disabled
                )
        ) {
            OrdersList(
                modifier = Modifier.fillMaxSize(),
                state = state,
                isSelectable = isSelectable,
                scrollToTopEvent = scrollToTopEvent,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain,
                onSearchErrorRetry = onSearchErrorRetry,
            )

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = WooPosSpacing.XSmall.value),
                backgroundColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun OrdersList(
    modifier: Modifier = Modifier,
    state: WooPosOrdersListState.Content,
    isSelectable: Boolean = true,
    scrollToTopEvent: SharedFlow<Unit>,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit,
    onSearchErrorRetry: () -> Unit
) {
    when (val items = state.items) {
        is WooPosOrdersListState.Content.Items.Loaded -> {
            LoadedOrdersList(
                modifier = modifier,
                items = items.items,
                isSelectable = isSelectable,
                paginationState = state.paginationState,
                scrollToTopEvent = scrollToTopEvent,
                onOrderSelected = onOrderSelected,
                onEndOfOrdersListReached = onEndOfOrdersListReached,
                onPaginationErrorTryAgain = onPaginationErrorTryAgain
            )
        }

        is WooPosOrdersListState.Content.Items.Searching -> {
            WooPosOrdersListLoadingPane(
                modifier = modifier.imePadding()
            )
        }

        is WooPosOrdersListState.Content.Items.Error -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                WooPosErrorScreen(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = WooPosSpacing.XLarge.value),
                    message = items.title,
                    reason = items.message,
                    primaryButton = WooPosErrorScreenButtonState(
                        text = stringResource(id = R.string.retry),
                        click = onSearchErrorRetry
                    )
                )
            }
        }

        is WooPosOrdersListState.Content.Items.NothingFound -> {
            WooPosEmptyScreen(
                modifier = modifier
                    .imePadding()
                    .padding(horizontal = WooPosSpacing.XLarge.value),
                title = items.title,
                message = items.message,
                contentDescription = stringResource(id = R.string.woopos_search_empty_image_content_description)
            )
        }
    }
}

@Composable
private fun LoadedOrdersList(
    modifier: Modifier = Modifier,
    items: List<WooPosOrdersState.OrderItemViewState>,
    isSelectable: Boolean = true,
    paginationState: WooPosPaginationState,
    scrollToTopEvent: SharedFlow<Unit>,
    onOrderSelected: (Long) -> Unit,
    onEndOfOrdersListReached: () -> Unit,
    onPaginationErrorTryAgain: () -> Unit
) {
    val listState = rememberLazyListState()

    val loadMoreBuffer = 5
    val shouldLoadMore = remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val total = info.totalItemsCount
            if (total == 0) return@derivedStateOf false
            val lastVisible = (info.visibleItemsInfo.lastOrNull()?.index ?: -1)
            lastVisible >= total - 1 - loadMoreBuffer
        }
    }
    LaunchedEffect(paginationState) {
        snapshotFlow { shouldLoadMore.value }
            .distinctUntilChanged()
            .filter { it }
            .collect { onEndOfOrdersListReached() }
    }

    LaunchedEffect(Unit) {
        scrollToTopEvent.collect {
            delay(100)
            listState.animateScrollToItem(0)
        }
    }

    WooPosLazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value),
        state = listState,
    ) {
        items(items, key = { it.id }) { item ->
            WooPosCard(
                modifier = modifier
                    .wrapContentHeight(),
                shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
                backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                elevation = WooPosElevation.Medium,
                shadowType = ShadowType.Soft,
                isSelected = isSelectable && item.isSelected,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOrderSelected(item.id) }
                        .padding(WooPosSpacing.Medium.value),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        WooPosText(
                            item.title,
                            style = WooPosTypography.BodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                        WooPosText(
                            item.date,
                            style = WooPosTypography.BodySmall,
                            color = WooPosTheme.colors.onSurfaceVariantHighest,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(Modifier.height(WooPosSpacing.XSmall.value))

                        val email = item.customerEmail.orEmpty()
                        if (email.isNotBlank()) {
                            WooPosText(
                                email,
                                style = WooPosTypography.BodySmall,
                                color = WooPosTheme.colors.onSurfaceVariantHighest,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(Modifier.height(WooPosSpacing.Small.value))

                        WooPosOrdersStatusBadge(item.status)
                    }

                    WooPosText(
                        text = item.total,
                        style = WooPosTypography.BodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        if (paginationState == WooPosPaginationState.Loading) {
            item {
                WooPosOrdersOrderLoadingRow()
            }
        }

        if (paginationState == WooPosPaginationState.Error) {
            item {
                OrdersPaginationErrorRow(onPaginationErrorTryAgain)
            }
        }
    }
}

@Composable
private fun OrdersEmpty(
    onActionClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosEmptyScreen(
        modifier = modifier.fillMaxSize(),
        icon = WooPosIcons.OrdersEmpty,
        title = stringResource(id = R.string.woopos_orders_empty_list_title),
        message = stringResource(id = R.string.woopos_orders_empty_list_message),
        contentDescription = stringResource(id = R.string.woopos_orders_empty_list_image_description),
        actionLabel = stringResource(id = R.string.woopos_orders_empty_action_label),
        onActionClicked = onActionClicked
    )
}

@Composable
private fun OrdersError(
    onRetryClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WooPosErrorScreen(
        modifier = modifier,
        message = stringResource(id = R.string.woopos_orders_loading_error_title),
        reason = stringResource(id = R.string.woopos_orders_loading_error_message),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_loading_error_retry_button),
            click = onRetryClicked
        )
    )
}

@Composable
private fun OrdersPaginationErrorRow(onPaginationErrorTryAgain: () -> Unit) {
    WooPosPaginationErrorIndicator(
        icon = null,
        message = stringResource(id = R.string.woopos_orders_pagination_error_title),
        description = stringResource(id = R.string.woopos_orders_pagination_error_content_description),
        primaryButton = WooPosErrorScreenButtonState(
            text = stringResource(id = R.string.woopos_orders_pagination_try_again_label),
            click = onPaginationErrorTryAgain
        ),
    )
}

@WooPosPreview
@Composable
fun WooPosOrdersScreenPreview() {
    val item1 = WooPosOrdersState.OrderItemViewState(
        id = 1,
        title = "#014",
        date = "Aug 28, 2025 at 10:31 AM",
        total = "$17.00",
        customerEmail = "johndoe@mail.com",
        isSelected = true,
        status = PosOrderStatus(
            text = "Completed",
            colorKey = OrderStatusColorKey.COMPLETED
        ),
        statusSlug = "Completed",
        createdAtMillis = 1
    )
    val item2 = WooPosOrdersState.OrderItemViewState(
        id = 2,
        title = "#013",
        date = "Jul 28, 2025 at 10:31 AM",
        total = "$43.90",
        customerEmail = "johndoe@mail.com",
        isSelected = false,
        status = PosOrderStatus(
            text = "Processing",
            colorKey = OrderStatusColorKey.PROCESSING
        ),
        statusSlug = "Completed",
        createdAtMillis = 1
    )

    val details1 = sampleOrderDetails(id = 1L, number = "#014")

    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Content(
                items = WooPosOrdersListState.Content.Items.Loaded(
                    items = listOf(item1, item2)
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
                paginationState = WooPosPaginationState.None
            ),
            detailState = WooPosOrderDetailsState.Loaded(
                details = details1,
                dialogState = WooPosOrderDetailsState.DialogState.Hidden
            ),
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onRefundDetailsDialogDismissed = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersSearchErrorStatePreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Content(
                items = WooPosOrdersListState.Content.Items.Error(
                    title = "Search error",
                    message = "Please try again"
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                ),
                paginationState = WooPosPaginationState.None
            ),
            detailState = WooPosOrderDetailsState.Idle,
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onRefundDetailsDialogDismissed = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersNothingFoundStatePreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Content(
                items = WooPosOrdersListState.Content.Items.NothingFound(
                    title = "Nothing found",
                    message = "Try a different search"
                ),
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Open(
                    input = WooPosSearchInputState.Open.Input.Query("test", 4),
                    isLoading = false
                ),
                paginationState = WooPosPaginationState.None
            ),
            detailState = WooPosOrderDetailsState.Idle,
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onRefundDetailsDialogDismissed = {}
        )
    }
}

@WooPosPreview
@Composable
fun WooPosOrdersEmptyStatePreview() {
    WooPosTheme {
        WooPosOrdersScreen(
            listState = WooPosOrdersListState.Empty(
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                searchInputState = WooPosSearchInputState.Closed,
            ),
            detailState = WooPosOrderDetailsState.Idle,
            scrollToTopEvent = MutableSharedFlow(),
            onBackClicked = {},
            onRefresh = {},
            onOrderSelected = {},
            onEndOfOrdersListReached = {},
            onPaginationErrorTryAgain = {},
            onSearchEvent = {},
            onSearchErrorRetry = {},
            onOrdersEmptyActionClicked = {},
            onOrdersLoadingErrorRetryButtonClicked = {},
            onUIEvent = {},
            onRetryDetailLoad = {},
            onRefundDetailsDialogDismissed = {},
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
private fun sampleOrderDetails(
    id: Long = 1L,
    number: String = "#014"
) = WooPosOrdersState.OrderDetailsViewState.Computed.Details(
    id = id,
    number = number,
    dateTime = "Aug 28, 2025 at 10:31 AM",
    customerEmail = "johndoe@mail.com",
    status = PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED),
    lineItems = WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState.Loaded(
        listOf(
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                id = 101,
                name = "Cup",
                attributesDescription = null,
                qtyAndUnitPrice = "1 x $8.50",
                lineTotal = "$15.00",
                imageUrl = null
            ),
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                id = 102,
                name = "Coffee Container",
                attributesDescription = "Blue, Large",
                qtyAndUnitPrice = "1 x $10.00",
                lineTotal = "$8.00",
                imageUrl = null
            ),
            WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow(
                id = 103,
                name = "Paper Filter",
                attributesDescription = null,
                qtyAndUnitPrice = "1 x $4.50",
                lineTotal = "$8.00",
                imageUrl = null
            )
        )
    ),
    refundedLineItems = WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState.Loaded(emptyList()),
    breakdown = WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown(
        products = "$23.00",
        discount = "-$5.00",
        discountCode = "8qew4mnq",
        taxes = "$0.00",
        shipping = null,
        refundsState = WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundsState.Loaded(
            refunds = listOf(
                WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundRow(
                    label = "Refund #1",
                    amount = "-$3.00",
                    date = "Aug 29, 2025 at 12:26 PM",
                    reason = "Customer bought an extra item.",
                ),
                WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundRow(
                    label = "Refund #2",
                    amount = "-$2.00",
                    date = "Aug 30, 2025 at 2:15 PM",
                    reason = null,
                ),
            ),
        ),
        netPayment = "$12.00"
    ),
    total = "$17.00",
    totalPaid = "$17.00",
    paymentMethodTitle = "WooCommerce In-Person Payments",
    actions = listOf(
        WooPosOrdersState.OrderAction.IssueRefund(id),
        WooPosOrdersState.OrderAction.EmailReceipt(id)
    )
)
