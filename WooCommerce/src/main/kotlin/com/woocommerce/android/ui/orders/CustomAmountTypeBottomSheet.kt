package com.woocommerce.android.ui.orders

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
@Composable
fun CustomAmountTypeBottomSheet() {
    BottomSheetContent()
}

@Composable
fun BottomSheetContent() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            text = "How do you want to add your custom amount?",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        CustomAmountOption(
            label = "A fixed amount",
            symbol = "$"
        )
        CustomAmountOption(
            label = "A percentage of the order total",
            symbol = "%"
        )
    }
}

@Composable
fun CustomAmountOption(label: String, symbol: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = symbol,
            modifier = Modifier.size(24.dp),
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label)
    }
}
@Preview
@Composable
fun PreviewCustomAmountBottomSheet() {
    CustomAmountTypeBottomSheet()
}
