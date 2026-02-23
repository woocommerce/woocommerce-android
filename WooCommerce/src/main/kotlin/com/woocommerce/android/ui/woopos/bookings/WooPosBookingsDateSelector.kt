package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.DatePickerDialog
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import java.util.Date

@Composable
fun WooPosBookingsDateSelector(
    dateSelectorState: DateSelectorState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier
                .padding(vertical = WooPosSpacing.XSmall.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onUIEvent(WooPosBookingsUIEvent.PreviousDayClicked) }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_left_24dp),
                    contentDescription = stringResource(R.string.woopos_bookings_date_selector_previous_day),
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Row(
                modifier = Modifier
                    .widthIn(min = 130.dp)
                    .clickable { showDatePicker = true }
                    .padding(horizontal = WooPosSpacing.XSmall.value),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_calendar_16),
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = dateSelectorState.formattedDate,
                    style = WooPosTypography.BodySmall,
                    fontWeight = FontWeight.Normal,
                    color = primaryColor,
                )
            }

            IconButton(
                onClick = { onUIEvent(WooPosBookingsUIEvent.NextDayClicked) }
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                    contentDescription = stringResource(R.string.woopos_bookings_date_selector_next_day),
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    if (showDatePicker) {
        DatePickerDialog(
            currentDate = Date(dateSelectorState.selectedDateMillis),
            onDateSelected = { selectedDate ->
                showDatePicker = false
                onUIEvent(WooPosBookingsUIEvent.DateSelected(selectedDate.time))
            },
            onDismissRequest = { showDatePicker = false },
        )
    }
}

@WooPosPreview
@Composable
fun WooPosBookingsDateSelectorPreview() {
    WooPosTheme {
        WooPosBookingsDateSelector(
            dateSelectorState = DateSelectorState(
                formattedDate = "19 Feb, Wed",
                selectedDateMillis = System.currentTimeMillis(),
            ),
            onUIEvent = {},
        )
    }
}
