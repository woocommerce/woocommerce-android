package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsState

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsState.PaymentSuccess,
    onNewTransactionClicked: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(
                    top = 24.dp.toAdaptivePadding(),
                    start = 24.dp.toAdaptivePadding(),
                    end = 24.dp.toAdaptivePadding(),
                    bottom = 0.dp
                )
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(16.dp),
                )
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(152, 241, 121, 0),
                            Color(152, 241, 121, 0x1A)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp),
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Icon(
                painter = painterResource(id = R.drawable.woo_pos_ic_payment_success),
                tint = Color.Unspecified,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.height(32.dp.toAdaptivePadding()))
            Text(
                text = stringResource(R.string.woopos_payment_successful_label),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF004D40)
            )

            Spacer(modifier = Modifier.height(32.dp.toAdaptivePadding()))

            TotalsSummary(state)

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp.toAdaptivePadding()))

        OutlinedButton(
            modifier = Modifier
                .padding(
                    top = 0.dp.toAdaptivePadding(),
                    start = 24.dp.toAdaptivePadding(),
                    end = 24.dp.toAdaptivePadding(),
                    bottom = 24.dp.toAdaptivePadding(),
                )
                .fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colors.onSurface,
            ),
            onClick = onNewTransactionClicked
        ) {
            Row(
                modifier = Modifier.padding(8.dp.toAdaptivePadding()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painterResource(id = R.drawable.woo_pos_ic_return_home),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(12.dp.toAdaptivePadding()))
                Text(
                    text = stringResource(R.string.woopos_new_transaction_button),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TotalsSummary(state: WooPosTotalsState.PaymentSuccess) {
    Column(
        modifier = Modifier
            .border(
                width = (0.5).dp,
                color = WooPosTheme.colors.border,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(24.dp.toAdaptivePadding())
            .width(380.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.woopos_payment_subtotal_label),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = state.orderSubtotalText,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Divider(color = WooPosTheme.colors.border, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.woopos_payment_tax_label),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Normal
            )
            Text(
                text = state.orderTaxText,
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Normal
            )
        }

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Divider(color = WooPosTheme.colors.border, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.woopos_payment_total_label),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = state.orderTotalText,
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3C2861)
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosPaymentSuccessScreen(
            state = WooPosTotalsState.PaymentSuccess(
                orderSubtotalText = "$11.98",
                orderTotalText = "$13.18",
                orderTaxText = "$1.20"
            ),
            onNewTransactionClicked = {}
        )
    }
}
