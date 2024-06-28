package com.woocommerce.android.ui.woopos.home.totals.payment.success

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
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
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
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
                    .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 0.dp)
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
                    style = MaterialTheme.typography.h4.copy(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF004D40)
                    ),
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(24.dp))

                Surface(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .widthIn(max = 420.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFBDBDBD))
                ) {
                    Column(
                        modifier = Modifier.padding(21.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.woopos_payment_subtotal_label),
                                style = MaterialTheme.typography.h6
                            )
                            Text(
                                text = state.orderSubtotalText,
                                style = MaterialTheme.typography.h6
                            )
                        }

                        Divider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.woopos_payment_tax_label),
                                style = MaterialTheme.typography.h6
                            )
                            Text(
                                text = state.orderTaxText,
                                style = MaterialTheme.typography.h6
                            )
                        }

                        Divider()

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
                                color = Color(0xFF512DA8)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                modifier = Modifier
                    .padding(top = 0.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
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
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(id = R.drawable.woo_pos_ic_return_home),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.woo_pos_new_transaction_button),
                        style = MaterialTheme.typography.h4,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
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
