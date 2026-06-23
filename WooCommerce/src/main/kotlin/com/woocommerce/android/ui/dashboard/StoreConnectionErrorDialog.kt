package com.woocommerce.android.ui.dashboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun StoreConnectionErrorDialog(
    onContactSupportClick: () -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismissClick,
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_150))
            ) {
                Text(
                    text = stringResource(id = R.string.store_connection_error_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                Text(
                    text = stringResource(id = R.string.store_connection_error_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_200)))

                WCColoredButton(
                    onClick = onContactSupportClick,
                    text = stringResource(id = R.string.store_connection_error_dialog_contact_support),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))

                WCOutlinedButton(
                    onClick = onDismissClick,
                    text = stringResource(id = R.string.dismiss),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview
@Composable
private fun StoreConnectionErrorDialogPreview() {
    WooThemeWithBackground {
        StoreConnectionErrorDialog(
            onContactSupportClick = {},
            onDismissClick = {},
        )
    }
}
