package com.woocommerce.android.ui.orders.creation.customerlistnew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
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
                onSearchQueryChanged = viewModel::onSearchQueryChanged,
                onSearchTypeChanged = viewModel::onSearchTypeChanged
            )
        }
    }
}

@Composable
fun CustomerListScreen(
    modifier: Modifier = Modifier,
    state: CustomerListViewState,
    onSearchQueryChanged: (String) -> Unit,
    onSearchTypeChanged: (Int) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.surface)
    ) {
        SearchLayoutWithParams(
            state = SearchLayoutWithParamsState(
                hint = R.string.order_creation_customer_search_hint,
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
    }
}
