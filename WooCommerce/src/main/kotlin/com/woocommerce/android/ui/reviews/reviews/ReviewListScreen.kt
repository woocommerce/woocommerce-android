package com.woocommerce.android.ui.reviews.reviews

import android.content.res.Configuration
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import java.util.Date
import androidx.compose.foundation.lazy.items

@Composable
fun ReviewListScreen(viewModel: ReviewListViewModel) {
    val uiState by viewModel.viewStateData.collectAsState()
    val reviews by viewModel.reviewList.observeAsState(emptyList())

    ReviewListScreen(
        uiState,
        reviews
    ) { viewModel.forceRefreshReviews() }
}

@Composable
fun ReviewListScreen(
    uiState: ReviewListViewModel.ViewState,
    reviews: List<ProductReview>,
    onRefresh: () -> Unit
) {
    if (reviews.isNotEmpty()) {
        ReviewList(
            reviews = reviews,
            isRefreshing = uiState.isRefreshing ?: false,
            onRefresh = onRefresh
        )
    }
}

@Composable
private fun ReviewList(
    reviews: List<ProductReview>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(isRefreshing),
        onRefresh = onRefresh,
    ) {
        LazyColumn {
            items(reviews) { review ->
                ReviewItem(review)
            }
        }
    }
}

@Composable
private fun ReviewItem(review: ProductReview) {

}

// region Preview
@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewReviewListScreen() {
    ReviewListScreen(
        uiState = ReviewListViewModel.ViewState(),
        reviews = listOf(
            ProductReview(
                remoteId = 1L,
                dateCreated = Date(),
                review = "all nice bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla bla",
                rating = 3,
                reviewerName = "Andrey",
                reviewerAvatarUrl = "https://google.com",
                remoteProductId = 5L,
                status = "Done",
                read = false,
                product = ProductReviewProduct(
                    remoteProductId = 5,
                    name = "Good Product",
                    externalUrl = "url",
                )
            )
        ),
        onRefresh = {}
    )
}
// end-region
