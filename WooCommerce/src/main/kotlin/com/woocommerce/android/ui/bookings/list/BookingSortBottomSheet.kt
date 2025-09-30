package com.woocommerce.android.ui.bookings.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingSortBottomSheet(state: BookingListSortBottomSheetState) {
    val sheetState = rememberModalBottomSheetState()
    WCModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = state.onDismiss
    ) {
        Column(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(id = R.string.product_list_sorting_header),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.size(16.dp))
            BookingListSortOption.entries.forEach { option ->
                ListItem(
                    modifier = Modifier.clickable { state.onSelect(option) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    headlineContent = { Text(text = option.name(), fontWeight = FontWeight.Medium) },
                    trailingContent = {
                        if (state.selectedOption == option) {
                            Image(
                                modifier = Modifier.padding(end = 12.dp),
                                imageVector = ImageVector.vectorResource(R.drawable.ic_check),
                                contentDescription = stringResource(R.string.product_list_sorting_list_item),
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun BookingListSortOption.name() = when (this) {
    BookingListSortOption.NewestToOldest -> stringResource(R.string.bookings_sort_by_newest_to_oldest)
    BookingListSortOption.OldestToNewest -> stringResource(R.string.bookings_sort_by_oldest_to_newest)
}
