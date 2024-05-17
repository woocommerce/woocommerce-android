package com.woocommerce.android.ui.orders.details

import android.content.res.Configuration
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.ShippingMethod
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.Header

@Composable
fun ShippingLineSection(
    shippingLineDetails: List<OrderDetailViewModel.ShippingLineDetails>,
    modifier: Modifier = Modifier
) {
    if (shippingLineDetails.isNotEmpty()) {
        Column(modifier = modifier) {
            Header(text = stringResource(id = R.string.order_detail_shipping_header))
            shippingLineDetails.forEach { shippingDetails ->
                Card(
                    shape = RectangleShape
                ) {
                    ShippingLineDetailsCard(shippingDetails = shippingDetails)
                }
            }
        }
    }
}

@Composable
fun ShippingLineDetailsCard(
    shippingDetails: OrderDetailViewModel.ShippingLineDetails,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.major_100))
            .border(
                brush = SolidColor(MaterialTheme.colors.onSurface.copy(alpha = 0.12f)),
                width = 1.dp,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.corner_radius_large))
            )
            .padding(dimensionResource(id = R.dimen.major_100))

    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = shippingDetails.name,
                style = TextStyle(
                    fontWeight = FontWeight.Normal,
                    fontSize = 18.sp
                ),
                color = colorResource(id = R.color.color_on_surface),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (shippingDetails.shippingMethod != null) {
                Text(
                    text = shippingDetails.shippingMethod.title,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.Gray
                    ),
                    color = colorResource(id = R.color.color_on_surface_medium),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = shippingDetails.amount,
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp
            ),
            color = colorResource(id = R.color.color_on_surface),
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ShippingLineDetailsPreview() {
    WooThemeWithBackground {
        ShippingLineDetailsCard(
            OrderDetailViewModel.ShippingLineDetails(
                name = "UPS Shipping",
                shippingMethod = ShippingMethod("ups","UPS"),
                amount = "$10.00"
            )
        )
    }
}

