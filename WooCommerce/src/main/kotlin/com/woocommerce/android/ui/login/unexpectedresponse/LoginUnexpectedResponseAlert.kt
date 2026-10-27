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
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.LegacyWooThemeWithBackground

@Composable
fun LoginUnexpectedResponseAlert(
    onRetryClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    UnexpectedResponseAlert(
        onDismissClick = onDismissClick,
        confirmButton = {
            WCTextButton(onClick = onRetryClick) {
                Text(text = stringResource(id = R.string.try_again))
            }
        },
        dismissButton = {
            WCTextButton(onClick = onDismissClick) {
                Text(text = stringResource(id = R.string.dismiss))
            }
        }
    )
}

/**
 * For screens without a form below the alert to hold Contact Support.
 */
@Composable
fun LoginUnexpectedResponseAlertWithSupport(
    onRetryClick: () -> Unit,
    onContactSupportClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    UnexpectedResponseAlert(
        onDismissClick = onDismissClick,
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                WCTextButton(onClick = onRetryClick) {
                    Text(text = stringResource(id = R.string.try_again))
                }
                WCTextButton(onClick = onContactSupportClick) {
                    Text(text = stringResource(id = R.string.support_contact))
                }
                WCTextButton(onClick = onDismissClick) {
                    Text(text = stringResource(id = R.string.dismiss))
                }
            }
        }
    )
}

@Composable
private fun UnexpectedResponseAlert(
    onDismissClick: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null
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
        confirmButton = confirmButton,
        dismissButton = dismissButton
    )
}

@LightDarkThemePreviews
@Composable
private fun LoginUnexpectedResponseAlertPreview() {
    LegacyWooThemeWithBackground {
        LoginUnexpectedResponseAlert(onRetryClick = {}, onDismissClick = {})
    }
}

@LightDarkThemePreviews
@Composable
private fun LoginUnexpectedResponseAlertWithSupportPreview() {
    LegacyWooThemeWithBackground {
        LoginUnexpectedResponseAlertWithSupport(onRetryClick = {}, onContactSupportClick = {}, onDismissClick = {})
    }
}
