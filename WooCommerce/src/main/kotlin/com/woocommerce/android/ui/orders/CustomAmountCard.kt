package com.woocommerce.android.ui.orders

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.math.BigDecimal

@Composable
fun CustomAmountCard(
    feeLine: Order.FeeLine,
    shouldShowDivider: Boolean,
) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                ImageWithBorder()
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = feeLine.name ?: "",
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp
                        ),
                        color = colorResource(id = R.color.color_on_surface)
                    )
                    Text(
                        text = stringResource(id = R.string.custom_amounts),
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = Color.Gray
                        ),
                        color = colorResource(id = R.color.color_on_surface_medium)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = feeLine.total.toString(),
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp
                    ),
                    color = colorResource(id = R.color.color_on_surface)
                )
            }
            if (shouldShowDivider) {
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = 1.dp,
                    modifier = Modifier.padding(
                        vertical = 8.dp,
                        horizontal = 8.dp
                    )
                )
            }
        }
    }
}

@Composable
fun ImageWithBorder() {
    Box(
        modifier = Modifier
            .border(width = 1.dp, color = colorResource(id = R.color.divider_color))
            .padding(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_custom_amount),
            contentDescription = "Custom Amount",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = "CUSTOM AMOUNTS",
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.CenterStart),
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CustomAmountCardPreview() {
    WooThemeWithBackground {
        CustomAmountCard(
            Order.FeeLine(
                id = 0L,
                name = "Services Rendered",
                total = BigDecimal.TEN,
                totalTax = BigDecimal.ZERO,
                taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE
            ),
            true
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HeaderPreview() {
    WooThemeWithBackground {
        Header()
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CustomAmountCardWithHeaderPreview() {
    WooThemeWithBackground {
        Column {
            Header()
            CustomAmountCard(
                Order.FeeLine(
                    id = 0L,
                    name = "Services Rendered",
                    total = BigDecimal.TEN,
                    totalTax = BigDecimal.ZERO,
                    taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE
                ),
                true
            )
        }
    }
}
