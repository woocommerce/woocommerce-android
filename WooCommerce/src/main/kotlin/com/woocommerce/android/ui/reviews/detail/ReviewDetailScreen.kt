package com.woocommerce.android.ui.reviews.detail

import android.content.res.Configuration
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.google.android.material.composethemeadapter.MdcTheme
import com.woocommerce.android.databinding.SkeletonNotifDetailBinding
import com.woocommerce.android.model.ProductReview
import java.util.Date

@Composable
fun ReviewDetailScreen(viewModel: ReviewDetailViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    ReviewDetailScreen(uiState)
}

@Composable
fun ReviewDetailScreen(uiState: ReviewDetailViewModel.ViewState) {
    MdcTheme {
        if (uiState.isSkeletonShown == true) {
            ReviewDetailSkeleton()
        } else {
            ReviewDetailCard()
        }
    }
}

@Composable
private fun ReviewDetailSkeleton() {
    AndroidViewBinding(SkeletonNotifDetailBinding::inflate)
}

@Composable
private fun ReviewDetailCard() {
    Card(
        backgroundColor = MaterialTheme.colors.surface,
    ) {

    }
}

@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewReviewDetailScreen() {
    ReviewDetailScreen(
        ReviewDetailViewModel.ViewState(
            productReview = ProductReview(
                remoteId = 1L,
                dateCreated = Date(),
                review = "all nice",
                rating = 3,
                reviewerName = "Andrey",
                reviewerAvatarUrl = "https://google.com",
                remoteProductId = 5L,
                status = "Done",
                read = false,
                product = null
            )
        )
    )
}

@Preview(name = "Skeleton")
@Composable
fun PreviewReviewDetailScreenSkeleton() {
    ReviewDetailScreen(
        ReviewDetailViewModel.ViewState(
            isSkeletonShown = true
        )
    )
}
