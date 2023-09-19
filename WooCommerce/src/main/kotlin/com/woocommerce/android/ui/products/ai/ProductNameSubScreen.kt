package com.woocommerce.android.ui.products.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProductNameSubScreen(viewModel: ProductNameSubViewModel, modifier: Modifier) {
    Column(modifier = modifier.background(MaterialTheme.colors.surface)) {
        Text(text = "The product name step")

        Spacer(modifier = Modifier.weight(1f))

        Button(onClick = viewModel::onDoneClick) {
            Text(text = "Continue")
        }
    }
}
