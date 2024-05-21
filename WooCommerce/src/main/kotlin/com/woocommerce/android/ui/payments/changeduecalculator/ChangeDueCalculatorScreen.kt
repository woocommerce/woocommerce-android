package com.woocommerce.android.ui.payments.changeduecalculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import java.math.BigDecimal

@Composable
fun ChangeDueCalculatorScreen(
    uiState: ChangeDueCalculatorViewModel.UiState,
    onNavigateUp: () -> Unit
) {
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
                        onValueChange = { newValue ->
                            if (newValue.isBigDecimalFormat()) {
                                BigDecimal.ZERO
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        label = { Text(stringResource(R.string.cash_payments_cash_received)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )

                    TextField(
                        value = "$0.00",
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        label = { Text(stringResource(R.string.cash_payments_change_due)) },
                        enabled = false,
                        readOnly = true
                    )
                }

                is ChangeDueCalculatorViewModel.UiState.Error -> {
                    Text(text = stringResource(R.string.error_generic), style = MaterialTheme.typography.h6)
                }
            }
        }
    }
}

fun String.isBigDecimalFormat(): Boolean {
    return try {
        BigDecimal(this)
        true
    } catch (ex: NumberFormatException) {
        false
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
