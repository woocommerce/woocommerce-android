package com.woocommerce.android.ui.products.addons.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.addons.order.OrderedAddonViewModel.ViewState
import org.wordpress.android.fluxc.domain.Addon

@Composable
fun OrderedAddonScreen(
    viewModel: OrderedAddonViewModel
) {
    val orderedAddonState by viewModel.orderedAddonsData.observeAsState(initial = emptyList())
    val orderedAddonViewState by viewModel.viewStateData.observeAsState(initial = ViewState())

    OrderedAddonScreen(
        orderedAddons = orderedAddonState,
        state = orderedAddonViewState
    )
}

@Composable
fun OrderedAddonScreen(
    orderedAddons: List<Addon>,
    state: ViewState
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        OrderedAddonList(orderedAddons = orderedAddons)
    }
}

@Composable
private fun OrderedAddonList(
    orderedAddons: List<Addon>
) {
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = Modifier
            .background(color = MaterialTheme.colors.surface)
    ) {
        itemsIndexed(orderedAddons) { _, addon ->
            Column(
                verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_50)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimensionResource(id = R.dimen.major_100),
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
            ) {
                OrderedAddonItem(orderedAddon = addon)
            }
        }
    }
}

@Composable
private fun OrderedAddonItem(
    orderedAddon: Addon
) {
    Column {
        Text(
            text = orderedAddon.name,
            style = MaterialTheme.typography.h1,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        orderedAddon.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    OrderedAddonList(
        orderedAddons = listOf(
            Addon.Checkbox(
                name = "First addon",
                titleFormat = Addon.TitleFormat.Heading,
                description = "First addon description",
                required = false,
                position = 0,
                options = emptyList()
            )
        )
    )
}
