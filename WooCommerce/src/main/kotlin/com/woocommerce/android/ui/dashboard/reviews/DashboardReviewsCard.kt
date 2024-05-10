package com.woocommerce.android.ui.dashboard.reviews

import androidx.compose.foundation.layout.Column
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardViewModel
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetMenu
import com.woocommerce.android.ui.dashboard.WidgetCard
import com.woocommerce.android.ui.dashboard.WidgetError
import com.woocommerce.android.ui.dashboard.defaultHideMenuEntry

@Composable
fun DashboardReviewsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardReviewsViewModel = viewModelWithFactory { factory: DashboardReviewsViewModel.Factory ->
        factory.create(parentViewModel = parentViewModel)
    }
) {
    WidgetCard(
        titleResource = DashboardWidget.Type.REVIEWS.titleResource,
        menu = DashboardWidgetMenu(
            listOf(
                DashboardWidget.Type.REVIEWS.defaultHideMenuEntry {
                    parentViewModel.onHideWidgetClicked(
                        DashboardWidget.Type.REVIEWS
                    )
                }
            )
        ),
        isError = false,
        modifier = modifier
    ) {
        viewModel.viewState.observeAsState().value?.let { viewState ->
            when (viewState) {
                is DashboardReviewsViewModel.ViewState.Loading -> {
                    CircularProgressIndicator()
                }

                is DashboardReviewsViewModel.ViewState.Success -> {
                    Column {
                        viewState.reviews.forEach { review ->
                            Text(text = review.review)
                            Divider()
                        }
                    }
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
}
