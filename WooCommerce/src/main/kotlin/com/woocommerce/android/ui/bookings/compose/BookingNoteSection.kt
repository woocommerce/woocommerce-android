package com.woocommerce.android.ui.bookings.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingNoteSection(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        BookingSectionHeader(R.string.booking_note_header)
        Column(
            modifier = Modifier.background(color = MaterialTheme.colorScheme.surfaceContainer)
        ) {
            HorizontalDivider(thickness = 0.5.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick() }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                BookingDetailsLabel(label = R.string.booking_note_label_add_note, modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            HorizontalDivider(thickness = 0.5.dp)
        }
        Text(
            text = stringResource(R.string.booking_note_description_private),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingNoteSectionPreview() {
    WooThemeWithBackground {
        BookingNoteSection(
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
