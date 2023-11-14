package com.woocommerce.android.ui.payments.scantopay

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.QRCode
import com.woocommerce.android.ui.compose.component.WCColoredButton

@Composable
fun ScanToPayScreen(
    qrContent: String,
    onDoneClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color = colorResource(id = R.color.color_surface_elevated))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(id = R.string.scan_to_pay_title),
            color = colorResource(id = R.color.color_on_surface_high),
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        QRCode(content = qrContent, size = 290.dp, overlayId = R.drawable.img_woo_bubble_white)
        Spacer(modifier = Modifier.height(12.dp))
        WCColoredButton(
            modifier = Modifier
                .fillMaxWidth(),
            onClick = { onDoneClicked() }
        ) {
            Text(text = stringResource(id = R.string.done))
        }
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ScanToPayScreenPreview() {
    ScanToPayScreen("https://woocommerce.com", {})
}
