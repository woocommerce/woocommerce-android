package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun QrLoginEndpointMissingScreen(
    onEnterUrlClicked: () -> Unit,
    onRetryClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(id = R.dimen.major_150)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_woo_generic_error),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_150)))
        Text(
            text = stringResource(id = R.string.login_qr_endpoint_missing_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = R.string.login_qr_endpoint_missing_body),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_200)))
        WCColoredButton(
            onClick = onEnterUrlClicked,
            text = stringResource(id = R.string.login_qr_endpoint_missing_enter_url),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        WCOutlinedButton(
            onClick = onRetryClicked,
            text = stringResource(id = R.string.login_qr_endpoint_missing_retry),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun QrLoginEndpointMissingScreenPreview() {
    WooThemeWithBackground {
        QrLoginEndpointMissingScreen(onEnterUrlClicked = {}, onRetryClicked = {})
    }
}
