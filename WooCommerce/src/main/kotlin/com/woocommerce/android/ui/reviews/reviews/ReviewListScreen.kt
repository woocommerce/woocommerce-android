package com.woocommerce.android.ui.reviews.reviews

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.material.composethemeadapter.MdcTheme
import com.gowtham.ratingbar.RatingBar
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.ProductReviewProduct
import com.woocommerce.android.ui.common.compose.elements.HtmlText
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.util.StringUtils
import dagger.hilt.android.internal.managers.ViewComponentManager
import java.text.SimpleDateFormat
import java.util.Date

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
    MdcTheme(
        setTextColors = true,
        setDefaultFontFamily = true,
    ) {
        Box(
            modifier =
            Modifier.background(MaterialTheme.colors.background)
        ) {
            if (reviews.isNotEmpty()) {
                ReviewList(
                    reviews = reviews,
                    isRefreshing = uiState.isRefreshing ?: false,
                    onRefresh = onRefresh
                )
            }
        }
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
        val groups = reviews.groupBy { it.dateCreated }
        LazyColumn {
            groups.forEach {
                val (date, dateReviews) = it
                item { ReviewItemHeader(date) }
                items(dateReviews) { review ->
                    val context = LocalContext.current
                    ReviewItem(review) {
                        openReviewDetail(context, review)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItem(
    review: ProductReview,
    onReviewClick: () -> Unit
) {
    Card(
        elevation = 1.dp,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onReviewClick.invoke() }
                .padding(
                    horizontal = 16.dp,
                    vertical = 16.dp,
                )
        ) {
            Image(
                painter = painterResource(R.drawable.ic_comment),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            ReviewItemInfoColumn(review)
        }
    }
}

private fun openReviewDetail(context: Context, review: ProductReview) {
    ((context as ViewComponentManager.FragmentContextWrapper).baseContext as MainNavigationRouter)
        .showReviewDetail(
            review.remoteId,
            launchedFromNotification = false,
            enableModeration = true,
            tempStatus = ""
        )
}

@Composable
private fun ReviewItemInfoColumn(review: ProductReview) {
    Column {
        Text(
            text = if (review.product == null) {
                stringResource(R.string.product_review_list_item_title, review.reviewerName)
            } else {
                stringResource(
                    R.string.review_list_item_title,
                    review.reviewerName,
                    review.product?.name?.fastStripHtml().orEmpty()
                )
            },
            style = MaterialTheme.typography.subtitle2,
        )
        Spacer(modifier = Modifier.heightIn(2.dp))
        val reviewText: String = StringUtils.getRawTextFromHtml(review.review)
        val reviewTextFormatted =
            if (ProductReviewStatus.fromString(review.status) == ProductReviewStatus.HOLD) {
                val pendingReviewText = "<font color=${colorResource(R.color.woo_orange_50).value}>" +
                    "${stringResource(R.string.pending_review_label)}</font>"
                "$pendingReviewText • $reviewText"
            } else {
                reviewText
            }
        HtmlText(
            html = reviewTextFormatted,
            style = R.style.Woo_TextView_Body1,
        )
        Spacer(modifier = Modifier.heightIn(6.dp))
        RatingBar(
            numStars = 5,
            value = review.rating.toFloat(),
            isIndicator = true,
            onRatingChanged = {},
            padding = 0.dp,
            onValueChange = {},
            activeColor = colorResource(R.color.woo_black_90),
            hideInactiveStars = true,
            modifier = Modifier.height(12.dp)
        )
    }
}

@SuppressLint("SimpleDateFormat")
@Composable
private fun ReviewItemHeader(date: Date) {
    Text(
        text = SimpleDateFormat("dd-MMMM-yyyy").format(date),
        style = MaterialTheme.typography.caption,
        modifier = Modifier.padding(all = 8.dp)
    )
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
