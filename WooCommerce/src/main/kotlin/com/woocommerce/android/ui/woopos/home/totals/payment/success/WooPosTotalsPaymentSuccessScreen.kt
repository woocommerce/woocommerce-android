package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
            .padding(24.dp.toAdaptivePadding())
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(WooPosTheme.colors.paymentSuccessBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        CheckMarkIcon()

        Spacer(modifier = Modifier.height(56.dp.toAdaptivePadding()))

        Text(
            text = stringResource(R.string.woopos_payment_successful_label),
            style = MaterialTheme.typography.h4,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Text(
            text = stringResource(R.string.woopos_success_screen_total, state.orderTotalText),
            style = MaterialTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.onSurface,
        )

        Spacer(modifier = Modifier.height(80.dp.toAdaptivePadding()))

        OutlinedButton(
            modifier = Modifier
                .padding(24.dp.toAdaptivePadding())
                .height(80.dp)
                .width(604.dp),
            onClick = onNewTransactionClicked
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.woo_pos_ic_return_home),
                tint = MaterialTheme.colors.onSurface,
                contentDescription = stringResource(id = R.string.woopos_new_order_button)
            )
            Spacer(modifier = Modifier.width(12.dp.toAdaptivePadding()))
            Text(
                text = stringResource(R.string.woopos_new_order_button),
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.SemiBold,
                color = WooPosTheme.colors.paymentSuccessText,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CheckMarkIcon() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(156.dp)
            .shadow(8.dp, CircleShape)
            .background(
                MaterialTheme.colors.background, CircleShape
            )
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            tint = WooPosTheme.colors.success,
            contentDescription = stringResource(id = R.string.woopos_payment_successful_label),
            modifier = Modifier.size(72.dp)
        )
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
