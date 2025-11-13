package com.woocommerce.android.ui.bookings.filter

import android.icu.text.ListFormatter
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCListItemWithInlineSubtitle
import com.woocommerce.android.util.UiHelpers

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
        // Convert values to "value1, value2, value3" using ListFormatter
        val context = LocalContext.current
        val local = LocalConfiguration.current
        val subtitle = remember(item.values) {
            val values = item.values
            if (values.isNullOrEmpty()) {
                context.getString(R.string.bookings_filter_default)
            } else {
                val texts = values.map { UiHelpers.getTextOfUiString(context, uiString = it) }
                ListFormatter.getInstance(local.locales[0], ListFormatter.Type.AND, ListFormatter.Width.NARROW)
                    .format(texts)
            }
        }

        WCListItemWithInlineSubtitle(
            text = stringResource(item.title),
            subtitle = subtitle,
            modifier = Modifier
                .defaultMinSize(minHeight = 64.dp)
                .clickable { item.onClick() }
                .padding(vertical = 8.dp)
        )
        HorizontalDivider(thickness = 0.5.dp)
    }
}
