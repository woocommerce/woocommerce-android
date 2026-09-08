package com.woocommerce.android.ui.moremenu.customer

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar
import com.woocommerce.android.ui.customer.CustomerListScreen
import com.woocommerce.android.ui.customer.canScrollBackward

@Composable
fun MenuCustomerListScreen(viewModel: CustomerListDetailsViewModel) {
    val state by viewModel.viewState.observeAsState()
    val listState = rememberLazyListState()
    val emptyScrollState = rememberScrollState()
    val skeletonListState = rememberLazyListState()

    state?.let {
        Scaffold(
            topBar = {
                Toolbar(
                    title = stringResource(id = R.string.more_menu_customers_title),
                    onNavigationButtonClick = viewModel::onNavigateBack,
                    showDivider = it.canScrollBackward(listState, emptyScrollState, skeletonListState),
                )
            }
        ) { padding ->
            CustomerListScreen(
                modifier = Modifier.padding(padding),
                state = it,
                onCustomerSelected = viewModel::onCustomerSelected,
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSearchTypeChanged = viewModel::onSearchTypeChanged,
                onEndOfListReached = viewModel::onEndOfListReached,
                listState = listState,
                emptyScrollState = emptyScrollState,
                skeletonListState = skeletonListState,
            )
        }
    }
}
