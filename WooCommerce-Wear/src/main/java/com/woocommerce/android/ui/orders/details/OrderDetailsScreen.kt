package com.woocommerce.android.ui.orders.details

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.presentation.component.LoadingScreen

@Composable
fun OrderDetailsScreen(viewModel: OrderDetailsViewModel) {
    val viewState = viewModel.viewState.observeAsState()
    OrderDetailsScreen(
        isLoading = viewState.value?.isLoading ?: false
    )
}

@Composable
fun OrderDetailsScreen(
    isLoading: Boolean
) {
    Column {
        if (isLoading) {
            LoadingScreen()
        } else {
            OrderDetailsContent()
        }
    }
}

@Composable
fun OrderDetailsContent() {
    Text("Order Details Content")
}
