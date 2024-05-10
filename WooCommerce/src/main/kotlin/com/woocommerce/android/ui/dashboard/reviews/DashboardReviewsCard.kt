package com.woocommerce.android.ui.dashboard.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardFilterableCardHeader
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry
import com.woocommerce.android.ui.reviews.ProductReviewStatus

@Composable
fun DashboardReviewsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardReviewsViewModel = viewModelWithFactory { factory: DashboardReviewsViewModel.Factory ->
        factory.create(parentViewModel = parentViewModel)
    }
) {
    viewModel.viewState.observeAsState().value?.let { viewState ->
        DashboardReviewsCard(
            viewState = viewState,
            onHideClicked = { parentViewModel.onHideWidgetClicked(DashboardWidget.Type.REVIEWS) },
            onFilterSelected = viewModel::onFilterSelected,
            modifier = modifier
        )
    }
}

@Composable
private fun DashboardReviewsCard(
    viewState: DashboardReviewsViewModel.ViewState,
    onHideClicked: () -> Unit,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    modifier: Modifier
) {
    WidgetCard(
        titleResource = DashboardWidget.Type.REVIEWS.titleResource,
        menu = DashboardWidgetMenu(
            listOf(
                DashboardWidget.Type.REVIEWS.defaultHideMenuEntry(onHideClicked)
            )
        ),
        isError = false,
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
                    onFilterSelected = onFilterSelected
                )
            }

            is DashboardReviewsViewModel.ViewState.Error -> {
                WidgetError(
                    onContactSupportClicked = { /*TODO*/ },
                    onRetryClicked = { /*TODO*/ }
                )
            }
        }
    }
}

@Composable
private fun ProductReviewsCardContent(
    viewState: DashboardReviewsViewModel.ViewState.Success,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Header(viewState.selectedFilter, onFilterSelected)

        viewState.reviews.forEach { review ->
            Text(text = review.review.fastStripHtml())
        }
    }
}

@Composable
private fun ReviewsLoading(
    selectedFilter: ProductReviewStatus,
    onFilterSelected: (ProductReviewStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    Column {
        Header(selectedFilter, onFilterSelected)
        CircularProgressIndicator(modifier = modifier)
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
            filterList = supportedFilters,
            onFilterSelected = onFilterSelected,
            mapper = { ProductReviewStatus.getLocalizedLabel(LocalContext.current, it) }
        )

        Divider()
    }
}

private val supportedFilters = listOf(
    ProductReviewStatus.ALL,
    ProductReviewStatus.APPROVED,
    ProductReviewStatus.HOLD,
    ProductReviewStatus.SPAM
)
