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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosIconButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import java.util.Calendar
import java.util.Date

@Composable
fun WooPosBookingsDateSelector(
    dateSelectorState: DateSelectorState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = WooPosSpacing.XSmall.value),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            WooPosIconButton(
                icon = ImageVector.vectorResource(R.drawable.ic_chevron_left_24dp),
                contentDescription = stringResource(R.string.woopos_bookings_date_selector_previous_day),
                onClick = { onUIEvent(WooPosBookingsUIEvent.PreviousDayClicked) }
            )

            Row(
                modifier = Modifier
                    .clickable { showDatePicker = true }
                    .padding(horizontal = WooPosSpacing.XSmall.value),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_calendar_16),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = dateSelectorState.formattedDate,
                    style = WooPosTypography.BodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            WooPosIconButton(
                icon = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                contentDescription = stringResource(R.string.woopos_bookings_date_selector_next_day),
                onClick = { onUIEvent(WooPosBookingsUIEvent.NextDayClicked) }
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }

    if (showDatePicker) {
        DatePickerDialog(
            currentDate = Date(dateSelectorState.selectedDateMillis),
            onDateSelected = { selectedDate ->
                showDatePicker = false
                val calendar = Calendar.getInstance().apply { time = selectedDate }
                calendar.set(Calendar.HOUR_OF_DAY, 12)
                onUIEvent(WooPosBookingsUIEvent.DateSelected(calendar.timeInMillis))
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
