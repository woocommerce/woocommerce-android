package com.woocommerce.android.ui.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun ProgressDialog(
    title: String,
    subtitle: String,
    onDismissRequest: () -> Unit = {},
    properties: DialogProperties = DialogProperties(dismissOnClickOutside = false)
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        Surface(shape = MaterialTheme.shapes.medium) {
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_75)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                        vertical = dimensionResource(id = R.dimen.major_150)
                    )
            ) {
                Text(text = title, style = MaterialTheme.typography.h6)
                Row(horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.major_150))) {
                    CircularProgressIndicator()
                    Text(text = subtitle, style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}

@Preview
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ProgressDialogPreview() {
    WooThemeWithBackground {
        ProgressDialog(title = "Title", subtitle = "Subtitle")
    }
}
