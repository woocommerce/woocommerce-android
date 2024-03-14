package com.woocommerce.android.ui.orders

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsViewModel.CustomAmountType

@Composable
fun CustomAmountTypeBottomSheet(currency: String, onClick: (CustomAmountType) -> Unit) {
    BottomSheetContent(currency, onClick)
}

@Composable
fun BottomSheetContent(
    currency: String,
    onClick: (CustomAmountType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(id = R.color.color_surface))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Divider(
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .padding(vertical = 8.dp),
            color = Color.Gray
        )
        Text(
            text = stringResource(id = R.string.custom_amounts_bottom_sheet_heading),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = Color.Gray
            ),
            modifier = Modifier.padding(bottom = 24.dp),
            color = colorResource(id = R.color.color_on_surface_medium)
        )
        CustomAmountOption(
            label = stringResource(id = R.string.custom_amounts_bottom_sheet_fixed_amount_option),
            symbol = currency,
            onClick = { onClick(CustomAmountType.FIXED_CUSTOM_AMOUNT) }
        )
        CustomAmountOption(
            label = stringResource(id = R.string.custom_amounts_bottom_sheet_percentage_amount_option),
            symbol = "%",
            onClick = { onClick(CustomAmountType.PERCENTAGE_CUSTOM_AMOUNT) }
        )
    }
}

@Composable
fun CustomAmountOption(label: String, symbol: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            modifier = Modifier.size(24.dp),
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
            ),
            color = colorResource(id = R.color.color_on_surface)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 20.sp,
            ),
            color = colorResource(id = R.color.color_on_surface)
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewCustomAmountBottomSheet() {
    CustomAmountTypeBottomSheet("$") {
    }
}
