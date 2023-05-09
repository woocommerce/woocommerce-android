package com.woocommerce.android.ui.payments.scantopay

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.component.QRCode

@Composable
fun ScanToPayScreen(qrContent: String) {
    QRCode(
        qrContent,
        size = 160.dp,
        padding = 16.dp,
    )
}
