package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParams
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParamsState

@Composable
fun CustomerListScreen(viewModel: CustomerListViewModel) {
    val state by viewModel.viewState.observeAsState()

    state?.let {
        Scaffold(topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.order_creation_fragment_title)) },
                navigationIcon = {
                    IconButton(viewModel::onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.back)
                        )
                    }
                },
                backgroundColor = colorResource(id = R.color.color_toolbar),
                elevation = 0.dp,
            )
        }) { padding ->
            CustomerListScreen(
                modifier = Modifier.padding(padding),
                state = it,
                onCustomerSelected = viewModel::onCustomerSelected,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSearchTypeChanged = viewModel::onSearchTypeChanged,
                onEndOfListReached = viewModel::onEndOfListReached,
            )
        }
    }
}

@Composable
fun CustomerListScreen(
    modifier: Modifier = Modifier,
    state: CustomerListViewState,
    onCustomerSelected: (Long) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onSearchTypeChanged: (Int) -> Unit,
    onEndOfListReached: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        SearchLayoutWithParams(
            state = SearchLayoutWithParamsState(
                hint = R.string.order_creation_customer_filter_hint,
                searchQuery = state.searchQuery,
                isActive = true,
                supportedSearchTypes = state.searchModes.map {
                    SearchLayoutWithParamsState.SearchType(
                        labelResId = it.labelResId,
                        isSelected = it.isSelected,
                    )
                }
            ),
            paramsFillWidth = false,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearchTypeSelected = onSearchTypeChanged,
        )

        Spacer(modifier = Modifier.height(8.dp))

        when (val body = state.body) {
            CustomerListViewState.CustomerList.Empty -> {
                // todo
            }

            CustomerListViewState.CustomerList.Error -> {
                // todo
            }

            CustomerListViewState.CustomerList.Loading -> {
                // todo
            }

            is CustomerListViewState.CustomerList.Loaded -> {
                CustomerListLoaded(
                    body,
                    onCustomerSelected,
                    onEndOfListReached,
                )
            }
        }.exhaustive
    }
}

@Composable
fun CustomerListLoaded(
    body: CustomerListViewState.CustomerList.Loaded,
    onCustomerSelected: (Long) -> Unit,
    onEndOfListReached: () -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
    ) {
        itemsIndexed(
            items = body.customers,
        ) { _, customer ->
            when (customer) {
                is CustomerListViewState.CustomerList.Item.Customer -> {
                    CustomerListItem(
                        customer = customer,
                        onCustomerSelected = onCustomerSelected
                    )
                    if (customer != body.customers.last()) {
                        Divider(
                            modifier = Modifier.offset(x = dimensionResource(id = R.dimen.major_100)),
                            color = colorResource(id = R.color.divider_color),
                            thickness = dimensionResource(id = R.dimen.minor_10)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(0.dp))
                    }
                }

                CustomerListViewState.CustomerList.Item.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentWidth()
                            .padding(vertical = dimensionResource(id = R.dimen.minor_100))
                    )
                }
            }.exhaustive
        }
    }

    InfiniteListHandler(listState = listState, buffer = 3) {
        onEndOfListReached()
    }
}

@Composable
fun CustomerListItem(
    customer: CustomerListViewState.CustomerList.Item.Customer,
    onCustomerSelected: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = R.string.coupon_list_view_coupon),
                role = Role.Button,
                onClick = { onCustomerSelected(customer.remoteId) }
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_50)
            )
    ) {
        Text(
            text = "${customer.firstName} ${customer.lastName}",
            style = MaterialTheme.typography.subtitle2,
            color = MaterialTheme.colors.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = customer.email,
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface
        )
    }
}

@Preview
@Composable
fun CustomerListScreenPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchQuery = "",
            searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_everything,
                    searchParam = "all",
                    isSelected = true,
                ),
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_name,
                    searchParam = "name",
                    isSelected = false,
                ),
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_email,
                    searchParam = "email",
                    isSelected = false,
                ),
            ),
            body = CustomerListViewState.CustomerList.Loaded(
                customers = listOf(
                    CustomerListViewState.CustomerList.Item.Customer(
                        remoteId = 1,
                        firstName = "John",
                        lastName = "Doe",
                        email = "John@gmail.com",
                    ),
                    CustomerListViewState.CustomerList.Item.Customer(
                        remoteId = 2,
                        firstName = "Andrei",
                        lastName = "K",
                        email = "blac@aaa.com",
                    ),
                    CustomerListViewState.CustomerList.Item.Loading,
                )
            ),
        ),
        {},
        {},
        {},
        {},
    )
}
