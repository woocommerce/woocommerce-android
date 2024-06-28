package com.woocommerce.android.ui.customer

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R

@Composable
fun MenuCustomerListScreen(viewModel: CustomerListDetailsViewModel) {
    val state by viewModel.viewState.observeAsState()

    state?.let {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(id = R.string.more_menu_customers_title)) },
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
