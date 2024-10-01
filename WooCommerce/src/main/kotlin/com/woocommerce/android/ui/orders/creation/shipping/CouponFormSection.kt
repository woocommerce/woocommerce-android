package com.woocommerce.android.ui.orders.creation.shipping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import java.math.BigDecimal

@Composable
fun CouponFormSection(
    couponDetails: List<Order.CouponLine>,
    onRemoveCoupon: (String) -> Unit
) {
    Column {
        couponDetails.forEach { coupon ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Coupon: ${coupon.code}")
                    Text(text = "Discount: ${coupon.discount}")
                }
                IconButton(onClick = { onRemoveCoupon(coupon.code) }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_gridicons_trash_24dp),
                        contentDescription = "Remove Coupon"
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCouponFormSection() {
    val sampleCoupons = listOf(
        Order.CouponLine(code = "SUMMER21", discount = BigDecimal(10.00).toString()),
        Order.CouponLine(code = "WELCOME5", discount = BigDecimal(5.00).toString())
    )

    CouponFormSection(
        couponDetails = sampleCoupons,
        onRemoveCoupon = { /* Do nothing for preview */ }
    )
}
