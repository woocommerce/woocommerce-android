package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewState

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsViewState.PaymentSuccess,
    onNewTransactionClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF98F179)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp.toAdaptivePadding())
                .weight(1f)
                .fillMaxWidth()
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
            Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))
            Text(
                text = stringResource(R.string.woopos_payment_successful_label),
                style = MaterialTheme.typography.h4,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface
            )

            Text(
                text = stringResource(R.string.woopos_success_screen_total, state.orderTotalText),
                style = MaterialTheme.typography.subtitle1,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(24.dp.toAdaptivePadding())
            )

            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp.toAdaptivePadding()))

        OutlinedButton(
            modifier = Modifier
                .padding(24.dp.toAdaptivePadding())
                .width(600.dp.toAdaptivePadding()),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colors.onSurface
            ),
            onClick = onNewTransactionClicked
        ) {
            Icon(
                modifier = Modifier.size(20.dp.toAdaptivePadding()),
                painter = painterResource(id = R.drawable.woo_pos_ic_return_home),
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp.toAdaptivePadding()))
            Text(
                text = stringResource(R.string.woopos_new_order_button),
                style = MaterialTheme.typography.button,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colors.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

@WooPosPreview
@Composable
fun WooPosPaymentSuccessScreenPreview() {
    WooPosTheme {
        WooPosPaymentSuccessScreen(
            state = WooPosTotalsViewState.PaymentSuccess(
                orderSubtotalText = "$11.98",
                orderTotalText = "$13.18",
                orderTaxText = "$1.20"
            ),
            onNewTransactionClicked = {}
        )
    }
}
