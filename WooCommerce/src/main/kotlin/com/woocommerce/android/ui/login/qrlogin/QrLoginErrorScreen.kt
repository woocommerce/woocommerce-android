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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
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
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_150),
                // Generous bottom padding so any trailing copy ("Any troubles signing in?
                // Check out the FAQ." or similar fallback link) doesn't visually crowd
                // the primary CTA above it.
                vertical = dimensionResource(id = R.dimen.major_200),
            ),
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
            text = bodyAnnotatedString(content),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(dimensionResource(id = R.dimen.major_250)))
        WCColoredButton(
            onClick = onPrimaryClicked,
            text = stringResource(id = content.primaryAction),
            modifier = Modifier.fillMaxWidth()
        )
        // 24dp (was 12dp) so the outlined secondary button doesn't visually touch the
        // primary "Try again" button above it, and so any trailing FAQ / fallback copy
        // (rendered by parent surfaces below this column) sits comfortably away from
        // the CTA stack.
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
 * Resolves the body text for an error. When [QrLoginErrorContent.bodyHighlightedArgs] is empty
 * we just return the body string as-is. Otherwise we treat the body as a `%1$s, %2$s, …`
 * template, substitute each arg in order, and apply a SemiBold span to each substituted run.
 * This pattern keeps `<b>…</b>` markup out of strings.xml so translators can't accidentally
 * drop or break it.
 */
@Composable
private fun bodyAnnotatedString(content: QrLoginErrorContent): AnnotatedString {
    val template = stringResource(id = content.body)
    if (content.bodyHighlightedArgs.isEmpty()) return AnnotatedString(template)
    val args = content.bodyHighlightedArgs.map { stringResource(id = it) }
    return buildAnnotatedString {
        var cursor = 0
        args.forEachIndexed { index, value ->
            val placeholder = "%${index + 1}\$s"
            val placeholderStart = template.indexOf(placeholder, startIndex = cursor)
            if (placeholderStart < 0) return@forEachIndexed
            append(template.substring(cursor, placeholderStart))
            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(value) }
            cursor = placeholderStart + placeholder.length
        }
        append(template.substring(cursor))
    }
}
