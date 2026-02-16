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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.ShadowType
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosCard
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography

@Composable
fun WooPosBookingsLoadingScreen(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {
        WooPosBookingsListLoadingPane(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(top = WOO_POS_BOOKINGS_TOOLBAR_HEIGHT + WooPosSpacing.Small.value)
                .weight(0.3f)
                .fillMaxHeight()
        )

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
    WooPosCard(shadowType = ShadowType.Soft) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(
                    horizontal = WooPosSpacing.Medium.value,
                    vertical = WooPosSpacing.Medium.value
                )
                .heightIn(min = 64.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(WooPosSpacing.XSmall.value)) {
                WooPosShimmerText(
                    text = "Booking #123",
                    style = WooPosTypography.BodySmall.style,
                    fontWeight = FontWeight.Bold
                )
                WooPosShimmerText(
                    text = "January 1, 2024 at 12:00 PM",
                    style = WooPosTypography.BodySmall.style
                )
                WooPosShimmerText(
                    text = "customer@example.com",
                    style = WooPosTypography.BodySmall.style
                )
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.2f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
                )
            }

            Spacer(Modifier.weight(1f))

            WooPosShimmerText(
                text = "$100.00",
                style = WooPosTypography.BodySmall.style
            )
        }
    }
}

@Composable
fun WooPosBookingsListLoadingPane(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.statusBarsPadding(),
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
        WooPosBookingsLoadingScreen()
    }
}
