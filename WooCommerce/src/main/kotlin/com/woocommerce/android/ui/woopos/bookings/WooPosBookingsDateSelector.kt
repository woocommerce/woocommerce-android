package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

private val DateSelectorButtonHeight = 40.dp

@Composable
fun WooPosBookingsDateSelector(
    dateSelectorState: DateSelectorState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val buttonShape = RoundedCornerShape(WooPosCornerRadius.Medium.value)
    val buttonColor = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = WooPosSpacing.Medium.value,
                vertical = WooPosSpacing.Medium.value,
            ),
        horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = buttonShape,
            color = buttonColor,
        ) {
            Box(
                modifier = Modifier
                    .size(DateSelectorButtonHeight)
                    .clickable { onUIEvent(WooPosBookingsUIEvent.PreviousDayClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_left_24dp),
                    contentDescription = stringResource(R.string.woopos_bookings_date_selector_previous_day),
                    tint = contentColor,
                    modifier = Modifier.size(WooPosIconSize.Small.value),
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Surface(
                shape = buttonShape,
                color = buttonColor,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DateSelectorButtonHeight)
                        .clickable { showDatePicker = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.ic_date_range_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(WooPosIconSize.XSmall.value),
                            tint = contentColor,
                        )
                        Spacer(modifier = Modifier.width(WooPosSpacing.XSmall.value))
                        WooPosText(
                            text = dateSelectorState.formattedDate,
                            style = WooPosTypography.BodySmall,
                            fontWeight = FontWeight.Bold,
                            color = contentColor,
                        )
                    }
                }
            }

            if (showDatePicker) {
                DatePickerPopup(
                    selectedDateMillis = dateSelectorState.selectedDateMillis,
                    onDateSelected = { selectedMillis ->
                        showDatePicker = false
                        onUIEvent(WooPosBookingsUIEvent.DateSelected(selectedMillis))
                    },
                    onDismiss = { showDatePicker = false },
                )
            }
        }

        Surface(
            shape = buttonShape,
            color = buttonColor,
        ) {
            Box(
                modifier = Modifier
                    .size(DateSelectorButtonHeight)
                    .clickable { onUIEvent(WooPosBookingsUIEvent.NextDayClicked) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_chevron_right_24dp),
                    contentDescription = stringResource(R.string.woopos_bookings_date_selector_next_day),
                    tint = contentColor,
                    modifier = Modifier.size(WooPosIconSize.Small.value),
                )
            }
        }
    }
}

@Composable
private fun DatePickerPopup(
    selectedDateMillis: Long,
    onDateSelected: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDateMillis,
    )
    LaunchedEffect(datePickerState.selectedDateMillis) {
        val millis = datePickerState.selectedDateMillis ?: return@LaunchedEffect
        if (millis != selectedDateMillis) {
            onDateSelected(millis)
        }
    }
    val density = LocalDensity.current
    val offsetY = with(density) { (DateSelectorButtonHeight + WooPosSpacing.XSmall.value).roundToPx() }

    Popup(
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, offsetY),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        DatePickerContent(datePickerState = datePickerState)
    }
}

// Material3 DatePicker requires a minimum width of 360dp; do not scale down.
@Suppress("WooPosDesignSystemComponentSizeUsageRule")
@Composable
private fun DatePickerContent(datePickerState: DatePickerState) {
    val colors = DatePickerDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    )
    Surface(
        modifier = Modifier
            .requiredWidth(360.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = WooPosElevation.Medium.value,
        shape = RoundedCornerShape(WooPosCornerRadius.Large.value),
    ) {
        DatePicker(
            state = datePickerState,
            colors = colors,
        )
    }
}

@WooPosPreview
@Composable
fun WooPosBookingsDateSelectorPreview() {
    WooPosTheme {
        WooPosBookingsDateSelector(
            dateSelectorState = DateSelectorState(
                formattedDate = "02 Sep, Tue",
                selectedDateMillis = System.currentTimeMillis(),
            ),
            onUIEvent = {},
        )
    }
}

@WooPosPreview
@Composable
fun DatePickerContentPreview() {
    WooPosTheme {
        DatePickerContent(
            datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = System.currentTimeMillis(),
            ),
        )
    }
}
