package com.woocommerce.android.ui.dashboard.reviews

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.compose.viewModelWithFactory
import com.woocommerce.android.ui.dashboard.DashboardViewModel

@Composable
fun DashboardReviewsCard(
    parentViewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    viewModel: DashboardReviewsViewModel = viewModelWithFactory { factory: DashboardReviewsViewModel.Factory ->
        factory.create(parentViewModel = parentViewModel)
    }
) {

}
