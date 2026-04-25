package com.woocommerce.android.ui.login.qrlogin

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
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

data class QrLoginErrorContent(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val primaryAction: Int,
    @StringRes val secondaryAction: Int = R.string.login_qr_endpoint_missing_enter_url,
)

@Composable
fun QrLoginErrorScreen(
    content: QrLoginErrorContent,
    onPrimaryClicked: () -> Unit,
    onSecondaryClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
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
            text = stringResource(id = content.title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_100)))
        Text(
            text = stringResource(id = content.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_200)))
        WCColoredButton(
            onClick = onPrimaryClicked,
            text = stringResource(id = content.primaryAction),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_75)))
        WCOutlinedButton(
            onClick = onSecondaryClicked,
            text = stringResource(id = content.secondaryAction),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun QrLoginErrorScreenPreview() {
    WooThemeWithBackground {
        QrLoginErrorScreen(
            content = QrLoginErrorContent(
                title = R.string.login_qr_scanner_error_token_title,
                body = R.string.login_qr_scanner_error_token_body,
                primaryAction = R.string.login_qr_endpoint_missing_retry,
            ),
            onPrimaryClicked = {},
            onSecondaryClicked = {},
        )
    }
}
