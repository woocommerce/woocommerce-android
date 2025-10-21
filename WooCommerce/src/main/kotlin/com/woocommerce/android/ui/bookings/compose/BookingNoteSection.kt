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
    note: String,
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
                    .padding(horizontal = 16.dp)
            ) {
                if (note.isEmpty()) {
                    BookingDetailsLabel(
                        label = R.string.booking_note_label_add_note,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    )
                } else {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp)
                    )
                }
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(start = 8.dp)
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
            note = "",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingNoteSectionWithNotePreview() {
    WooThemeWithBackground {
        BookingNoteSection(
            note = "The customer prefers eco-friendly products and shorter length cuts. Please ensure the stylist " +
                "recommends sustainable options and is prepared for a trim focusing on shorter lengths. " +
                "If there are any special requests or allergies, please confirm with the customer " +
                "prior to the appointment.",
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}
