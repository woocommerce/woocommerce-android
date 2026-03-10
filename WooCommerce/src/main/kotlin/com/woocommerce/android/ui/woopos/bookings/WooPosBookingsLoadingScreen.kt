package com.woocommerce.android.ui.woopos.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosElevation
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosBookingsLoadingScreen(
    dateSelectorState: DateSelectorState,
    onUIEvent: (WooPosBookingsUIEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceBright)
                .weight(0.3f)
                .fillMaxHeight()
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = WooPosSpacing.Medium.value)
                    .heightIn(min = WOO_POS_BOOKINGS_TOOLBAR_HEIGHT),
            )

            WooPosBookingsDateSelector(
                dateSelectorState = dateSelectorState,
                onUIEvent = onUIEvent,
            )

            WooPosBookingsListLoadingPane(
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
fun WooPosBookingsBookingLoadingRow() {
    WooPosCard(
        modifier = Modifier.wrapContentHeight(),
        shape = RoundedCornerShape(WooPosCornerRadius.Medium.value),
        backgroundColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        elevation = WooPosElevation.Medium,
        shadowType = ShadowType.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(WooPosSpacing.Medium.value),
        ) {
            WooPosShimmerText(
                text = "3:00-4:00 PM",
                style = WooPosTypography.BodySmall.style,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(WooPosSpacing.XSmall.value))

            WooPosShimmerText(
                text = "Precision Haircut \u00B7 Customer",
                style = WooPosTypography.BodySmall.style
            )

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            Row(
                horizontalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value),
            ) {
                ShimmerBadge("Unattended")
                ShimmerBadge("Paid")
            }
        }
    }
}

@Composable
private fun ShimmerBadge(text: String) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val measured = textMeasurer.measure(
        text = text,
        style = WooPosTypography.Caption.style
    )
    val textWidth = with(density) { measured.size.width.toDp() }
    val textHeight = with(density) { measured.size.height.toDp() }
    WooPosShimmerBox(
        modifier = Modifier
            .width(textWidth + WooPosSpacing.Small.value * 2)
            .height(textHeight + WooPosSpacing.XXSmall.value * 2)
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
    )
}

@Composable
fun WooPosBookingsListLoadingPane(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(WooPosSpacing.Medium.value),
        contentPadding = PaddingValues(WooPosSpacing.Medium.value)
    ) {
        items(7) {
            WooPosBookingsBookingLoadingRow()
        }
    }
}

@Composable
fun BookingDetailsLoadingPane(modifier: Modifier = Modifier) {
    Box(modifier = modifier)
}

@WooPosPreview
@Composable
fun WooPosBookingsLoadingStatePreview() {
    WooPosTheme {
        WooPosBookingsLoadingScreen(
            dateSelectorState = DateSelectorState(
                formattedDate = "23 Feb, Mon",
                selectedDateMillis = System.currentTimeMillis(),
            ),
            onUIEvent = {},
        )
    }
}
