package com.woocommerce.android.ui.dashboard.reviews

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.compose.designsystem.WooTheme
import com.woocommerce.android.ui.compose.designsystem.component.WooDivider
import com.woocommerce.android.ui.compose.designsystem.icons.Star
import com.woocommerce.android.ui.compose.designsystem.icons.WooIcons
import com.woocommerce.android.ui.compose.rememberNavController
import com.woocommerce.android.ui.dashboard.DashboardFilterableCardHeader
import com.woocommerce.android.ui.dashboard.DashboardFragmentDirections
import com.woocommerce.android.ui.dashboard.DashboardSkeleton
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
    viewModel: DashboardReviewsViewModel = hiltViewModel { factory: DashboardReviewsViewModel.Factory ->
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
                DashboardReviewListItem(
                    review = productReview,
                    onClicked = { onReviewClicked(productReview) }
                )
            }
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
                DashboardSkeleton(width = 24.dp, height = 24.dp)

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    DashboardSkeleton(width = 260.dp, height = 16.dp)
                    DashboardSkeleton(width = 120.dp, height = 16.dp)
                    DashboardSkeleton(width = 60.dp, height = 16.dp)
                    Spacer(modifier = Modifier)
                    WooDivider()
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

        WooDivider()
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
            style = WooTheme.text.titleLarge.strong,
            color = WooTheme.colors.surface.onDefault,
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
            style = WooTheme.text.bodyLarge.regular,
            color = WooTheme.colors.surface.onDefault,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DashboardReviewListItem(
    review: ProductReview,
    onClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClicked),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(WooTheme.spacing.space5),
            modifier = Modifier.padding(
                horizontal = WooTheme.padding.padding5,
                vertical = WooTheme.padding.padding3,
            ),
        ) {
            val isPending = review.status == ProductReviewStatus.HOLD.toString()
            Icon(
                painter = painterResource(id = R.drawable.ic_comment),
                contentDescription = null,
                tint = if (isPending) WooTheme.colors.secondary else WooTheme.colors.surface.onDefault,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(
                    WooTheme.spacing.space2 + WooTheme.spacing.space1
                )
            ) {
                Text(
                    text = if (review.product == null) {
                        stringResource(R.string.product_review_list_item_title, review.reviewerName)
                    } else {
                        stringResource(
                            R.string.review_list_item_title,
                            review.reviewerName,
                            review.product?.name?.fastStripHtml().orEmpty(),
                        )
                    },
                    style = WooTheme.text.titleMedium.emphasized,
                    color = WooTheme.colors.surface.onDefault,
                )
                val reviewText = buildAnnotatedString {
                    if (isPending) {
                        withStyle(SpanStyle(color = WooTheme.colors.container.onSecondaryContainer)) {
                            append(stringResource(id = R.string.pending_review_label))
                        }
                        append(" \u2022 ")
                    }
                    append(StringUtils.getRawTextFromHtml(review.review))
                }
                Text(
                    text = reviewText,
                    style = WooTheme.text.bodyMedium.regular,
                    color = WooTheme.colors.surface.onDefault,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (review.rating > 0) {
                    DashboardReviewRating(rating = review.rating)
                }
            }
        }
        WooDivider(modifier = Modifier.padding(start = 56.dp))
    }
}

@Composable
private fun DashboardReviewRating(rating: Int) {
    val ratingDescription = pluralStringResource(
        R.plurals.dashboard_review_rating_content_description,
        rating,
        rating,
    )
    Row(
        modifier = Modifier.semantics {
            contentDescription = ratingDescription
        },
    ) {
        repeat(rating) {
            Icon(
                imageVector = WooIcons.Solid.Star,
                contentDescription = null,
                modifier = Modifier.size(WooTheme.iconSize.size16),
                tint = WooTheme.colors.container.onSecondaryContainer,
            )
        }
    }
}
