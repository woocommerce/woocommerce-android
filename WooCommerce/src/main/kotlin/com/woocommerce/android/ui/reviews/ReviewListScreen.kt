package com.woocommerce.android.ui.reviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.InfiniteListHandler
import com.woocommerce.android.ui.compose.component.WCPullToRefreshBox
import com.woocommerce.android.ui.compose.component.WCSwitch

@Composable
fun ReviewListScreen(
    viewModel: ReviewListViewModel,
    onReviewClick: (ProductReview) -> Unit,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewState by viewModel.viewStateData.liveData.observeAsState()
    val reviews by viewModel.reviewList.observeAsState()

    viewState?.let { state ->
        ReviewListScreen(
            reviews = reviews.orEmpty(),
            viewState = state,
            onReviewClick = onReviewClick,
            onRefresh = { viewModel.forceRefreshReviews() },
            onLoadMore = { viewModel.loadMoreReviews() },
            onUnreadFilterChanged = { viewModel.onUnreadReviewsFilterChanged(it) },
            onLearnMoreClick = onLearnMoreClick,
            modifier = modifier
        )
    }
}

@Composable
private fun ReviewListScreen(
    reviews: List<ProductReview>,
    viewState: ReviewListViewModel.ViewState,
    onReviewClick: (ProductReview) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onUnreadFilterChanged: (Boolean) -> Unit,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        UnreadFilterRow(
            isChecked = viewState.isUnreadFilterEnabled,
            onCheckedChange = onUnreadFilterChanged
        )
        if (viewState.isSkeletonShown == true) {
            ReviewListSkeleton()
        } else {
            if (reviews.isEmpty()) {
                ReviewListEmptyView(
                    isUnreadFilterEnabled = viewState.isUnreadFilterEnabled,
                    onLearnMoreClick = onLearnMoreClick,
                    modifier = Modifier.weight(1f)
                )
            } else {
                ReviewListContent(
                    reviews = reviews,
                    isRefreshing = viewState.isRefreshing == true,
                    isLoadingMore = viewState.isLoadingMore == true,
                    onReviewClick = onReviewClick,
                    onRefresh = onRefresh,
                    onLoadMore = onLoadMore,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun UnreadFilterRow(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.product_review_list_unread_reviews_filter),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            WCSwitch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
        HorizontalDivider()
    }
}

@Composable
private fun ReviewListContent(
    reviews: List<ProductReview>,
    isRefreshing: Boolean,
    isLoadingMore: Boolean,
    onReviewClick: (ProductReview) -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val groupedReviews = remember(reviews) {
        reviews.groupBy { TimeGroup.getTimeGroupForDate(it.dateCreated) }
            .toSortedMap(compareBy { it.ordinal })
    }

    WCPullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
        ) {
            groupedReviews.forEach { (timeGroup, groupReviews) ->
                stickyHeader(key = timeGroup.name) {
                    TimeGroupHeader(timeGroup = timeGroup)
                }
                items(
                    items = groupReviews,
                    key = { it.remoteId }
                ) { review ->
                    val isLastInGroup = review == groupReviews.last()
                    ReviewListItem(
                        review = review,
                        onClicked = { onReviewClick(review) },
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        showDivider = !isLastInGroup
                    )
                }
            }
            if (isLoadingMore) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        InfiniteListHandler(listState = listState, onLoadMore = onLoadMore)
    }
}

@Composable
private fun TimeGroupHeader(
    timeGroup: TimeGroup,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(timeGroup.labelRes),
        style = MaterialTheme.typography.titleSmall,
        color = colorResource(R.color.color_on_surface_medium),
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp)
    )
}

@Composable
private fun ReviewListSkeleton(
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(5) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SkeletonView(width = 24.dp, height = 24.dp)

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SkeletonView(width = 260.dp, height = 16.dp)
                    SkeletonView(width = 120.dp, height = 16.dp)
                    SkeletonView(width = 60.dp, height = 16.dp)
                    Spacer(modifier = Modifier)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun ReviewListEmptyView(
    isUnreadFilterEnabled: Boolean,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Image(
            painter = painterResource(id = R.drawable.img_empty_reviews),
            contentDescription = null,
            modifier = Modifier.sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
        )

        Text(
            text = stringResource(
                id = if (isUnreadFilterEnabled) {
                    R.string.empty_review_filtered_list_title
                } else {
                    R.string.empty_review_list_title
                }
            ),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(
                id = if (isUnreadFilterEnabled) {
                    R.string.empty_review_filtered_list_message
                } else {
                    R.string.empty_review_list_message
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        if (!isUnreadFilterEnabled) {
            TextButton(onClick = onLearnMoreClick) {
                Text(text = stringResource(R.string.learn_more))
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
