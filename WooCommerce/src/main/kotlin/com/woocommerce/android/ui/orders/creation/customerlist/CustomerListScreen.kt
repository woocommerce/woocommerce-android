package com.woocommerce.android.ui.orders.creation.customerlist

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParams
import com.woocommerce.android.ui.compose.component.SearchLayoutWithParamsState
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewState.CustomerList.Item.Customer
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

@Composable
fun CustomerListScreen(viewModel: CustomerListViewModel) {
    val state by viewModel.viewState.observeAsState()

    state?.let {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.order_creation_add_customer)) },
                    navigationIcon = {
                        IconButton(viewModel::onNavigateBack) {
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
                if (it.showFab) CustomerListAddCustomerButton(viewModel::onAddCustomerClicked)
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
                hint = state.searchHint,
                searchQuery = state.searchQuery,
                isSearchFocused = state.searchFocused,
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

        PartialLoadingIndicator(state)

        when (val body = state.body) {
            is CustomerListViewState.CustomerList.Empty -> CustomerListEmpty(
                body.message,
                body.image,
                body.button,
            )

            is CustomerListViewState.CustomerList.Error -> CustomerListError(body.message)
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
private fun PartialLoadingIndicator(state: CustomerListViewState) {
    val spacerHeightWithLoading = 8.dp
    val spacerHeightWithoutLoading = 6.dp
    if (state.partialLoading) {
        Spacer(modifier = Modifier.height(spacerHeightWithLoading))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacerHeightWithoutLoading - spacerHeightWithLoading)
        )
    } else {
        Spacer(modifier = Modifier.height(spacerHeightWithoutLoading))
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

@Composable
private fun CustomerListLoaded(
    body: CustomerListViewState.CustomerList.Loaded,
    onCustomerSelected: (WCCustomerModel) -> Unit,
    onEndOfListReached: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(key1 = body) {
        if (body.shouldResetScrollPosition) listState.scrollToItem(0)
    }

    LazyColumn(
        state = listState,
    ) {
        itemsIndexed(
            items = body.customers,
        ) { _, customer ->
            when (customer) {
                is Customer -> {
                    CustomerListItem(
                        customer = customer,
                        showGuestChip = body.showGuestChip,
                        onCustomerSelected = onCustomerSelected
                    )
                    if (customer != body.customers.last()) {
                        CustomerListDivider()
                    } else {
                        Spacer(modifier = Modifier.height(0.dp))
                    }
                }

                CustomerListViewState.CustomerList.Item.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }

        if (body.customers.lastOrNull() !is CustomerListViewState.CustomerList.Item.Loading) {
            item {
                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))
            }
        }
    }

    InfiniteListHandler(listState = listState, buffer = 3) {
        onEndOfListReached()
    }
}

@Composable
private fun CustomerListItem(
    customer: Customer,
    showGuestChip: Boolean,
    onCustomerSelected: (WCCustomerModel) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = true,
                role = Role.Button,
                onClick = { onCustomerSelected(customer.payload) }
            )
            .padding(
                horizontal = dimensionResource(id = R.dimen.major_100),
                vertical = dimensionResource(id = R.dimen.minor_100)
            )
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = customer.name.render(),
                    color = colorResource(id = R.color.color_on_surface),
                    style = MaterialTheme.typography.subtitle1,
                    fontWeight = FontWeight.W500,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = customer.username.render(),
                    color = colorResource(id = R.color.color_on_surface_medium),
                    style = MaterialTheme.typography.subtitle1,
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = customer.email.render(),
                color = colorResource(id = R.color.color_on_surface),
                style = MaterialTheme.typography.body2,
            )
        }

        if (showGuestChip && customer.isGuest) {
            Box(
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.color_on_surface_disabled),
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_100))
                    )
                    .padding(
                        vertical = dimensionResource(id = R.dimen.minor_50),
                        horizontal = dimensionResource(id = R.dimen.minor_100)
                    )
            ) {
                Text(
                    text = stringResource(id = R.string.customer_picker_guest),
                    color = colorResource(id = R.color.woo_white)
                )
            }
        }
    }
}

@Composable
private fun Customer.Text.render() =
    when (this) {
        is Customer.Text.Highlighted -> buildHighlightedText()

        is Customer.Text.Placeholder -> buildPlaceholder()
    }

@Composable
private fun Customer.Text.Placeholder.buildPlaceholder() =
    buildAnnotatedString {
        withStyle(
            SpanStyle(
                color = colorResource(id = R.color.color_on_surface_disabled),
            )
        ) {
            append(text)
        }
    }

@Composable
private fun Customer.Text.Highlighted.buildHighlightedText() =
    buildAnnotatedString {
        if (start >= end) {
            append(text)
            return@buildAnnotatedString
        }
        append(text.substring(0, start))
        withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
            append(text.substring(start, end))
        }
        append(text.substring(end, text.length))
    }

@Composable
private fun CustomerListEmpty(
    @StringRes message: Int,
    @DrawableRes image: Int,
    button: Button?,
) {
    CustomerListNoDataState(
        text = message,
        image = image,
        button = button,
    )
}

@Composable
private fun CustomerListError(@StringRes message: Int) {
    CustomerListNoDataState(
        text = message,
        image = R.drawable.img_woo_generic_error,
        button = null,
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
private fun CustomerListNoDataState(
    @StringRes text: Int,
    @DrawableRes image: Int,
    button: Button?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimensionResource(id = R.dimen.major_200)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Image(
            painter = painterResource(id = image),
            contentDescription = null,
        )
        Spacer(Modifier.size(dimensionResource(id = R.dimen.major_200)))
        Text(
            text = stringResource(id = text),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(
                start = dimensionResource(id = R.dimen.major_150),
                end = dimensionResource(id = R.dimen.major_150)
            )
        )

        button?.let {
            Spacer(Modifier.size(dimensionResource(id = R.dimen.minor_100)))
            WCColoredButton(onClick = button.onClick) {
                Text(text = stringResource(id = button.text))
            }
        }

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
            searchHint = R.string.order_creation_customer_search_hint,
            searchQuery = "",
            searchFocused = false,
            showFab = true,
            searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_username,
                    searchParam = "username",
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
                    Customer(
                        remoteId = 1,
                        name = Customer.Text.Highlighted("John Doe", 0, 1),
                        email = Customer.Text.Highlighted("email@email.com", 3, 10),
                        username = Customer.Text.Highlighted("· JohnDoe", 3, 6),

                        payload = WCCustomerModel(),
                    ),
                    Customer(
                        remoteId = 2,
                        name = Customer.Text.Highlighted("Andrei Kdn", 5, 8),
                        email = Customer.Text.Highlighted("blabla@email.com", 3, 10),
                        username = Customer.Text.Highlighted("· AndreiDoe", 3, 6),

                        payload = WCCustomerModel(),
                    ),
                    Customer(
                        remoteId = 0L,
                        name = Customer.Text.Placeholder("No name"),
                        email = Customer.Text.Placeholder("No email"),
                        username = Customer.Text.Placeholder(""),

                        payload = WCCustomerModel(),
                    ),
                    CustomerListViewState.CustomerList.Item.Loading,
                ),
                shouldResetScrollPosition = true,
                showGuestChip = true,
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
fun CustomerListScreenEmptyOldPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchHint = R.string.order_creation_customer_search_hint,
            searchQuery = "search",
            showFab = false,
            searchFocused = false,
            searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_username,
                    searchParam = "username",
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
            body = CustomerListViewState.CustomerList.Empty(
                message = R.string.order_creation_customer_search_empty_on_old_version_wcpay,
                image = R.drawable.img_search_suggestion,
                button = Button(
                    R.string.order_creation_customer_search_empty_add_details_manually,
                    {},
                )
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
fun CustomerListScreenEmptyNewPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchHint = R.string.order_creation_customer_search_hint,
            searchQuery = "search",
            showFab = true,
            searchFocused = false,
            searchModes = emptyList(),
            body = CustomerListViewState.CustomerList.Empty(
                message = R.string.order_creation_customer_search_empty,
                image = R.drawable.img_empty_search,
                button = null
            ),
        ),
        {},
        {},
        {},
        {},
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun CustomerListScreenErrorPreview() {
    CustomerListScreen(
        modifier = Modifier,
        state = CustomerListViewState(
            searchHint = R.string.order_creation_customer_search_old_wc_hint,
            searchQuery = "search",
            showFab = true,
            searchFocused = false,
            searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_username,
                    searchParam = "username",
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
            body = CustomerListViewState.CustomerList.Error(R.string.error_generic),
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
            searchHint = R.string.order_creation_customer_search_hint,
            searchQuery = "",
            showFab = true,
            searchFocused = false,
            searchModes = listOf(
                SearchMode(
                    labelResId = R.string.order_creation_customer_search_username,
                    searchParam = "username",
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
