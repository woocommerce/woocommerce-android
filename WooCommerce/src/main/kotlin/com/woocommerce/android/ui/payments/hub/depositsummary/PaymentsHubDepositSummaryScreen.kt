package com.woocommerce.android.ui.payments.hub.depositsummary

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.StringUtils

@Composable
fun PaymentsHubDepositSummaryView(
    viewModel: PaymentsHubDepositSummaryViewModel = viewModel()
) {
    viewModel.viewState.observeAsState().let {
        WooThemeWithBackground {
            when (val value = it.value) {
                is PaymentsHubDepositSummaryState.Success -> PaymentsHubDepositSummaryView(value.overview)
                null,
                PaymentsHubDepositSummaryState.Loading,
                is PaymentsHubDepositSummaryState.Error -> {
                    // show nothing
                }
            }
        }
    }
}

@Composable
fun PaymentsHubDepositSummaryView(
    overview: PaymentsHubDepositSummaryState.Overview
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.color_surface))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    style = MaterialTheme.typography.body2,
                    text = stringResource(id = R.string.card_reader_hub_deposit_summary_available_funds),
                    color = colorResource(id = R.color.color_on_surface)
                )
                Text(
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight(700),
                    text = overview.infoPerCurrency[overview.defaultCurrency]?.availableFunds.toString(),
                    color = colorResource(id = R.color.color_on_surface)
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    style = MaterialTheme.typography.body2,
                    text = stringResource(id = R.string.card_reader_hub_deposit_summary_pending_funds),
                    color = colorResource(id = R.color.color_on_surface)
                )
                Text(
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight(700),
                    text = overview.infoPerCurrency[overview.defaultCurrency]?.pendingFunds.toString(),
                    color = colorResource(id = R.color.color_on_surface)
                )
                Text(
                    style = MaterialTheme.typography.caption,
                    text = StringUtils.getQuantityString(
                        context = LocalContext.current,
                        quantity = overview.infoPerCurrency[overview.defaultCurrency]?.pendingBalanceDepositsCount ?: 0,
                        default = R.string.card_reader_hub_deposit_summary_pending_deposits_plural,
                        one = R.string.card_reader_hub_deposit_summary_pending_deposits_one,
                    ),
                    color = colorResource(id = R.color.color_surface_variant)
                )
            }
        }

        Divider(
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PaymentsHubDepositSummaryViewPreview() {
    WooThemeWithBackground {
        PaymentsHubDepositSummaryView(
            PaymentsHubDepositSummaryState.Overview(
                defaultCurrency = "USD",
                infoPerCurrency = mapOf(
                    "USD" to PaymentsHubDepositSummaryState.Info(
                        availableFunds = "100$",
                        pendingFunds = "200$",
                        pendingBalanceDepositsCount = 1,
                        fundsAvailableInDays = PaymentsHubDepositSummaryState.Info.Interval.Days(1),
                        nextDeposit = PaymentsHubDepositSummaryState.Deposit(
                            amount = 100,
                            status = PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED,
                            date = null
                        ),
                        lastDeposit = PaymentsHubDepositSummaryState.Deposit(
                            amount = 100,
                            status = PaymentsHubDepositSummaryState.Deposit.Status.FAILED,
                            date = null
                        )
                    ),
                    "EUR" to PaymentsHubDepositSummaryState.Info(
                        availableFunds = "100$",
                        pendingFunds = "200$",
                        pendingBalanceDepositsCount = 1,
                        fundsAvailableInDays = PaymentsHubDepositSummaryState.Info.Interval.Days(1),
                        nextDeposit = PaymentsHubDepositSummaryState.Deposit(
                            amount = 100,
                            status = PaymentsHubDepositSummaryState.Deposit.Status.ESTIMATED,
                            date = null
                        ),
                        lastDeposit = PaymentsHubDepositSummaryState.Deposit(
                            amount = 100,
                            status = PaymentsHubDepositSummaryState.Deposit.Status.PAID,
                            date = null
                        )
                    )
                )
            )
        )
    }
}
