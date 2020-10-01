package com.woocommerce.android.ui.orders.list

import com.woocommerce.android.R
import com.woocommerce.android.extensions.getBillingName
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.model.TimeGroup.GROUP_FUTURE
import com.woocommerce.android.model.TimeGroup.GROUP_OLDER_MONTH
import com.woocommerce.android.model.TimeGroup.GROUP_OLDER_TWO_DAYS
import com.woocommerce.android.model.TimeGroup.GROUP_OLDER_WEEK
import com.woocommerce.android.model.TimeGroup.GROUP_TODAY
import com.woocommerce.android.model.TimeGroup.GROUP_YESTERDAY
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier.OrderIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier.SectionHeaderIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.LoadingItem
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.SectionHeader
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.list.datasource.ListItemDataSourceInterface
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

/**
 * Works with a [androidx.paging.PagedList] by providing the logic needed to fetch the data used to populate
 * the order list view.
 *
 * @see [ListItemDataSourceInterface] and [org.wordpress.android.fluxc.model.list.datasource.InternalPagedListDataSource]
 * in FluxC to get a better understanding of how this works with the underlying internal list management code.
 */
class OrderListItemDataSource(
    private val dispatcher: Dispatcher,
    private val orderStore: WCOrderStore,
    private val networkStatus: NetworkStatus,
    private val fetcher: OrderFetcher,
    private val resourceProvider: ResourceProvider
) : ListItemDataSourceInterface<WCOrderListDescriptor, OrderListItemIdentifier, OrderListItemUIType> {
    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCOrderListDescriptor,
        itemIdentifiers: List<OrderListItemIdentifier>
    ): List<OrderListItemUIType> {
        val remoteItemIds = itemIdentifiers.mapNotNull { (it as? OrderIdentifier)?.remoteId }
        val ordersMap = orderStore.getOrdersForDescriptor(listDescriptor, remoteItemIds)
        val isLastItemByRemoteIdMap = itemIdentifiers
                .mapNotNull { (it as? OrderIdentifier) }
                .associate { it.remoteId to it.isLastItemInSection }

        // Fetch missing items
        fetcher.fetchOrders(
                site = listDescriptor.site,
                remoteItemIds = remoteItemIds.filter { !ordersMap.containsKey(it) }
        )

        val mapSummary = { remoteOrderId: RemoteId ->
            ordersMap[remoteOrderId].let { order ->
                if (order == null) {
                    LoadingItem(remoteOrderId)
                } else {
                    OrderListItemUI(
                            remoteOrderId = RemoteId(order.remoteOrderId),
                            orderNumber = order.number,
                            orderName = order.getBillingName(
                                resourceProvider.getString(R.string.orderdetail_customer_name_default)
                            ),
                            orderTotal = order.total,
                            status = order.status,
                            dateCreated = order.dateCreated,
                            currencyCode = order.currency,
                            isLastItemInSection = isLastItemByRemoteIdMap[RemoteId(order.remoteOrderId)] ?: false
                    )
                }
            }
        }

        return itemIdentifiers.map { identifier ->
            when (identifier) {
                is OrderIdentifier -> mapSummary(identifier.remoteId)
                is SectionHeaderIdentifier -> SectionHeader(title = identifier.title)
            }
        }
    }

    override fun getItemIdentifiers(
        listDescriptor: WCOrderListDescriptor,
        remoteItemIds: List<RemoteId>,
        isListFullyFetched: Boolean
    ): List<OrderListItemIdentifier> {
        val orderSummaries = orderStore.getOrderSummariesByRemoteOrderIds(listDescriptor.site, remoteItemIds)
                .let { summariesByRemoteId ->
                    val summaries = remoteItemIds.mapNotNull { summariesByRemoteId[it] }

                    if (!networkStatus.isConnected()) {
                        // The network is not connected so remove any order summaries from the list where
                        // a matching order has not yet been downloaded. This prevents the user from seeing
                        // a "loading" view for that item indefinitely.
                        val cachedOrders = orderStore.getOrdersForDescriptor(listDescriptor, remoteItemIds)
                        summaries.filter { cachedOrders.containsKey(RemoteId(it.remoteOrderId)) }
                    } else summaries
                }

        val listFuture = mutableListOf<OrderIdentifier>()
        val listToday = mutableListOf<OrderIdentifier>()
        val listYesterday = mutableListOf<OrderIdentifier>()
        val listTwoDays = mutableListOf<OrderIdentifier>()
        val listWeek = mutableListOf<OrderIdentifier>()
        val listMonth = mutableListOf<OrderIdentifier>()
        val mapToRemoteOrderIdentifier = { summary: WCOrderSummaryModel ->
            OrderIdentifier(RemoteId(summary.remoteOrderId))
        }
        orderSummaries.forEach {
            // Default to today if the date cannot be parsed. This date is in UTC.
            val date: Date = DateTimeUtils.dateUTCFromIso8601(it.dateCreated) ?: Date()

            // Check if future-dated orders should be excluded from the results list.
            if (listDescriptor.excludeFutureOrders) {
                val currentDate = Date()
                if (DateUtils().isAfterDate(currentDate, date)) {
                    // This order is dated for the future so skip adding it to the list
                    return@forEach
                }
            }

            when (TimeGroup.getTimeGroupForDate(date)) {
                GROUP_FUTURE -> listFuture.add(mapToRemoteOrderIdentifier(it))
                GROUP_TODAY -> listToday.add(mapToRemoteOrderIdentifier(it))
                GROUP_YESTERDAY -> listYesterday.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_TWO_DAYS -> listTwoDays.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_WEEK -> listWeek.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_MONTH -> listMonth.add(mapToRemoteOrderIdentifier(it))
            }
        }

        val allItems = mutableListOf<OrderListItemIdentifier>()
        if (listFuture.isNotEmpty()) {
            listFuture.lastOrNull()?.isLastItemInSection = true
            allItems += listOf(SectionHeaderIdentifier(GROUP_FUTURE)) + listFuture
        }

        if (listToday.isNotEmpty()) {
            listToday.lastOrNull()?.isLastItemInSection = true
            allItems += listOf(SectionHeaderIdentifier(GROUP_TODAY)) + listToday
        }
        if (listYesterday.isNotEmpty()) {
            listYesterday.lastOrNull()?.isLastItemInSection = true
            allItems += listOf(SectionHeaderIdentifier(GROUP_YESTERDAY)) + listYesterday
        }
        if (listTwoDays.isNotEmpty()) {
            listTwoDays.lastOrNull()?.isLastItemInSection = true
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_TWO_DAYS)) + listTwoDays
        }
        if (listWeek.isNotEmpty()) {
            listWeek.lastOrNull()?.isLastItemInSection = true
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_WEEK)) + listWeek
        }
        if (listMonth.isNotEmpty()) {
            listMonth.lastOrNull()?.isLastItemInSection = true
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_MONTH)) + listMonth
        }
        return allItems
    }

    override fun fetchList(listDescriptor: WCOrderListDescriptor, offset: Long) {
        val fetchOrderListPayload = FetchOrderListPayload(listDescriptor, offset)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderListAction(fetchOrderListPayload))
    }
}
