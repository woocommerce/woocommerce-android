package com.woocommerce.android.ui.payments.changeduecalculator

import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCMaterialOutlinedCurrencyEditTextView
import java.math.BigDecimal

@Composable
fun ChangeDueCalculatorScreen(
    uiState: ChangeDueCalculatorViewModel.UiState,
    onNavigateUp: () -> Unit,
    onCompleteOrderClick: () -> Unit
) {
    val context = LocalContext.current

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
                        stringResource(R.string.loading),
                        style = MaterialTheme.typography.h6
                    )

                    is ChangeDueCalculatorViewModel.UiState.Success -> {
                        val hintString = stringResource(R.string.cash_payments_cash_received)
                        var view: WCMaterialOutlinedCurrencyEditTextView? by remember { mutableStateOf(null) }

                        LaunchedEffect(view) {
                            view?.let {
                                it.requestFocus()
                                context.getSystemService(
                                    InputMethodManager::class.java
                                ).showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }

                        AndroidView(
                            factory = { ctx ->
                                WCMaterialOutlinedCurrencyEditTextView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.WRAP_CONTENT
                                    )
                                    gravity = android.view.Gravity.START
                                    imeOptions = android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
                                    visibility = android.view.View.VISIBLE
                                    supportsEmptyState = false
                                    supportsNegativeValues = false
                                    hint = hintString
                                    setValueIfDifferent(uiState.amountDue)
                                    view = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 8.dp, start = 16.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp, bottom = 16.dp, start = 32.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.cash_payments_change_due),
                                style = LocalTextStyle.current.copy(
                                    fontSize = TextUnit(16f, TextUnitType.Sp)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$0.00",
                                style = LocalTextStyle.current.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = TextUnit(44f, TextUnitType.Sp)
                                ),
                                maxLines = 1
                            )
                        }

                        RecordTransactionDetailsNote(
                            modifier = Modifier
                                .padding(top = 16.dp, bottom = 16.dp, start = 16.dp)
                                .fillMaxWidth()
                        )

                        MarkOrderAsCompleteButton(
                            onClick = onCompleteOrderClick,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }

                    is ChangeDueCalculatorViewModel.UiState.Error -> {
                        Text(
                            text = stringResource(R.string.error_generic),
                            style = MaterialTheme.typography.h6
                        )
                    }
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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
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
fun MarkOrderAsCompleteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    WCColoredButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp)
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
            change = BigDecimal("0.00")
        ),
        onNavigateUp = {},
        onCompleteOrderClick = {}
    )
}
