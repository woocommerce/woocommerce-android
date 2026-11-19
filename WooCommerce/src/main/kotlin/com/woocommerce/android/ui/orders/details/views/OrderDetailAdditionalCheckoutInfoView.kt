package com.woocommerce.android.ui.orders.details.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground
import com.woocommerce.android.ui.orders.details.AdditionalCheckoutField

@Composable
fun OrderDetailAdditionalCheckoutInfoView(fields: List<AdditionalCheckoutField>) {
    if (fields.isEmpty()) return

    Column {
        Text(
            text = stringResource(id = R.string.order_detail_additional_checkout_info_header).uppercase(),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )
        Card(
            shape = RectangleShape
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100)),
                modifier = Modifier.padding(vertical = dimensionResource(id = R.dimen.minor_100))
            ) {
                fields.forEach { field ->
                    AdditionalCheckoutFieldRow(
                        title = field.label,
                        value = field.value.displayText()
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalCheckoutFieldRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            )
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(end = dimensionResource(id = R.dimen.major_100))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
        )
    }
}

@Composable
private fun AdditionalCheckoutField.Value.displayText(): String = when (this) {
    is AdditionalCheckoutField.Value.Text -> text
    is AdditionalCheckoutField.Value.Checkbox -> stringResource(id = if (isChecked) R.string.yes else R.string.no)
}

@Preview
@Composable
private fun OrderDetailAdditionalCheckoutInfoViewPreview() {
    LegacyWooThemeWithBackground {
        OrderDetailAdditionalCheckoutInfoView(
            fields = listOf(
                AdditionalCheckoutField("Newsletter", AdditionalCheckoutField.Value.Checkbox(true)),
                AdditionalCheckoutField("Delivery slot", AdditionalCheckoutField.Value.Text("evening")),
                AdditionalCheckoutField("Gift message", AdditionalCheckoutField.Value.Text("Happy birthday"))
            )
        )
    }
}
