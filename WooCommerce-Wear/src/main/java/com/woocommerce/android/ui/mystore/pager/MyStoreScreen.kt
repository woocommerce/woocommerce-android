package com.woocommerce.android.ui.mystore.pager

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.horologist.compose.pager.PagerScreen
import com.woocommerce.android.ui.mystore.StoreStatsScreen
import com.woocommerce.android.ui.mystore.StoreStatsViewModel
import com.woocommerce.android.ui.orders.OrdersListScreen
import com.woocommerce.android.ui.orders.OrdersListViewModel

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
