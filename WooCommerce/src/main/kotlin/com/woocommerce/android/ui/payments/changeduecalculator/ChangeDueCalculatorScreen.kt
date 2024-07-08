package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.NullableCurrencyTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import org.wordpress.android.fluxc.model.WCSettingsModel
import java.math.BigDecimal

private const val MAX_INPUT_CHARS = 30

@Composable
fun ChangeDueCalculatorScreen(
    uiState: ChangeDueCalculatorViewModel.UiState,
    onNavigateUp: () -> Unit,
    onCompleteOrderClick: () -> Unit,
    onAmountReceivedChanged: (BigDecimal?) -> Unit,
    onRecordTransactionDetailsCheckedChanged: (Boolean) -> Unit
) {
    WooThemeWithBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = uiState.title) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back)
                            )
                        }
                    },
                    backgroundColor = MaterialTheme.colors.surface,
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.Top,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var inputText by remember { mutableStateOf<BigDecimal?>(uiState.amountDue) }

                    LaunchedEffect(uiState.amountReceived) {
                        inputText = uiState.amountReceived
                    }

                    val focusRequester = remember { FocusRequester() }
                    val keyboardController = LocalSoftwareKeyboardController.current

                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }

                    WCOutlinedTypedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        value = inputText,
                        label = stringResource(R.string.cash_payments_cash_received),
                        valueMapper = NullableCurrencyTextFieldValueMapper.create(
                            decimalSeparator = uiState.decimalSeparator,
                            numberOfDecimals = uiState.numberOfDecimals
                        ),
                        onValueChange = { newValue ->
                            if (newValue == null || newValue.toPlainString().length <= MAX_INPUT_CHARS) {
                                inputText = newValue
                                onAmountReceivedChanged(newValue)
                            }
                        },
                        visualTransformation = CurrencyVisualTransformation(
                            uiState.currencySymbol,
                            uiState.currencyPosition
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = stringResource(R.string.cash_payments_change_due),
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = uiState.changeDueText,
                        style = MaterialTheme.typography.h3,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    RecordTransactionDetailsNote(
                        modifier = Modifier.fillMaxWidth(),
                        checked = uiState.recordTransactionDetailsChecked,
                        onCheckedChange = onRecordTransactionDetailsCheckedChanged
                    )

                    MarkOrderAsCompleteButton(
                        loading = uiState.loading,
                        enabled = uiState.canCompleteOrder,
                        onClick = onCompleteOrderClick,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun RecordTransactionDetailsNote(
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.cash_payments_record_transaction_details),
            style = MaterialTheme.typography.body1
        )
        WCSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun MarkOrderAsCompleteButton(
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WCColoredButton(
        onClick = onClick,
        enabled = !loading && enabled,
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
            )
        } else {
            Text(text = stringResource(R.string.cash_payments_mark_order_as_complete))
        }
    }
}

@Composable
@PreviewLightDark
fun ChangeDueCalculatorScreenSuccessPreviewUnchecked() {
    ChangeDueCalculatorScreen(
        uiState = ChangeDueCalculatorViewModel.UiState(
            amountDue = BigDecimal("666.00"),
            change = BigDecimal("0.00"),
            amountReceived = BigDecimal("666.00"),
            loading = false,
            recordTransactionDetailsChecked = false,
            canCompleteOrder = true,
            currencySymbol = "$",
            currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
            decimalSeparator = ".",
            numberOfDecimals = 2
        ),
        onNavigateUp = {},
        onCompleteOrderClick = {},
        onAmountReceivedChanged = {},
        onRecordTransactionDetailsCheckedChanged = {},
    )
}

@Composable
@PreviewLightDark
fun ChangeDueCalculatorScreenSuccessPreviewChecked() {
    ChangeDueCalculatorScreen(
        uiState = ChangeDueCalculatorViewModel.UiState(
            amountDue = BigDecimal("666.00"),
            change = BigDecimal("0.00"),
            amountReceived = BigDecimal("666.00"),
            loading = true,
            recordTransactionDetailsChecked = true,
            canCompleteOrder = true,
            currencySymbol = "€",
            currencyPosition = WCSettingsModel.CurrencyPosition.LEFT_SPACE,
            decimalSeparator = ".",
            numberOfDecimals = 2
        ),
        onNavigateUp = {},
        onCompleteOrderClick = {},
        onAmountReceivedChanged = {},
        onRecordTransactionDetailsCheckedChanged = {},
    )
}

@Composable
@PreviewLightDark
fun ChangeDueCalculatorScreenSuccessPreviewDisabled() {
    ChangeDueCalculatorScreen(
        uiState = ChangeDueCalculatorViewModel.UiState(
            amountDue = BigDecimal("666.00"),
            change = BigDecimal("0.00"),
            amountReceived = BigDecimal("666.00"),
            loading = false,
            recordTransactionDetailsChecked = true,
            canCompleteOrder = false,
            currencySymbol = "€",
            currencyPosition = WCSettingsModel.CurrencyPosition.LEFT,
            decimalSeparator = ".",
            numberOfDecimals = 2
        ),
        onNavigateUp = {},
        onCompleteOrderClick = {},
        onAmountReceivedChanged = {},
        onRecordTransactionDetailsCheckedChanged = {},
    )
}
