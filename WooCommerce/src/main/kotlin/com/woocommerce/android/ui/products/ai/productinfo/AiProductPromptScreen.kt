package com.woocommerce.android.ui.products.ai.productinfo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.Toolbar

@Composable
fun AiProductPromptScreen(viewModel: AiProductPromptViewModel) {
    BackHandler(onBack = viewModel::onBackButtonClick)

    viewModel.state.observeAsState().value?.let { state ->
        AiProductPromptScreen(
            uiState = state,
            onBackButtonClick = viewModel::onBackButtonClick,
        )
    }
}

@Composable
fun AiProductPromptScreen(
    uiState: AiProductPromptViewModel.AiProductPromptState,
    onBackButtonClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            Toolbar(
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationButtonClick = onBackButtonClick,
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
        ) {
            Text(
                text = "Product creation with AI V2 Fragment",
                style = MaterialTheme.typography.subtitle1,
                color = colorResource(id = R.color.color_on_surface_medium)
            )
        }
    }
}
