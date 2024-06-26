package com.woocommerce.android.ui.dashboard.reviews

import android.widget.RatingBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFilterableCardHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.reviews.ProductReviewStatus
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent

@Composable
fun DashboardReviewsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardReviewsViewModel = viewModelWithFactory { factory: DashboardReviewsViewModel.Factory ->
        factory.create(parentViewModel = parentViewModel)
    }
) {
    HandleEvents(viewModel.event)

    viewModel.viewState.observeAsState().value?.let { viewState ->
        DashboardReviewsCard(
            viewState = viewState,
            onHideClicked = { parentViewModel.onHideWidgetClicked(DashboardWidget.Type.REVIEWS) },
            onFilterSelected = viewModel::onFilterSelected,
            onViewAllClicked = viewModel::onViewAllClicked,
            onReviewClicked = viewModel::onReviewClicked,
            onContactSupportClicked = parentViewModel::onContactSupportClicked,
            onRetryClicked = viewModel::onRetryClicked,
            modifier = modifier
        )
    }
}

@Composable
private fun HandleEvents(event: LiveData<MultiLiveEvent.Event>) {
    val navController = rememberNavController()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(event, navController, lifecycleOwner) {
        val observer = Observer { event: MultiLiveEvent.Event ->
            when (event) {
                is DashboardReviewsViewModel.OpenReviewsList -> navController.navigateSafely(
                    DashboardFragmentDirections.actionDashboardToReviews()
                )

                is DashboardReviewsViewModel.OpenReviewDetail -> {
                    // Open the review list screen first as it's responsible for handling review status changes
                    navController.navigateSafely(
                        DashboardFragmentDirections.actionDashboardToReviews()
                    )
                    // Continue to the details screen
                    navController.navigateSafely(
                        directions = DashboardFragmentDirections.actionGlobalReviewDetailFragment(
                            launchedFromNotification = false,
                            remoteReviewId = event.review.remoteId
                        ),
                        skipThrottling = true
                    )
                }
            }
        }

        event.observe(lifecycleOwner, observer)

        onDispose {
            event.removeObserver(observer)
        }
    }
}

@Composable
private fun DashboardReviewsCard(
    viewState: DashboardReviewsViewModel.ViewState,
    onHideClicked: () -> Unit,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    onViewAllClicked: () -> Unit,
    onReviewClicked: (ProductReview) -> Unit,
    onContactSupportClicked: () -> Unit,
    onRetryClicked: () -> Unit,
    modifier: Modifier
) {
    WidgetCard(
        titleResource = DashboardWidget.Type.REVIEWS.titleResource,
        menu = DashboardWidgetMenu(
            listOf(
                DashboardWidget.Type.REVIEWS.defaultHideMenuEntry(onHideClicked)
            )
        ),
        button = DashboardViewModel.DashboardWidgetAction(
            titleResource = R.string.dashboard_reviews_card_view_all_button,
            action = onViewAllClicked
        ),
        isError = viewState is DashboardReviewsViewModel.ViewState.Error,
        modifier = modifier
    ) {
        when (viewState) {
            is DashboardReviewsViewModel.ViewState.Loading -> {
                ReviewsLoading(
                    selectedFilter = viewState.selectedFilter,
                    onFilterSelected = onFilterSelected,
                )
            }

            is DashboardReviewsViewModel.ViewState.Success -> {
                ProductReviewsCardContent(
                    viewState = viewState,
                    onFilterSelected = onFilterSelected,
                    onReviewClicked = onReviewClicked
                )
            }

            is DashboardReviewsViewModel.ViewState.Error -> {
                WidgetError(
                    onContactSupportClicked = onContactSupportClicked,
                    onRetryClicked = onRetryClicked
                )
            }
        }
    }
}

@Composable
private fun ProductReviewsCardContent(
    viewState: DashboardReviewsViewModel.ViewState.Success,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    onReviewClicked: (ProductReview) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Header(viewState.selectedFilter, onFilterSelected)

        if (viewState.reviews.isEmpty()) {
            EmptyView(selectedFilter = viewState.selectedFilter)
        } else {
            viewState.reviews.forEach { productReview ->
                ReviewListItem(
                    review = productReview,
                    onClicked = { onReviewClicked(productReview) }
                )
            }
        }
    }
}

@Composable
private fun ReviewListItem(
    review: ProductReview,
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_comment),
            contentDescription = null,
            tint = if (review.read == false) {
                MaterialTheme.colors.primary
            } else {
                MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            }
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface
            )

            val reviewText = buildAnnotatedString {
                if (review.status == ProductReviewStatus.HOLD.toString()) {
                    withStyle(SpanStyle(color = colorResource(id = R.color.woo_orange_50))) {
                        append(stringResource(id = R.string.pending_review_label))
                    }

                    append(" • ")
                }

                append(StringUtils.getRawTextFromHtml(review.review))
            }

            Text(
                text = reviewText,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            )

            if (review.rating > 0) {
                AndroidView(
                    factory = { context ->
                        RatingBar(context, null, androidx.appcompat.R.attr.ratingBarStyleSmall)
                    },
                    update = { ratingBar ->
                        ratingBar.rating = 100F
                        ratingBar.numStars = review.rating
                    }
                )
            }

            Spacer(modifier = Modifier)
            Divider()
        }
    }
}

@Composable
private fun ReviewsLoading(
    selectedFilter: ProductReviewStatus,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Header(selectedFilter, onFilterSelected)
        repeat(3) {
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
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun Header(
    selectedFilter: ProductReviewStatus,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        DashboardFilterableCardHeader(
            title = stringResource(id = R.string.dashboard_reviews_card_header_title),
            currentFilter = selectedFilter,
            filterList = DashboardReviewsViewModel.supportedFilters,
            onFilterSelected = onFilterSelected,
            mapper = { ProductReviewStatus.getLocalizedLabel(LocalContext.current, it) }
        )

        Divider()
    }
}

@Composable
private fun EmptyView(
    selectedFilter: ProductReviewStatus,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_reviews),
            contentDescription = null,
            modifier = Modifier.sizeIn(maxWidth = 160.dp, maxHeight = 160.dp)
        )

        Text(
            text = stringResource(
                id = if (selectedFilter == ProductReviewStatus.ALL) {
                    R.string.empty_review_list_title
                } else {
                    R.string.dashboard_reviews_card_empty_title_filtered
                }
            ),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(
                id = if (selectedFilter == ProductReviewStatus.ALL) {
                    R.string.empty_review_list_message
                } else {
                    R.string.dashboard_reviews_card_empty_message_filtered
                }
            ),
            style = MaterialTheme.typography.body1,
            textAlign = TextAlign.Center
        )
    }
}
