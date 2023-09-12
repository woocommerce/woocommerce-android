package com.woocommerce.android.ui.orders.list

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.woocommerce.android.R
import com.woocommerce.android.extensions.getBillingName
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

private const val STARTING_PAGE_OFFSET = 0

class OrderListItemPagingSource @Inject constructor(
    private val orderStore: WCOrderStore,
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider
) : PagingSource<Int, OrderListItemUIType>() {
    override fun getRefreshKey(state: PagingState<Int, OrderListItemUIType>): Int? {
        // We need to get the previous key (or next key if previous is null) of the page
        // that was closest to the most recently accessed index.
        // Anchor position is the most recently accessed index
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, OrderListItemUIType> {
        val offset = params.key ?: STARTING_PAGE_OFFSET
        val response = orderStore.fetchOrdersSync(selectedSite.get(), offset, params.loadSize)
        return if (response.isError) {
            LoadResult.Error(Throwable("Response.error"))
        } else {
            val nextKey = if (response.model.isNullOrEmpty()) {
                null
            } else {
                offset + params.loadSize
            }
            val data: List<OrderListItemUIType> = response.model?.map { order ->
                @Suppress("DEPRECATION_ERROR")
                (OrderListItemUIType.OrderListItemUI(
                    orderId = order.orderId,
                    orderNumber = order.number,
                    orderName = order.getBillingName(
                        resourceProvider.getString(R.string.orderdetail_customer_name_default)
                    ),
                    orderTotal = order.total,
                    status = order.status,
                    dateCreated = order.dateCreated,
                    currencyCode = order.currency,
                    isLastItemInSection = false
                ))
            }.orEmpty()
                .let { getHeaders(it) }

            LoadResult.Page(
                data = data,
                prevKey = if (offset == STARTING_PAGE_OFFSET) null else offset - params.loadSize,
                nextKey = nextKey
            )
        }
    }

    private fun getHeaders(data: List<OrderListItemUIType.OrderListItemUI>): List<OrderListItemUIType> {
        val listToday = mutableListOf<OrderListItemUIType>()
        val listYesterday = mutableListOf<OrderListItemUIType>()
        val listTwoDays = mutableListOf<OrderListItemUIType>()
        val listWeek = mutableListOf<OrderListItemUIType>()
        val listMonth = mutableListOf<OrderListItemUIType>()

        data.forEach {
            // Default to today if the date cannot be parsed. This date is in UTC.
            val date: Date = DateTimeUtils.dateUTCFromIso8601(it.dateCreated) ?: Date()
            when (TimeGroup.getTimeGroupForDate(date)) {
                TimeGroup.GROUP_TODAY -> listToday.add(it)
                TimeGroup.GROUP_YESTERDAY -> listYesterday.add(it)
                TimeGroup.GROUP_OLDER_TWO_DAYS -> listTwoDays.add(it)
                TimeGroup.GROUP_OLDER_WEEK -> listWeek.add(it)
                TimeGroup.GROUP_OLDER_MONTH -> listMonth.add(it)
                else -> {}
            }
        }
        val allItems = mutableListOf<OrderListItemUIType>()

        if (listToday.isNotEmpty()) {
            allItems += listOf(OrderListItemUIType.SectionHeader(TimeGroup.GROUP_TODAY)) + listToday
        }
        if (listYesterday.isNotEmpty()) {
            allItems += listOf(OrderListItemUIType.SectionHeader(TimeGroup.GROUP_YESTERDAY)) + listYesterday
        }
        if (listTwoDays.isNotEmpty()) {
            allItems += listOf(OrderListItemUIType.SectionHeader(TimeGroup.GROUP_OLDER_TWO_DAYS)) + listTwoDays
        }
        if (listWeek.isNotEmpty()) {
            allItems += listOf(OrderListItemUIType.SectionHeader(TimeGroup.GROUP_OLDER_WEEK)) + listWeek
        }
        if (listMonth.isNotEmpty()) {
            allItems += listOf(OrderListItemUIType.SectionHeader(TimeGroup.GROUP_OLDER_MONTH)) + listMonth
        }
        return allItems
    }
}
