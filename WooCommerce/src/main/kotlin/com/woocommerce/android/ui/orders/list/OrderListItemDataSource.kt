package com.woocommerce.android.ui.orders.list

import androidx.lifecycle.Lifecycle
import com.woocommerce.android.model.TimeGroup
import com.woocommerce.android.model.TimeGroup.GROUP_OLDER_MONTH
import com.woocommerce.android.model.TimeGroup.GROUP_OLDER_TWO_DAYS
import com.woocommerce.android.model.TimeGroup.GROUP_OLDER_WEEK
import com.woocommerce.android.model.TimeGroup.GROUP_TODAY
import com.woocommerce.android.model.TimeGroup.GROUP_YESTERDAY
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier.OrderIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemIdentifier.SectionHeaderIdentifier
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.LoadingItem
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.OrderListItemUI
import com.woocommerce.android.ui.orders.list.OrderListItemUIType.SectionHeader
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
    val dispatcher: Dispatcher,
    val orderStore: WCOrderStore,
    private val lifecycle: Lifecycle
) : ListItemDataSourceInterface<WCOrderListDescriptor, OrderListItemIdentifier, OrderListItemUIType> {
    private val fetcher = OrderFetcher(lifecycle, dispatcher)

    override fun getItemsAndFetchIfNecessary(
        listDescriptor: WCOrderListDescriptor,
        itemIdentifiers: List<OrderListItemIdentifier>
    ): List<OrderListItemUIType> {
        val remoteItemIds = itemIdentifiers.mapNotNull { (it as? OrderIdentifier)?.remoteId }
        val ordersMap = orderStore.getOrdersForDescriptor(listDescriptor, remoteItemIds)

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
                            orderName = "${order.billingFirstName} ${order.billingLastName}",
                            orderTotal = order.total,
                            status = order.status,
                            dateCreated = order.dateCreated,
                            currencyCode = order.currency
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
                    remoteItemIds.mapNotNull { summariesByRemoteId[it] }
                }

        val listToday = ArrayList<OrderIdentifier>()
        val listYesterday = ArrayList<OrderIdentifier>()
        val listTwoDays = ArrayList<OrderIdentifier>()
        val listWeek = ArrayList<OrderIdentifier>()
        val listMonth = ArrayList<OrderIdentifier>()
        val mapToRemoteOrderIdentifier = { summary: WCOrderSummaryModel ->
            OrderIdentifier(RemoteId(summary.remoteOrderId))
        }
        orderSummaries.forEach {
            // Default to today if the date cannot be parsed
            val date: Date = DateTimeUtils.dateUTCFromIso8601(it.dateCreated) ?: Date()
            when (TimeGroup.getTimeGroupForDate(date)) {
                GROUP_TODAY -> listToday.add(mapToRemoteOrderIdentifier(it))
                GROUP_YESTERDAY -> listYesterday.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_TWO_DAYS -> listTwoDays.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_WEEK -> listWeek.add(mapToRemoteOrderIdentifier(it))
                GROUP_OLDER_MONTH -> listMonth.add(mapToRemoteOrderIdentifier(it))
            }
        }

        val allItems = mutableListOf<OrderListItemIdentifier>()
        if (listToday.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_TODAY)) + listToday
        }
        if (listYesterday.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_YESTERDAY)) + listYesterday
        }
        if (listTwoDays.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_TWO_DAYS)) + listTwoDays
        }
        if (listWeek.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_WEEK)) + listWeek
        }
        if (listMonth.isNotEmpty()) {
            allItems += listOf(SectionHeaderIdentifier(GROUP_OLDER_MONTH)) + listMonth
        }
        return allItems
    }

    override fun fetchList(listDescriptor: WCOrderListDescriptor, offset: Long) {
        val fetchOrderListPayload = FetchOrderListPayload(listDescriptor, offset)
        dispatcher.dispatch(WCOrderActionBuilder.newFetchOrderListAction(fetchOrderListPayload))
    }
}
