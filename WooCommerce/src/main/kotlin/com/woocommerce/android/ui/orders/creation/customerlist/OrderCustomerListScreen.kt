@file:JvmName("CustomerListSelectionScreenKt")

package com.woocommerce.android.ui.orders.creation.customerlist

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
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
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.customer.CustomerListScreen
import org.wordpress.android.fluxc.model.customer.WCCustomerModel

/**
 * @param handleInsets true if the screen should handle insets manually.
 *  This is needed when the screen is used in a DialogFragment, otherwise the handling of insets at the root
 *  layout will be enough.
 *
 *  Note: normally this shouldn't be needed, as we consume the insets in the root layout, but due to this
 *  bug https://issuetracker.google.com/issues/411868840 the insets are re-applied when the keyboard is shown.
 */
@Composable
fun OrderCustomerListScreen(
    viewModel: CustomerListSelectionViewModel,
    handleInsets: Boolean
) {
    val state by viewModel.viewState.observeAsState()
    state?.let {
        OrderCustomerListScreen(
            state = it,
            handleInsets = handleInsets,
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
    handleInsets: Boolean,
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
            Toolbar(
                title = stringResource(id = R.string.order_creation_add_customer),
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = onNavigateBack,
                windowInsets = if (handleInsets) WindowInsets.statusBars else WindowInsets(0),
            )
        },
        floatingActionButton = {
            if (state.showFab) CustomerListAddCustomerButton(onAddCustomerClicked)
        },
        modifier = modifier
    ) { padding ->
        CustomerListScreen(
            modifier = Modifier
                .padding(padding)
                .then(if (handleInsets) Modifier.navigationBarsPadding().imePadding() else Modifier),
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
            handleInsets = false,
            onNavigateBack = {},
            onAddCustomerClicked = {},
            onCustomerSelected = {},
            onSearchQueryChanged = {},
            onSearchTypeChanged = {},
            onEndOfListReached = {},
        )
    }
}
