package com.woocommerce.android.ui.analytics

import com.woocommerce.android.model.DeltaPercentage
import com.woocommerce.android.model.OrdersStat
import com.woocommerce.android.model.ProductsStat
import com.woocommerce.android.model.RevenueStat
import com.woocommerce.android.model.SessionStat
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.OrdersResult.OrdersData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.ProductsResult.ProductsData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.RevenueResult.RevenueData
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.VisitorsResult
import com.woocommerce.android.ui.analytics.hub.sync.AnalyticsRepository.VisitorsResult.VisitorsData
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.CUSTOM
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType.TODAY
import org.mockito.kotlin.mock
import java.util.Calendar
import java.util.Locale

val testRangeSelection = TODAY.generateSelectionData(
    calendar = Calendar.getInstance(),
    locale = Locale.getDefault(),
    dateUtils = mock()
)

val testCustomRangeSelection = CUSTOM.generateSelectionData(
    calendar = Calendar.getInstance(),
    locale = Locale.getDefault(),
    dateUtils = mock()
)

val testRevenueStat = RevenueStat(
    totalValue = 1234.5,
    totalDelta = DeltaPercentage.NotExist,
    netValue = 1000.0,
    netDelta = DeltaPercentage.NotExist,
    currencyCode = "",
    totalRevenueByInterval = emptyList(),
    netRevenueByInterval = emptyList()
)

val testOrdersStat = OrdersStat(
    ordersCount = 23,
    ordersCountDelta = DeltaPercentage.NotExist,
    avgOrderValue = 500.0,
    avgOrderDelta = DeltaPercentage.NotExist,
    currencyCode = "",
    ordersCountByInterval = emptyList(),
    avgOrderValueByInterval = emptyList()
)

val testProductsStat = ProductsStat(
    itemsSold = 123,
    itemsSoldDelta = DeltaPercentage.NotExist,
    products = emptyList()
)

const val testVisitorsCount = 150

val testSessionStat = SessionStat(
    ordersCount = testOrdersStat.ordersCount,
    visitorsCount = testVisitorsCount
)

val testRevenueResult = RevenueData(testRevenueStat) as RevenueResult
val testOrdersResult = OrdersData(testOrdersStat) as OrdersResult
val testProductsResult = ProductsData(testProductsStat) as ProductsResult
val testVisitorsResult = VisitorsData(testVisitorsCount) as VisitorsResult
