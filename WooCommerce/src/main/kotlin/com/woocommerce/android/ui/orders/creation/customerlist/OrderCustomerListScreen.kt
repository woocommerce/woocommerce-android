@file:JvmName("CustomerListSelectionScreenKt")

package com.woocommerce.android.ui.orders.creation.customerlist

import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.customer.CustomerListScreen
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

@Composable
fun OrderCustomerListScreen(viewModel: CustomerListSelectionViewModel) {
    val state by viewModel.viewState.observeAsState()
    state?.let {
        OrderCustomerListScreen(
            state = it,
            onNavigateBack = viewModel::onNavigateBack,
            onAddCustomerClicked = viewModel::onAddCustomerClicked,
            onCustomerSelected = viewModel::onCustomerSelected,
            onSearchQueryChanged = viewModel::onSearchQueryChanged,
            onSearchTypeChanged = viewModel::onSearchTypeChanged,
            onEndOfListReached = viewModel::onEndOfListReached
        )
    }
}

@Composable
fun OrderCustomerListScreen(
    state: CustomerListViewState,
    onNavigateBack: () -> Unit,
    onAddCustomerClicked: () -> Unit,
    onCustomerSelected: (WCCustomerModel) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchTypeChanged: (Int) -> Unit,
    onEndOfListReached: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.order_creation_add_customer)) },
                navigationIcon = {
                    IconButton(onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                elevation = 0.dp,
            )
        },
        floatingActionButton = {
            if (state.showFab) CustomerListAddCustomerButton(onAddCustomerClicked)
        },
        modifier = modifier
    ) { padding ->
        CustomerListScreen(
            modifier = Modifier.padding(padding),
            state = state,
            onCustomerSelected = onCustomerSelected,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearchTypeChanged = onSearchTypeChanged,
            onEndOfListReached = onEndOfListReached,
        )
    }
}

@Composable
private fun CustomerListAddCustomerButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        backgroundColor = colorResource(id = R.color.color_primary),
        contentColor = colorResource(id = R.color.woo_white),
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(id = R.string.order_creation_add_customer_content_description)
        )
    }
}

@Preview
@Composable
fun OrderCustomerListScreenPreview() {
    WooThemeWithBackground {
        OrderCustomerListScreen(
            state = CustomerListViewState(
                searchHint = R.string.order_creation_customer_search_hint,
                searchQuery = "",
                searchFocused = false,
                showFab = true,
                searchModes = emptyList(),
                partialLoading = true,
                body = CustomerListViewState.CustomerList.Loaded(
                    customers = listOf(
                        CustomerListViewState.CustomerList.Item.Customer(
                            remoteId = 1,
                            name = CustomerListViewState.CustomerList.Item.Customer.Text.Highlighted("John Doe", 0, 1),
                            email = CustomerListViewState.CustomerList.Item.Customer.Text.Highlighted(
                                "email@email.com",
                                3,
                                10
                            ),
                            username = CustomerListViewState.CustomerList.Item.Customer.Text.Highlighted(
                                "· JohnDoe",
                                3,
                                6
                            ),

                            payload = WCCustomerModel(),
                        ),
                        CustomerListViewState.CustomerList.Item.Customer(
                            remoteId = 2,
                            name = CustomerListViewState.CustomerList.Item.Customer.Text.Highlighted(
                                "Andrei Kdn",
                                5,
                                8
                            ),
                            email = CustomerListViewState.CustomerList.Item.Customer.Text.Highlighted(
                                "blabla@email.com",
                                3,
                                10
                            ),
                            username = CustomerListViewState.CustomerList.Item.Customer.Text.Highlighted(
                                "· AndreiDoe",
                                3,
                                6
                            ),

                            payload = WCCustomerModel(),
                        ),
                        CustomerListViewState.CustomerList.Item.Customer(
                            remoteId = 0L,
                            name = CustomerListViewState.CustomerList.Item.Customer.Text.Placeholder("No name"),
                            email = CustomerListViewState.CustomerList.Item.Customer.Text.Placeholder("No email"),
                            username = CustomerListViewState.CustomerList.Item.Customer.Text.Placeholder(""),

                            payload = WCCustomerModel(),
                        ),
                        CustomerListViewState.CustomerList.Item.Loading,
                    ),
                    shouldResetScrollPosition = true
                ),
            ),
            onNavigateBack = {},
            onAddCustomerClicked = {},
            onCustomerSelected = {},
            onSearchQueryChanged = {},
            onSearchTypeChanged = {},
            onEndOfListReached = {},
        )
    }
}
