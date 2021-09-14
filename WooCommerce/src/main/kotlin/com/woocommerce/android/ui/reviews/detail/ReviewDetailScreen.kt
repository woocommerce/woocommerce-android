package com.woocommerce.android.ui.reviews.detail

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun ReviewDetailScreen() {
    HomeScreen("Test")
}

@Preview("Review Detail")
@Composable
fun PreviewReviewDetailScreen() {
    HomeScreen("Test")
}

@Composable
fun HomeScreen(text: String) {
    Text(text)
}
