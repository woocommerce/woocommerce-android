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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.details.OrderDetailFragment.CurrencyFormattedAmount
import com.woocommerce.android.ui.orders.details.OrderDetailFragment.CustomAmountUI
import java.math.BigDecimal

@Composable
fun CustomAmountCard(
    customAmountUI: CustomAmountUI
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
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
            ) {
                ImageWithBorder()
                Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.minor_100)))
                Column(modifier = Modifier.weight(2f)) {
                    Text(
                        text = customAmountUI.name,
                        style = TextStyle(
                            fontWeight = FontWeight.Normal,
                            fontSize = 18.sp
                        ),
                        color = colorResource(id = R.color.color_on_surface),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                    text = customAmountUI.amount.amount,
                    style = TextStyle(
                        fontWeight = FontWeight.Normal,
                        fontSize = 18.sp
                    ),
                    color = colorResource(id = R.color.color_on_surface),
                )
            }
            if (customAmountUI.shouldShowDivider) {
                Divider(
                    color = colorResource(id = R.color.divider_color),
                    thickness = dimensionResource(id = R.dimen.minor_10),
                    modifier = Modifier.padding(
                        vertical = dimensionResource(id = R.dimen.minor_100),
                        horizontal = dimensionResource(id = R.dimen.minor_100)
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
fun Header(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
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
            CustomAmountUI(
                name = "Services Rendered",
                amount = CurrencyFormattedAmount(BigDecimal.TEN.toString()),
                shouldShowDivider = false
            )
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CustomAmountCardWithLongNamePreview() {
    WooThemeWithBackground {
        CustomAmountCard(
            CustomAmountUI(
                name = "Very long name for testing very long name",
                amount = CurrencyFormattedAmount(BigDecimal.TEN.toString()),
                shouldShowDivider = false
            )
        )
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HeaderPreview() {
    WooThemeWithBackground {
        Header(text = stringResource(id = R.string.order_detail_custom_amounts_header))
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CustomAmountCardWithHeaderPreview() {
    WooThemeWithBackground {
        Column {
            Header(text = stringResource(id = R.string.order_detail_custom_amounts_header))
            CustomAmountCard(
                CustomAmountUI(
                    name = "Services Rendered",
                    amount = CurrencyFormattedAmount(BigDecimal.TEN.toString()),
                    shouldShowDivider = false
                )
            )
        }
    }
}
