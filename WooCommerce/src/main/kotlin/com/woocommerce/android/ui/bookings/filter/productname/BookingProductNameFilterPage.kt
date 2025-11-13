package com.woocommerce.android.ui.bookings.filter.productname

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import org.wordpress.android.fluxc.network.rest.wpcom.wc.bookings.BookingsFilterOption

@Composable
fun BookingProductNameFilterRoute(
    initialServiceEvents: BookingsFilterOption.ServiceEvents?,
    onServiceEventsFilterChanged: (BookingsFilterOption.ServiceEvents) -> Unit,
) {
    val viewModel =
        hiltViewModel<BookingProductNameFilterViewModel, BookingProductNameFilterViewModel.Factory>
        { factory ->
            factory.create(initialServiceEvents, onServiceEventsFilterChanged)
        }
    val uiState by viewModel.uiState.collectAsState()
    BookingProductNameFilterPage(uiState)
}

@Composable
fun BookingProductNameFilterPage(state: BookingProductNameFilterUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            searchQuery = state.searchQuery,
            onSearchQueryChanged = state.onSearchQueryChanged
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.items) { item ->
                ProductRow(item = item)
            }
        }
    }
}

@Composable
private fun SearchBar(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(text = stringResource(R.string.bookings_filter_search_service_event))
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_search_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        TextButton(onClick = { focusManager.clearFocus() }) {
            Text(text = stringResource(R.string.cancel))
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
