package com.woocommerce.android.ui.orders.creation.shipping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.model.Order
import java.math.BigDecimal

@Composable
fun CouponFormSection(
    couponDetails: List<Order.CouponLine>,
    //formatCurrency: (BigDecimal) -> String,
    onRemoveCoupon: (String) -> Unit
) {
    Column {
        couponDetails.forEach { coupon ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),// Adjust the padding as needed
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Coupon: ${coupon.code}")
                    Text(text = "Discount: ${coupon.discount}")
                }
                Button(onClick = { onRemoveCoupon(coupon.code) }) {
                    Text(text = "Remove")
                }
            }
        }
    }
}

