package com.woocommerce.android.ui.bookings.filter.productname

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.component.WCSearchField
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@Composable
fun BookingServiceEventFilterRoute(
    initialServiceEvents: BookingsFilterOption.ServiceEvents?,
    onServiceEventsFilterChanged: (BookingsFilterOption.ServiceEvents) -> Unit,
) {
    val viewModel =
        hiltViewModel<BookingServiceEventFilterViewModel, BookingServiceEventFilterViewModel.Factory>
        { factory ->
            factory.create(initialServiceEvents, onServiceEventsFilterChanged)
        }
    val uiState by viewModel.uiState.collectAsState()
    BookingServiceEventFilterPage(uiState)
}

@Composable
fun BookingServiceEventFilterPage(state: BookingServiceEventFilterUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        WCSearchField(
            value = state.searchQuery,
            onValueChange = state.onSearchQueryChanged,
            hint = stringResource(string.bookings_filter_search_service_event),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(id = dimen.major_100),
                    vertical = dimensionResource(id = dimen.minor_100)
                ),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false
            ),
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.items) { item ->
                ProductRow(item = item)
            }
        }
    }
}

@Composable
private fun ProductRow(item: BookableProductItem) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 64.dp)
                .clickable(onClick = item.onClick)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.product.name ?: stringResource(R.string.bookings_filter_default),
                style = MaterialTheme.typography.titleMedium,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (item.selected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_done_secondary),
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp)
    }
}
