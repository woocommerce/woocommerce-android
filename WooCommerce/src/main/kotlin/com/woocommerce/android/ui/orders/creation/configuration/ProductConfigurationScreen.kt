package com.woocommerce.android.ui.orders.creation.configuration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R


@Composable
fun ProductConfigurationScreen(viewModel: ProductConfigurationViewModel) {
    val viewState by viewModel.viewState.collectAsState()
    BackHandler(onBack = viewModel::onCancel)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(id = R.string.product_configuration_title)) },
            navigationIcon = {
                IconButton(viewModel::onCancel) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = R.string.close)
                    )
                }
            },
            backgroundColor = colorResource(id = R.color.color_toolbar),
            elevation = 0.dp,
        )
    }) { padding ->
        when (val state = viewState) {
            is ProductConfigurationViewModel.ViewState.Error -> Text(text = state.message)
            is ProductConfigurationViewModel.ViewState.Loading -> Text(text = "Loading")
            is ProductConfigurationViewModel.ViewState.DisplayConfiguration -> {
                ProductConfigurationScreen(
                    productConfiguration = state.productConfiguration,
                    onSaveConfigurationClick = {},
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
fun ProductConfigurationScreen(
    productConfiguration: ProductConfiguration,
    onSaveConfigurationClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize()) {
        Text(
            text = "Configuration Keys: ${productConfiguration.configuration.keys} Values: ${productConfiguration.configuration.values}",
            modifier = modifier
        )
        Text(
            text = "Configuration Keys: ${productConfiguration.childrenConfiguration?.keys} Values: ${productConfiguration.childrenConfiguration?.values}",
            modifier = modifier
        )
        Button(onClick = onSaveConfigurationClick) {
            Text(text = "Save Configuration")
        }
    }
}
