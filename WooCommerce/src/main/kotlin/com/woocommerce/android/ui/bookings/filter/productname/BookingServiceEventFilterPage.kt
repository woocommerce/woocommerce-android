package com.woocommerce.android.ui.bookings.filter.productname

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.string
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.bookings.filter.BookingsFilterSelectionPage
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCSearchField
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
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
            hint = stringResource(string.bookings_filter_search_service),
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
        HorizontalDivider(thickness = 0.5.dp)
        if (state.isLoading) {
            BookingServiceEventFilterSkeletons()
        } else {
            BookingsFilterSelectionPage(items = state.items)
        }
    }
}

@Composable
private fun BookingServiceEventFilterSkeletons() {
    Column(modifier = Modifier.fillMaxWidth()) {
        SkeletonView(
            modifier = Modifier
                .size(62.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(175.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(130.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(167.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
        SkeletonView(
            modifier = Modifier
                .size(166.dp, 64.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(start = 16.dp), thickness = 0.5.dp)
    }
}

@LightDarkThemePreviews
@Composable
private fun BookingServiceEventFilterPagePreview() {
    WooThemeWithBackground {
        val availableProducts = listOf(
            BookableProduct(id = 1, name = UiString.UiStringText("Yoga Class")),
            BookableProduct(id = 2, name = UiString.UiStringText("Pilates Session")),
            BookableProduct(id = 3, name = UiString.UiStringText("Surf Lesson")),
        )
        val selected = BookingsFilterOption.ServiceEvents(
            values = setOf(
                BookingsFilterOption.ProductInfo(productId = 1, productName = "Yoga Class")
            )
        )
        val state = BookingServiceEventFilterUiState(
            availableProducts = availableProducts,
            selectedProducts = selected,
            searchQuery = "",
            onProductSelected = {},
            onSearchQueryChanged = {}
        )
        BookingServiceEventFilterPage(state)
    }
}
