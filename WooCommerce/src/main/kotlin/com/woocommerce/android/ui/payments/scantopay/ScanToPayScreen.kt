package com.woocommerce.android.ui.payments.scantopay

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.QRCode

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ScanToPayScreen(qrContent: String) {
    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Expanded)
    ModalBottomSheetLayout(
        sheetState = state,
        sheetContent = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(id = R.string.scan_to_pay_title),
                    style = MaterialTheme.typography.subtitle1,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
                QRCode(content = qrContent, size = 250.dp, padding = 16.dp)
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            }
        }
    ) {}
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ScanToPayScreenPreview() {
    ScanToPayScreen("https://woocommerce.com")
}
