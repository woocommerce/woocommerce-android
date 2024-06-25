package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsState

@Composable
fun WooPosPaymentSuccessScreen(
    state: WooPosTotalsState.PaymentSuccess,
    onNewTransactionClicked: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0x0098F179),
                                Color(0x1A98F179)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.woo_pos_ic_payment_success),
                    tint = Color.Unspecified,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.woopos_payment_successful_label),
                    style = TextStyle(
                        fontSize = 48.sp,
                        fontWeight = FontWeight(700),
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colors.onSurface,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Subtotal: ${state.orderSubtotalText}",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight(700),
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colors.onSurface,
                )
                Text(
                    text = "Tax: ${state.orderTaxText}",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight(700),
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colors.onSurface,
                )
                Text(
                    text = "Total: ${state.orderTotalText}",
                    style = TextStyle(
                        fontSize = 28.sp,
                        fontWeight = FontWeight(700),
                        textAlign = TextAlign.Center,
                    ),
                    color = MaterialTheme.colors.onSurface,
                )
            }
            OutlinedButton(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colors.onSurface,
                ),
                onClick = onNewTransactionClicked
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        modifier = Modifier.width(29.dp).height(29.dp),
                        painter = painterResource(id = R.drawable.woo_pos_ic_return_home),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(id = R.string.new_transaction_button),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosPaymentSuccessScreenPreview() {
    WooPosPaymentSuccessScreen(
        state = WooPosTotalsState.PaymentSuccess(
            orderSubtotalText = "$420.00",
            orderTotalText = "$462.00",
            orderTaxText = "$42.00"
        ),
        onNewTransactionClicked = { /* No-Op */ }
    )
}
