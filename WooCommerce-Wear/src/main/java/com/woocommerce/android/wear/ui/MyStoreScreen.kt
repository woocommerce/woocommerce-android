package com.woocommerce.android.wear.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.horologist.compose.pager.PagerScreen
import com.woocommerce.android.wear.ui.orders.list.OrdersListScreen
import com.woocommerce.android.wear.ui.orders.list.OrdersListViewModel
import com.woocommerce.android.wear.ui.stats.StoreStatsScreen
import com.woocommerce.android.wear.ui.stats.StoreStatsViewModel

@Composable
fun MyStoreScreen(
    storeStatsViewModel: StoreStatsViewModel,
    ordersListViewModel: OrdersListViewModel
) {
    val pageState = rememberPagerState(pageCount = { 2 })
    PagerScreen(
        state = pageState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            STATS_PAGE -> StoreStatsScreen(viewModel = storeStatsViewModel)
            ORDERS_PAGE -> OrdersListScreen(viewModel = ordersListViewModel)
        }
    }
}

private const val STATS_PAGE = 0
private const val ORDERS_PAGE = 1
