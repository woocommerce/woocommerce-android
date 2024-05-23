package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ChangeDueCalculatorScreen(
    uiState: ChangeDueCalculatorViewModel.UiState,
    onNavigateUp: () -> Unit
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
                    backgroundColor = colorResource(id = R.color.color_toolbar),
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is ChangeDueCalculatorViewModel.UiState.Loading -> Text(
                        stringResource(
                            R.string.loading,
                        ),
                        style = MaterialTheme.typography.h6
                    )

                    is ChangeDueCalculatorViewModel.UiState.Success -> {
                        OutlinedTextField(
                            value = uiState.amountDue,
                            onValueChange = {
                                // TODO
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp, start = 16.dp),
                            label = { Text(stringResource(R.string.cash_payments_cash_received)) },
                            textStyle = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = TextUnit(44f, TextUnitType.Sp)
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )

                        Text(
                            text = "$0.00", // Your text content
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, bottom = 16.dp, start = 32.dp),
                            style = LocalTextStyle.current.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = TextUnit(44f, TextUnitType.Sp)
                            ),
                            maxLines = 1
                        )

                        RecordTransactionDetailsNote(
                            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp, start = 16.dp).fillMaxWidth()
                        )
                    }

                    is ChangeDueCalculatorViewModel.UiState.Error -> {
                        Text(text = stringResource(R.string.error_generic), style = MaterialTheme.typography.h6)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordTransactionDetailsNote(
    checked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    modifier: Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.cash_payments_record_transaction_details),
            modifier = Modifier.align(Alignment.CenterVertically),
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
@PreviewLightDark()
fun ChangeDueCalculatorScreenSuccessPreview() {
    ChangeDueCalculatorScreen(
        uiState = ChangeDueCalculatorViewModel.UiState.Success(
            amountDue = "$666.00",
            change = 0.00.toBigDecimal()
        ),
        onNavigateUp = {}
    )
}
