package com.woocommerce.android.mediapicker

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.CAMERA
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.DEVICE
import org.wordpress.android.mediapicker.api.MediaPickerSetup.DataSource.WP_MEDIA_LIBRARY

@Composable
fun MediaPickerDialog(
    onDismissRequest: () -> Unit,
    onMediaLibraryRequested: (DataSource) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface, MaterialTheme.shapes.medium)
                .shadow(elevation = dimensionResource(id = dimen.major_75)),
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = dimensionResource(id = dimen.major_100)),
            ) {
                Text(
                    modifier = Modifier
                        .padding(
                            start = dimensionResource(id = dimen.major_100),
                            end = dimensionResource(id = dimen.major_100),
                            bottom = dimensionResource(id = dimen.minor_100)
                        ),
                    text = stringResource(id = string.media_picker_dialog_title),
                    style = MaterialTheme.typography.h6
                )

                DialogButton(
                    image = drawable.ic_gridicons_image,
                    title = string.image_source_device_chooser,
                    onClick = { onMediaLibraryRequested(DEVICE) }
                )
                DialogButton(
                    image = drawable.ic_gridicons_camera,
                    title = string.image_source_device_camera,
                    onClick = { onMediaLibraryRequested(CAMERA) }
                )
                DialogButton(
                    image = drawable.ic_wordpress,
                    title = string.image_source_wp_media_library,
                    onClick = { onMediaLibraryRequested(WP_MEDIA_LIBRARY) }
                )
            }
        }
    }
}

@Composable
private fun DialogButton(@DrawableRes image: Int, @StringRes title: Int, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(id = dimen.major_100),
                    vertical = dimensionResource(id = dimen.minor_100)
                )
        ) {
            Image(
                painter = painterResource(id = image),
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = dimen.major_150))
            )
            Text(
                modifier = Modifier
                    .align(alignment = Alignment.CenterVertically)
                    .padding(start = dimensionResource(id = dimen.major_100)),
                text = stringResource(id = title),
                color = MaterialTheme.colors.onSurface,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Preview
@Composable
private fun PreviewMediaPickerDialog() {
    MediaPickerDialog({}, {})
}
