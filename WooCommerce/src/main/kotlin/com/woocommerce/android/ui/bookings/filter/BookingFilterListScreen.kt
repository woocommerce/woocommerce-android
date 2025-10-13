package com.woocommerce.android.ui.bookings.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCListItemWithInlineSubtitle
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun BookingFilterListScreen(state: BookingFilterListUiState) {
    Scaffold(
        topBar = {
            Column {
                Toolbar(
                    title = stringResource(id = R.string.bookings_filters_default_title),
                    onNavigationButtonClick = state.onClose,
                    navigationIcon = ImageVector.vectorResource(id = R.drawable.ic_gridicons_cross_24dp)
                )
                HorizontalDivider(thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                HorizontalDivider(thickness = 0.5.dp)
                WCColoredButton(
                    onClick = state.onShowBookings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(stringResource(id = R.string.bookings_filters_show_bookings))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(state.items) { item -> BookingFilterListRow(item) }
        }
    }
}

@Composable
private fun BookingFilterListRow(item: BookingFilterListItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        WCListItemWithInlineSubtitle(
            text = stringResource(item.title),
            subtitle = item.value ?: stringResource(id = R.string.bookings_filter_default),
            modifier = Modifier
                .defaultMinSize(minHeight = 64.dp)
                .clickable { item.onClick() }
                .padding(vertical = 8.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingFilterListScreenPreview() {
    WooThemeWithBackground {
        BookingFilterListScreen(
            state = BookingFilterListUiState(
                initialBookingFilters = null,
            )
        )
    }
}
