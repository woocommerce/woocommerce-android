package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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

    val chevronColor = MaterialTheme.colorScheme.primary
    val chipContentColor = WooPosTheme.colors.tertiaryIconColor

    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(top = WooPosSpacing.Small.value),
        )

        Row(
            modifier = Modifier
                .padding(vertical = WooPosSpacing.XSmall.value),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onUIEvent(WooPosBookingsUIEvent.PreviousDayClicked) },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_left_24dp),
                    contentDescription = stringResource(R.string.woopos_bookings_date_selector_previous_day),
                    tint = chevronColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            AssistChip(
                onClick = { showDatePicker = true },
                label = {
                    Row {
                        WooPosText(
                            text = dateSelectorState.formattedDate,
                            style = WooPosTypography.BodySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        WooPosText(
                            text = " ${dateSelectorState.formattedDay}",
                            style = WooPosTypography.BodySmall,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.ic_date_range_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.widthIn(min = 150.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    labelColor = chipContentColor,
                    leadingIconContentColor = chipContentColor,
                ),
                border = null,
            )

            IconButton(
                onClick = { onUIEvent(WooPosBookingsUIEvent.NextDayClicked) },
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                    contentDescription = stringResource(R.string.woopos_bookings_date_selector_next_day),
                    tint = chevronColor,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
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
                formattedDate = "19 Feb",
                formattedDay = "Wed",
                selectedDateMillis = System.currentTimeMillis(),
            ),
            onUIEvent = {},
        )
    }
}
