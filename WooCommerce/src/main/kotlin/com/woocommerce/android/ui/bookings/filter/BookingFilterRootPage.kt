package com.woocommerce.android.ui.bookings.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCListItemWithInlineSubtitle

@Composable
fun BookingFilterRootPage(
    items: List<BookingFilterListItem>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
    ) {
        items(items) { item -> BookingFilterListRow(item) }
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
