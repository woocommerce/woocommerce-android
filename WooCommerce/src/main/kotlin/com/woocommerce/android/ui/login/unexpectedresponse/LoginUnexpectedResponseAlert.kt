package com.woocommerce.android.ui.login.unexpectedresponse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground

@Composable
fun LoginUnexpectedResponseAlert(
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit,
    onBrowserLoginClick: (() -> Unit)? = null
) {
    AlertDialog(
        title = { Text(text = stringResource(id = R.string.login_unexpected_response_title)) },
        text = {
            Text(
                text = stringResource(id = R.string.login_unexpected_response_message),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        onDismissRequest = onDismissClick,
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                WCTextButton(onClick = onRetryClick) {
                    Text(text = stringResource(id = R.string.try_again), textAlign = TextAlign.End)
                }
                if (onBrowserLoginClick != null) {
                    WCTextButton(onClick = onBrowserLoginClick) {
                        Text(
                            text = stringResource(id = R.string.login_site_credentials_use_web_authorization),
                            textAlign = TextAlign.End
                        )
                    }
                }
                WCTextButton(onClick = onDismissClick) {
                    Text(text = stringResource(id = R.string.dismiss), textAlign = TextAlign.End)
                }
            }
        }
    )
}

@LightDarkThemePreviews
@Composable
private fun LoginUnexpectedResponseAlertPreview() {
    LegacyWooThemeWithBackground {
        LoginUnexpectedResponseAlert(onRetryClick = {}, onDismissClick = {}, onBrowserLoginClick = {})
    }
}
