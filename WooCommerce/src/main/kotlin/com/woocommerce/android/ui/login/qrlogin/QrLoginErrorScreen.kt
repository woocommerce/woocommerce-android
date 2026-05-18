package com.woocommerce.android.ui.login.qrlogin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.annotatedStringRes
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

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
    ) {
        // Content scrolls so the primary / secondary buttons stay visible in landscape on phones,
        // with large system fonts, or with longer translated copy where the static layout would
        // otherwise push them off the bottom edge.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Content(content = content)
        }
        Buttons(
            content = content,
            onPrimaryClicked = onPrimaryClicked,
            onSecondaryClicked = onSecondaryClicked,
        )
    }
}

@Composable
private fun Content(content: QrLoginErrorContent) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
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
            text = bodyAnnotatedString(content),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Buttons(
    content: QrLoginErrorContent,
    onPrimaryClicked: () -> Unit,
    onSecondaryClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = dimensionResource(id = R.dimen.major_150),
                bottom = dimensionResource(id = R.dimen.major_100)
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        WCColoredButton(
            onClick = onPrimaryClicked,
            text = stringResource(id = content.primaryAction),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_150)))
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

/**
 * Resolves the body text for an error. Body strings are formatted via `%1$s` / `%2$s`
 * placeholders filled from [QrLoginErrorContent.bodyHighlightedArgs], then parsed as HTML so
 * `<b>…</b>` (and any other supported markup) is rendered as styled spans instead of literal
 * tags.
 */
@Composable
private fun bodyAnnotatedString(content: QrLoginErrorContent): AnnotatedString {
    val args = content.bodyHighlightedArgs.map { stringResource(id = it) }.toTypedArray()
    @Suppress("SpreadOperator")
    return annotatedStringRes(content.body, *args)
}
