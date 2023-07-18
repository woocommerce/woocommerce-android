package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParams
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParamsState
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

@Composable
fun CustomerListScreen(viewModel: CustomerListViewModel) {
    val state by viewModel.viewState.observeAsState()

    state?.let {
        Scaffold(
            topBar = {
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
            },
            floatingActionButton = {
                CustomerListAddCustomerButton(viewModel::onAddCustomerClicked)
            }
        ) { padding ->
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
    onCustomerSelected: (WCCustomerModel) -> Unit,
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
                isSearchFocused = false,
                areSearchTypesAlwaysVisible = true,
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

        if (state.partialLoading) {
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

        when (val body = state.body) {
            CustomerListViewState.CustomerList.Empty -> CustomerListEmpty()
            CustomerListViewState.CustomerList.Error -> CustomerListError()
            CustomerListViewState.CustomerList.Loading -> CustomerListSkeleton()
            is CustomerListViewState.CustomerList.Loaded -> {
                CustomerListLoaded(
                    body,
                    onCustomerSelected,
                    onEndOfListReached,
                )
            }
        }
    }
}

@Composable
private fun CustomerListAddCustomerButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        backgroundColor = colorResource(id = R.color.color_primary),
    ) {
        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add coupon")
    }
}

@Composable
private fun CustomerListLoaded(
    body: CustomerListViewState.CustomerList.Loaded,
    onCustomerSelected: (WCCustomerModel) -> Unit,
    onEndOfListReached: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = body) {
        if (body.firstPageLoaded) listState.scrollToItem(0)
    }

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
                        CustomerListDivider()
                    } else {
                        Spacer(modifier = Modifier.height(0.dp))
                    }
                }

                CustomerListViewState.CustomerList.Item.Loading -> CustomerListLoadingItem()
            }
        }
    }

    InfiniteListHandler(listState = listState, buffer = 3) {
        onEndOfListReached()
    }
}

@Composable
private fun CustomerListItem(
    customer: CustomerListViewState.CustomerList.Item.Customer,
    onCustomerSelected: (WCCustomerModel) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                onClickLabel = stringResource(id = R.string.coupon_list_view_coupon),
                role = Role.Button,
                onClick = { onCustomerSelected(customer.payload) }
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            )
    ) {
        Text(
            text = "${customer.firstName} ${customer.lastName}",
            style = MaterialTheme.typography.subtitle1,
            fontWeight = FontWeight.W700,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = customer.email,
            style = MaterialTheme.typography.body2,
        )
    }
}

@Composable
private fun CustomerListEmpty() {
    CustomerListNoDataState(
        text = R.string.order_creation_customer_search_empty,
        image = R.drawable.img_empty_search
    )
}

@Composable
private fun CustomerListError() {
    CustomerListNoDataState(
        text = R.string.error_generic,
        image = R.drawable.img_woo_generic_error
    )
}

@Composable
private fun CustomerListSkeleton() {
    val numberOfSkeletonRows = 10
    LazyColumn(
        Modifier.background(color = MaterialTheme.colors.surface)
    ) {
        repeat(numberOfSkeletonRows) {
            item {
                CustomerListLoadingItem()

                if (it != numberOfSkeletonRows - 1) {
                    CustomerListDivider()
                } else {
                    Spacer(modifier = Modifier.height(0.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomerListNoDataState(@StringRes text: Int, @DrawableRes image: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.major_200)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(id = text),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_325)))
        Image(
            painter = painterResource(id = image),
            contentDescription = null,
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun CustomerListLoadingItem() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            )
    ) {
        Column {
            SkeletonView(
                modifier = Modifier
                    .width(dimensionResource(id = R.dimen.skeleton_text_large_width))
                    .height(dimensionResource(id = R.dimen.skeleton_text_height_100))
            )
            Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_50)))
            SkeletonView(
                modifier = Modifier
                    .width(dimensionResource(id = R.dimen.skeleton_text_extra_large_width))
                    .height(dimensionResource(id = R.dimen.skeleton_text_height_75))
            )
        }
    }
}

@Composable
private fun CustomerListDivider() {
    Divider(
        modifier = Modifier
            .offset(x = dimensionResource(id = R.dimen.major_100)),
        color = colorResource(id = R.color.divider_color),
        thickness = dimensionResource(id = R.dimen.minor_10)
    )
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
            partialLoading = true,
            body = CustomerListViewState.CustomerList.Loaded(
                customers = listOf(
                    CustomerListViewState.CustomerList.Item.Customer(
                        remoteId = 1,
                        firstName = "John",
                        lastName = "Doe",
                        email = "John@gmail.com",

                        payload = WCCustomerModel(),
                    ),
                    CustomerListViewState.CustomerList.Item.Customer(
                        remoteId = 2,
                        firstName = "Andrei",
                        lastName = "K",
                        email = "blac@aaa.com",

                        payload = WCCustomerModel(),
                    ),
                    CustomerListViewState.CustomerList.Item.Loading,
                ),
                firstPageLoaded = true,
            ),
        ),
        {},
        {},
        {},
        {},
    )
}

@Preview
@Composable
fun CustomerListScreenEmptyPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchQuery = "search",
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
            body = CustomerListViewState.CustomerList.Empty,
        ),
        {},
        {},
        {},
        {},
    )
}

@Preview
@Composable
fun CustomerListScreenErrorPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchQuery = "search",
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
            body = CustomerListViewState.CustomerList.Error,
        ),
        {},
        {},
        {},
        {},
    )
}

@Preview
@Composable
fun CustomerListScreenLoadingPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchQuery = "search",
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
            body = CustomerListViewState.CustomerList.Loading,
        ),
        {},
        {},
        {},
        {},
    )
}
