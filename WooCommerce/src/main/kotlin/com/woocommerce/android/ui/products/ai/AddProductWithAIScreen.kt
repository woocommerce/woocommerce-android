package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun AddProductWithAIScreen(viewModel: AddProductWithAIViewModel) {
    viewModel.state.observeAsState().value?.let {
        AddProductWithAIScreen(
            state = it,
            onDismissClick = viewModel::onDismissClick
        )
    }
}

@Composable
fun AddProductWithAIScreen(
    state: AddProductWithAIViewModel.State,
    onDismissClick: () -> Unit
) {
    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = Icons.Filled.Clear,
                onNavigationButtonClick = onDismissClick
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            LinearProgressIndicator(progress = state.progress, modifier = Modifier.fillMaxWidth())

            SubScreen(
                subViewModel = state.subViewModel,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SubScreen(subViewModel: AddProductWithAISubViewModel<*>, modifier: Modifier) {
    when (subViewModel) {
        is ProductNameSubViewModel -> ProductNameSubScreen(subViewModel, modifier)
    }
}
