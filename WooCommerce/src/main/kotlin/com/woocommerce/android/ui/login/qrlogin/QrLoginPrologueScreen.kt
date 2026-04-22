package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun QrLoginPrologueScreen(
    onScanClicked: () -> Unit,
    onFallbackClicked: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.prologue_login_background_color))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_prologue_bg_white),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                colorResource(id = R.color.prologue_login_shape_color)
            ),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensionResource(id = R.dimen.major_100)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 96.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_woo_logo_white_99dp),
                    contentDescription = null
                )
                Spacer(Modifier.height(dimensionResource(id = R.dimen.major_200)))
                Text(
                    text = stringResource(id = R.string.login_qr_prologue_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
                Text(
                    text = stringResource(id = R.string.login_qr_prologue_instruction),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = dimensionResource(id = R.dimen.prologue_button_skip_bottom_margin)),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WCColoredButton(
                    onClick = onScanClicked,
                    text = stringResource(id = R.string.login_qr_prologue_scan_button),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(dimensionResource(id = R.dimen.minor_50)))
                WCTextButton(
                    onClick = onFallbackClicked,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.login_qr_prologue_fallback_link),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "QR login prologue")
@Composable
private fun QrLoginPrologueScreenPreview() {
    WooThemeWithBackground {
        QrLoginPrologueScreen(onScanClicked = {}, onFallbackClicked = {})
    }
}
