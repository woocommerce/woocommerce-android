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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.BigDecimalTextFieldValueMapper
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTypedTextField
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.math.BigDecimal

@Composable
fun ChangeDueCalculatorScreen(
    uiState: ChangeDueCalculatorViewModel.UiState,
    recordTransactionDetailsChecked: Boolean,
    onNavigateUp: () -> Unit,
    onCompleteOrderClick: () -> Unit,
    onAmountReceivedChanged: (BigDecimal) -> Unit,
    onRecordTransactionDetailsCheckedChanged: (Boolean) -> Unit
) {
    WooThemeWithBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = getTitleText(uiState)) },
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
                    when (uiState) {
                        is ChangeDueCalculatorViewModel.UiState.Loading -> Text(
                            stringResource(R.string.loading),
                            style = MaterialTheme.typography.h6
                        )

                        is ChangeDueCalculatorViewModel.UiState.Success -> {
                            var inputText by remember { mutableStateOf(uiState.amountReceived) }

                            LaunchedEffect(uiState.amountReceived) {
                                inputText = uiState.amountReceived
                            }

                            val focusRequester = remember { FocusRequester() }
                            val keyboardController = LocalSoftwareKeyboardController.current

                            LaunchedEffect(Unit) {
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(dimensionResource(id = R.dimen.minor_100)),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                WCOutlinedTypedTextField(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                        .focusRequester(focusRequester),
                                    value = inputText,
                                    label = stringResource(R.string.cash_payments_cash_received),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    valueMapper = BigDecimalTextFieldValueMapper.create(supportsNegativeValue = true),
                                    onValueChange = {
                                        inputText = it
                                        onAmountReceivedChanged(android.icu.math.BigDecimal(it).toBigDecimal())
                                    }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.cash_payments_change_due),
                                    style = LocalTextStyle.current.copy(
                                        fontSize = TextUnit(16f, TextUnitType.Sp)
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (uiState.change < BigDecimal.ZERO) {
                                        "-"
                                    } else {
                                        uiState.change.toPlainString()
                                    },
                                    style = LocalTextStyle.current.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = TextUnit(44f, TextUnitType.Sp)
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            RecordTransactionDetailsNote(
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .fillMaxWidth(),
                                checked = recordTransactionDetailsChecked,
                                onCheckedChange = onRecordTransactionDetailsCheckedChanged
                            )

                            MarkOrderAsCompleteButton(
                                onClick = onCompleteOrderClick,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
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
            .clickable { onCheckedChange(!checked) }
            .padding(all = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.cash_payments_record_transaction_details),
            style = LocalTextStyle.current.copy(
                fontSize = TextUnit(16f, TextUnitType.Sp)
            )
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun MarkOrderAsCompleteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    WCColoredButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(text = stringResource(R.string.cash_payments_mark_order_as_complete))
    }
}

@Composable
private fun getTitleText(uiState: ChangeDueCalculatorViewModel.UiState): String {
    return when (uiState) {
        is ChangeDueCalculatorViewModel.UiState.Success -> stringResource(
            R.string.cash_payments_take_payment_title,
            uiState.amountDue
        )

        else -> stringResource(id = R.string.cash_payments_take_payment_title)
    }
}

@Composable
@Preview(showBackground = true)
fun ChangeDueCalculatorScreenSuccessPreview() {
    ChangeDueCalculatorScreen(
        uiState = ChangeDueCalculatorViewModel.UiState.Success(
            amountDue = BigDecimal("666.00"),
            change = BigDecimal("0.00"),
            amountReceived = BigDecimal("0.00")
        ),
        recordTransactionDetailsChecked = false,
        onNavigateUp = {},
        onCompleteOrderClick = {},
        onAmountReceivedChanged = {},
        onRecordTransactionDetailsCheckedChanged = {}
    )
}
