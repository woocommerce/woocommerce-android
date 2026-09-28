package com.woocommerce.android.ui.woopos.cashmanagement

import android.content.Context
import android.content.Intent
import com.woocommerce.android.R
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerReason
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButtonSmall
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosToggleButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonState
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCircularLoadingIndicator
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Composable
fun WooPosCashManagementScreen(
    onBack: () -> Unit,
    onOrder: (Long) -> Unit,
    viewModel: WooPosCashManagementViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val drawer = viewModel.drawer
    val lifecycle = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by rememberSaveable { mutableStateOf(CashDialog.NONE) }
    var historySelected by rememberSaveable { mutableStateOf(false) }
    var printError by remember { mutableStateOf<String?>(null) }
    var drawerMessage by remember { mutableStateOf<String?>(null) }
    val printerConnected by drawer.isConnected.collectAsState()

    DisposableEffect(lifecycle, state.detailId, historySelected) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                if (historySelected) viewModel.loadHistory(force = true)
                if (state.detailId != null) viewModel.refreshDetail()
            }
        }
        lifecycle.lifecycle.addObserver(observer)
        onDispose { lifecycle.lifecycle.removeObserver(observer) }
    }

    Row(Modifier.fillMaxSize().padding(WooPosSpacing.Large.value)) {
        Column(Modifier.width(WooPosSpacing.Large.value * 15).fillMaxHeight()
            .then(if (historySelected) Modifier else Modifier.verticalScroll(rememberScrollState())),
            verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
            WooPosOutlinedButtonSmall(text = stringResource(R.string.woopos_cash_back_register), onClick = onBack)
            CashText(stringResource(R.string.woopos_cash_title), style = WooPosTypography.Heading)
            Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                WooPosToggleButton(text = stringResource(R.string.woopos_cash_current), isSelected = !historySelected, onClick = { historySelected = false })
                WooPosToggleButton(text = stringResource(R.string.woopos_cash_past), isSelected = historySelected, onClick = { historySelected = true; viewModel.loadHistory() })
            }
            HorizontalDivider()
            if (historySelected) {
                if (state.loadingHistory && state.history.isEmpty()) CashLoading(stringResource(R.string.woopos_cash_loading_past))
                state.historyError?.let {
                    CashText(it, color = WooPosTheme.colors.alert)
                    WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_retry), onClick = { viewModel.loadHistory(force = true) })
                }
                if (!state.loadingHistory && state.history.isEmpty() && state.historyError == null) {
                    CashText(stringResource(R.string.woopos_cash_no_past))
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                    items(state.history, key = { it.id }) { session ->
                        Column(Modifier.fillMaxWidth().clickable { viewModel.selectSession(session.id) }
                            .padding(WooPosSpacing.Small.value)) {
                            CashText(stringResource(R.string.woopos_cash_session_number, session.id), style = WooPosTypography.BodyMedium)
                            CashText(formatDate(session.dateCreatedGmt))
                            CashText(stringResource(R.string.woopos_cash_expected_value, cash(session.expectedAmount, session)))
                        }
                    }
                    if (state.hasMoreHistory) item {
                        WooPosOutlinedButton(
                            text = stringResource(R.string.woopos_cash_load_more),
                            state = if (state.loadingHistory) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
                            onClick = viewModel::loadMoreHistory,
                        )
                    }
                }
            } else {
                if (state.loadingCurrent && state.current == null) CashLoading(stringResource(R.string.woopos_cash_loading_current))
                state.currentError?.let {
                    CashText(it, color = WooPosTheme.colors.alert)
                    WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_retry), onClick = viewModel::refresh)
                }
                val session = state.current
                if (session == null && !state.loadingCurrent && state.currentError == null) {
                    CashText(stringResource(R.string.woopos_cash_no_current))
                    WooPosButton(text = stringResource(R.string.woopos_cash_start), onClick = { mode = CashDialog.START })
                } else if (session != null) {
                    CashText(stringResource(R.string.woopos_cash_session_number, session.id), style = WooPosTypography.BodyLarge)
                    CashText(stringResource(R.string.woopos_cash_opened_by, formatDate(session.dateCreatedGmt), session.openedByName))
                    session.drawerId?.let { CashText(stringResource(R.string.woopos_cash_session_drawer_name, it)) }
                    CashText(stringResource(R.string.woopos_cash_opening_value, cash(session.openingAmount, session)))
                    CashText(stringResource(R.string.woopos_cash_sales_value, cash(session.cashSalesTotal, session)))
                    CashText(stringResource(R.string.woopos_cash_refunds_value, cash(session.cashRefundsTotal, session)))
                    CashText(stringResource(R.string.woopos_cash_paid_in_value, cash(session.paidInTotal, session)))
                    CashText(stringResource(R.string.woopos_cash_paid_out_value, cash(session.paidOutTotal, session)))
                    CashText(stringResource(R.string.woopos_cash_expected), style = WooPosTypography.BodyMedium)
                    CashText(cash(session.expectedAmount, session), style = WooPosTypography.Heading)
                    WooPosButton(text = stringResource(R.string.woopos_cash_view_activity),
                        onClick = { viewModel.selectSession(session.id) })
                    Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                        WooPosOutlinedButton(
                            text = stringResource(R.string.woopos_cash_pay_in),
                            state = if (state.currentError == null) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
                            onClick = { mode = CashDialog.PAY_IN }
                        )
                        WooPosOutlinedButton(
                            text = stringResource(R.string.woopos_cash_pay_out),
                            state = if (state.currentError == null) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
                            onClick = { mode = CashDialog.PAY_OUT }
                        )
                    }
                    WooPosOutlinedButton(
                        text = stringResource(R.string.woopos_cash_open_drawer),
                        onClick = {
                            scope.launch {
                                drawerMessage = when (drawer.open(WooPosCashDrawerReason.NO_SALE)) {
                                    com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerOpenResult.OPEN_REQUESTED ->
                                        context.getString(R.string.woopos_cash_drawer_requested)
                                    com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerOpenResult.NOT_CONNECTED ->
                                        context.getString(R.string.woopos_cash_printer_disconnected)
                                    com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerOpenResult.NO_SESSION ->
                                        context.getString(R.string.woopos_cash_drawer_no_session)
                                    com.woocommerce.android.ui.woopos.cashdrawer.WooPosCashDrawerOpenResult.FAILED ->
                                        context.getString(R.string.woopos_cash_drawer_failed)
                                }
                            }
                        },
                        state = if (printerConnected && state.currentError == null) WooPosButtonState.ENABLED
                            else WooPosButtonState.DISABLED,
                    )
                    drawerMessage?.let { CashText(it) }
                    WooPosButton(
                        text = stringResource(R.string.woopos_cash_count_close),
                        state = if (state.currentError == null) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
                        onClick = { mode = CashDialog.CLOSE }
                    )
                }
            }
        }
        Spacer(Modifier.width(WooPosSpacing.Large.value))
        VerticalDivider(Modifier.fillMaxHeight())
        Spacer(Modifier.width(WooPosSpacing.Large.value))
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
            val detail = state.detail
            if (state.detailId == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    WooPosText(
                        text = stringResource(R.string.woopos_cash_select_session),
                        style = WooPosTypography.BodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(WooPosSpacing.XLarge.value),
                    )
                }
            } else {
                WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_close_details), onClick = viewModel::clearDetail)
                if (state.loadingDetail && detail == null) CashLoading(stringResource(R.string.woopos_cash_loading_detail))
                state.detailError?.let {
                    CashText(it, color = WooPosTheme.colors.alert)
                    WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_retry), onClick = viewModel::refreshDetail)
                }
                if (detail != null) {
                    CashText(stringResource(R.string.woopos_cash_detail_title, detail.id), style = WooPosTypography.Heading)
                    CashText(if (detail.status == "closed") stringResource(R.string.woopos_cash_closed)
                        else stringResource(R.string.woopos_cash_open))
                    CashText(stringResource(R.string.woopos_cash_opened_by, formatDate(detail.dateCreatedGmt), detail.openedByName))
                    detail.drawerId?.let { CashText(stringResource(R.string.woopos_cash_session_drawer_name, it)) }
                    detail.dateClosedGmt?.let {
                        CashText(stringResource(R.string.woopos_cash_closed_by, formatDate(it), detail.closedByName.orEmpty()))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value)) {
                        Column { CashText(stringResource(R.string.woopos_cash_opening)); CashText(cash(detail.openingAmount, detail)) }
                        Column { CashText(stringResource(R.string.woopos_cash_sales)); CashText(cash(detail.cashSalesTotal, detail)) }
                        Column { CashText(stringResource(R.string.woopos_cash_refunds)); CashText(cash(detail.cashRefundsTotal, detail)) }
                        Column { CashText(stringResource(R.string.woopos_cash_paid_in)); CashText(cash(detail.paidInTotal, detail)) }
                        Column { CashText(stringResource(R.string.woopos_cash_paid_out)); CashText(cash(detail.paidOutTotal, detail)) }
                    }
                    CashText(stringResource(R.string.woopos_cash_expected_value, cash(detail.expectedAmount, detail)),
                        style = WooPosTypography.BodyLarge)
                    detail.countedAmount?.let { CashText(stringResource(R.string.woopos_cash_counted_value, cash(it, detail))) }
                    detail.variance?.let { CashText(stringResource(R.string.woopos_cash_variance_value, cash(it, detail))) }
                    detail.note?.takeIf { it.isNotBlank() }?.let {
                        CashText(stringResource(R.string.woopos_cash_note_value, it))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                        WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_share_csv), onClick = {
                            scope.launch {
                                printError = runCatching {
                                    shareSession(context, detail, viewModel.allMovements(detail.id))
                                }.exceptionOrNull()?.message
                            }
                        })
                        WooPosOutlinedButton(
                            text = stringResource(R.string.woopos_cash_print_summary),
                            onClick = {
                                scope.launch {
                                    printError = runCatching { viewModel.allMovements(detail.id) }
                                        .mapCatching { drawer.printCloseOut(formatCloseOut(detail, it)).getOrThrow() }
                                        .exceptionOrNull()?.message
                                }
                            },
                            state = if (printerConnected) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
                        )
                        WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_refresh), onClick = viewModel::refreshDetail)
                    }
                    printError?.let { CashText(it, color = WooPosTheme.colors.alert) }
                    HorizontalDivider()
                    CashText(stringResource(R.string.woopos_cash_activity), style = WooPosTypography.BodyLarge)
                    if (!state.loadingDetail && state.movements.isEmpty()) CashText(stringResource(R.string.woopos_cash_no_activity))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                        items(state.movements, key = { it.id }) { movement ->
                            Row(Modifier.fillMaxWidth().clickable(enabled = movement.orderId != null) {
                                movement.orderId?.let(onOrder)
                            }.padding(WooPosSpacing.Small.value),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    CashText(movement.type.replace('_', ' ').replaceFirstChar(Char::uppercase))
                                    CashText("${formatDate(movement.occurredAt)} · ${movement.createdByName}")
                                    movement.reason?.takeIf { it.isNotBlank() }?.let { CashText(it) }
                                    movement.orderId?.let {
                                        CashText(stringResource(R.string.woopos_cash_order_link, it))
                                    }
                                }
                                CashText(cash(movement.amount, detail))
                            }
                            HorizontalDivider()
                        }
                        if (state.hasMoreMovements) item {
                            WooPosOutlinedButton(
                                text = stringResource(R.string.woopos_cash_load_more_activity),
                                state = if (state.loadingMovements) WooPosButtonState.LOADING else WooPosButtonState.ENABLED,
                                onClick = viewModel::loadMoreMovements,
                            )
                        }
                    }
                }
            }
        }
    }

    if (mode != CashDialog.NONE) {
        CashEntryDialog(mode, state, onDismiss = { mode = CashDialog.NONE }, onSubmit = { amount, note ->
            when (mode) {
                CashDialog.START -> viewModel.start(amount, drawer.drawerName) { _ ->
                    mode = CashDialog.NONE
                }
                CashDialog.PAY_IN, CashDialog.PAY_OUT -> {
                    val type = if (mode == CashDialog.PAY_IN) "paid_in" else "paid_out"
                    val drawerNameAtCapture = drawer.drawerName
                    viewModel.adjust(type, amount, note.orEmpty()) { sessionId, sessionDrawerId, siteLocalId ->
                        mode = CashDialog.NONE
                        drawer.scheduleAutomaticOpen(
                            WooPosCashDrawerReason.NO_SALE, sessionId,
                            sessionDrawerId, drawerNameAtCapture, siteLocalId = siteLocalId
                        )
                    }
                }
                CashDialog.CLOSE -> viewModel.close(amount, note) {
                    mode = CashDialog.NONE
                    historySelected = true
                }
                CashDialog.NONE -> Unit
            }
        }, onRecount = viewModel::acknowledgeRecount,
            onRetryStartPrecision = viewModel::loadStartPrecision,
            canOpenBeforeStart = drawer.isConnected.collectAsState().value,
            onOpenBeforeStart = { scope.launch { drawer.openBeforeSession() } })
    }
}

private enum class CashDialog { NONE, START, PAY_IN, PAY_OUT, CLOSE }

@Composable
private fun CashEntryDialog(
    mode: CashDialog,
    state: WooPosCashManagementState,
    onDismiss: () -> Unit,
    onSubmit: (BigDecimal, String?) -> Unit,
    onRecount: () -> Unit,
    onRetryStartPrecision: () -> Unit,
    canOpenBeforeStart: Boolean,
    onOpenBeforeStart: () -> Unit,
) {
    var amountText by rememberSaveable(mode) { mutableStateOf("") }
    var note by rememberSaveable(mode) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val session = state.current
    val amount = amountText.replace(',', '.').toBigDecimalOrNull()
    val precision = session?.currencyPrecision ?: state.startPrecision
    val validScale = amount?.let { value -> precision?.let { cashAmountFitsPrecision(value, it) } } == true
    val validAmount = amount != null && validScale && amount >= BigDecimal.ZERO &&
        (mode == CashDialog.START || mode == CashDialog.CLOSE || amount > BigDecimal.ZERO) &&
        (mode != CashDialog.PAY_OUT || (session != null && amount <= session.expectedCash))
    val variance = if (mode == CashDialog.CLOSE && amount != null && session != null) {
        amount - session.expectedCash
    } else null
    val title = when (mode) {
        CashDialog.START -> stringResource(R.string.woopos_cash_start)
        CashDialog.PAY_IN -> stringResource(R.string.woopos_cash_pay_cash_in)
        CashDialog.PAY_OUT -> stringResource(R.string.woopos_cash_pay_cash_out)
        CashDialog.CLOSE -> stringResource(R.string.woopos_cash_count_close_session)
        CashDialog.NONE -> ""
    }
    LaunchedEffect(mode) { focus.requestFocus() }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxSize().imePadding().padding(WooPosSpacing.Large.value),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)
            ) {
                WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_back_management), onClick = onDismiss)
                CashText(title, style = WooPosTypography.Heading)
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Large.value)) {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
                        if (mode == CashDialog.START) {
                            CashText(stringResource(R.string.woopos_cash_count_start_hint))
                            if (precision == null && state.startPrecisionError == null) {
                                CashLoading(stringResource(R.string.woopos_cash_loading_currency_settings))
                            }
                            state.startPrecisionError?.let {
                                CashText(it, color = WooPosTheme.colors.alert)
                                WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_retry),
                                    onClick = onRetryStartPrecision)
                            }
                            WooPosOutlinedButton(
                                text = stringResource(R.string.woopos_cash_open_to_count),
                                state = if (canOpenBeforeStart) WooPosButtonState.ENABLED else WooPosButtonState.DISABLED,
                                onClick = onOpenBeforeStart,
                            )
                            if (!canOpenBeforeStart) CashText(stringResource(R.string.woopos_cash_no_printer))
                        }
                        if (session != null) {
                            CashText(stringResource(R.string.woopos_cash_session_number, session.id), style = WooPosTypography.BodyLarge)
                            session.drawerId?.let { CashText(stringResource(R.string.woopos_cash_session_drawer_name, it)) }
                            CashText(stringResource(R.string.woopos_cash_opening_value, cash(session.openingAmount, session)))
                            CashText(stringResource(R.string.woopos_cash_sales_value, cash(session.cashSalesTotal, session)))
                            CashText(stringResource(R.string.woopos_cash_refunds_value, cash(session.cashRefundsTotal, session)))
                            CashText(stringResource(R.string.woopos_cash_paid_in_value, cash(session.paidInTotal, session)))
                            CashText(stringResource(R.string.woopos_cash_paid_out_value, cash(session.paidOutTotal, session)))
                            CashText(stringResource(R.string.woopos_cash_expected_value, cash(session.expectedAmount, session)),
                                style = WooPosTypography.BodyLarge)
                        }
                    }
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value)) {
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = { amountText = it },
                            label = { CashText(if (mode == CashDialog.CLOSE)
                                stringResource(R.string.woopos_cash_counted_cash)
                                else stringResource(R.string.woopos_cash_amount)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.focusRequester(focus).fillMaxWidth(),
                            singleLine = true,
                        )
                        if (amountText.isNotEmpty() && precision != null && !validAmount) CashText(
                            if (mode == CashDialog.PAY_OUT && amount != null && session != null &&
                                amount > session.expectedCash
                            ) stringResource(R.string.woopos_cash_payout_limit)
                            else stringResource(R.string.woopos_cash_amount_invalid, precision),
                            color = WooPosTheme.colors.alert
                        )
                        if (mode != CashDialog.START) OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { CashText(if (mode == CashDialog.CLOSE)
                                stringResource(R.string.woopos_cash_note_optional)
                                else stringResource(R.string.woopos_cash_reason_optional)) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (session != null) variance?.let {
                            CashText(stringResource(R.string.woopos_cash_variance_value, cash(it.toPlainString(), session)),
                                style = WooPosTypography.BodyLarge)
                        }
                        if (state.recountRequired) CashText(
                            stringResource(R.string.woopos_cash_recount_required),
                            color = WooPosTheme.colors.alert
                        )
                        state.operationError?.let { CashText(it, color = WooPosTheme.colors.alert) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value)) {
                    WooPosOutlinedButton(text = stringResource(R.string.woopos_cash_cancel), onClick = onDismiss)
                    WooPosButton(
                        text = if (state.recountRequired) stringResource(R.string.woopos_cash_count_again)
                            else stringResource(R.string.woopos_cash_confirm),
                        onClick = {
                            if (state.recountRequired) {
                                amountText = ""
                                onRecount()
                            } else if (amount != null) onSubmit(amount, note)
                        },
                        state = if (state.saving) WooPosButtonState.LOADING
                            else if (state.recountRequired || validAmount) WooPosButtonState.ENABLED
                            else WooPosButtonState.DISABLED,
                    )
                }
            }
        }
    }
}

private fun cash(amount: String, session: WooPosCashSession): String {
    return runCatching {
        val value = amount.toBigDecimal().setScale(session.currencyPrecision, RoundingMode.HALF_UP)
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = Currency.getInstance(session.currency)
            minimumFractionDigits = session.currencyPrecision
            maximumFractionDigits = session.currencyPrecision
        }.format(value)
    }.getOrDefault("${session.currency} $amount")
}

private fun shareSession(context: android.content.Context, session: WooPosCashSession, movements: List<WooPosCashMovement>) {
    val csv = buildString {
        appendLine("Session,${session.id}")
        appendLine("Currency,${session.currency}")
        appendLine("Opened,${session.dateCreatedGmt},${session.openedByName.csv()}")
        appendLine("Closed,${session.dateClosedGmt.orEmpty()},${session.closedByName.orEmpty().csv()}")
        appendLine("Opening,${session.openingAmount}")
        appendLine("Sales,${session.cashSalesTotal}")
        appendLine("Refunds,${session.cashRefundsTotal}")
        appendLine("Paid in,${session.paidInTotal}")
        appendLine("Paid out,${session.paidOutTotal}")
        appendLine("Expected,${session.expectedAmount}")
        appendLine("Counted,${session.countedAmount.orEmpty()}")
        appendLine("Variance,${session.variance.orEmpty()}")
        appendLine("Note,${session.note.orEmpty().csv()}")
        appendLine("Type,Amount,Time,Actor,Order,Refund,Reason")
        movements.forEach { movement ->
            appendLine("${movement.type},${movement.amount},${movement.occurredAt}," +
                "${movement.createdByName.csv()},${movement.orderId ?: ""},${movement.refundId ?: ""}," +
                movement.reason.orEmpty().csv())
        }
    }
    val directory = File(context.filesDir, "cash-sessions").apply { mkdirs() }
    val file = File(directory, "cash-session-${session.id}.csv").apply { writeText(csv, Charsets.UTF_8) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.woopos_cash_detail_title, session.id))
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, context.getString(R.string.woopos_cash_share_chooser)))
}

private fun String.csv(): String = "\"${replace("\"", "\"\"")}\""

private fun formatCloseOut(session: WooPosCashSession, movements: List<WooPosCashMovement>): String = buildString {
    appendLine("Cash session #${session.id}")
    appendLine("Opened: ${formatDate(session.dateCreatedGmt)}")
    appendLine("By: ${session.openedByName}")
    appendLine("Closed: ${session.dateClosedGmt?.let(::formatDate).orEmpty()}")
    appendLine("By: ${session.closedByName.orEmpty()}")
    appendLine("Opening: ${cash(session.openingAmount, session)}")
    appendLine("Sales: ${cash(session.cashSalesTotal, session)}")
    appendLine("Refunds: ${cash(session.cashRefundsTotal, session)}")
    appendLine("Paid in: ${cash(session.paidInTotal, session)}")
    appendLine("Paid out: ${cash(session.paidOutTotal, session)}")
    appendLine("Expected: ${cash(session.expectedAmount, session)}")
    appendLine("Counted: ${session.countedAmount?.let { cash(it, session) }.orEmpty()}")
    appendLine("Variance: ${session.variance?.let { cash(it, session) }.orEmpty()}")
    session.note?.takeIf { it.isNotBlank() }?.let { appendLine("Note: $it") }
    appendLine("Activity")
    movements.forEach {
        appendLine("${it.type.replace('_', ' ')} ${it.amount} #${it.orderId ?: ""} ${it.createdByName}")
    }
}

private fun formatDate(value: String): String = runCatching {
    val instant = runCatching { java.time.OffsetDateTime.parse(value).toInstant() }
        .getOrElse { java.time.LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC) }
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
        .withZone(java.time.ZoneId.systemDefault()).format(instant)
}.getOrDefault(value)

@Composable
private fun CashLoading(message: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        modifier = Modifier.padding(vertical = WooPosSpacing.Large.value),
    ) {
        WooPosCircularLoadingIndicator()
        CashText(message, style = WooPosTypography.BodyMedium)
    }
}

@Composable
private fun CashText(
    text: String,
    style: WooPosTypography = WooPosTypography.BodySmall,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    WooPosText(text = text, style = style, color = color, modifier = modifier)
}
