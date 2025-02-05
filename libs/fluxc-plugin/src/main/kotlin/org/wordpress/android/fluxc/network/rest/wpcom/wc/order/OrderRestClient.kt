@file:Suppress("DEPRECATION_ERROR")

package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaData.OrderAttributionInfoKeys
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.model.order.UpdateOrderRequest
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Billing
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Shipping
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDtoMapper.Companion.toDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.AddOrderShipmentTrackingResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.BulkUpdateOrderStatusResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.DeleteOrderShipmentTrackingResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentProvidersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderShipmentTrackingsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderStatusOptionsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersByIdsResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.TIMEOUT_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.SearchOrdersResponsePayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.fluxc.utils.DateUtils
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import org.wordpress.android.fluxc.utils.toWooPayload
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import java.util.Calendar
import javax.inject.Inject
import kotlin.collections.MutableMap.MutableEntry

@Suppress("LargeClass", "TooManyFunctions")
class OrderRestClient @Inject constructor(
    private val dispatcher: Dispatcher,
    private val orderDtoMapper: OrderDtoMapper,
    private val wooNetwork: WooNetwork,
    private val coroutineEngine: CoroutineEngine
) {
    /**
     * Makes a GET call to `/wp-json/wc/v3/orders` retrieving a list of orders for the given
     * WooCommerce [SiteModel].
     *
     * The number of orders fetched is defined in [WCOrderStore.NUM_ORDERS_PER_FETCH], and retrieving older
     * orders is done by passing an [offset].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS] action with the resulting list of orders.
     *
     * @param [filterByStatus] Nullable. If not null, fetch only orders with a matching order status.
     */
    fun fetchOrders(site: SiteModel, offset: Int, filterByStatus: String? = null) {
        coroutineEngine.launch(T.API, this, "fetchOrders") {
            val url = WOOCOMMERCE.orders.pathV3
            val params = mutableMapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "_fields" to ORDER_FIELDS
            ).putIfNotEmpty(
                "status" to filterByStatus
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                params = params,
                clazz = Array<OrderDto>::class.java
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val orderModels = response.data?.map { orderDto ->
                        orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                    }.orEmpty()

                    val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                    val payload = FetchOrdersResponsePayload(
                        site, orderModels, filterByStatus, offset > 0, canLoadMore
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = wpAPINetworkErrorToOrderError(response.error)
                    val payload = FetchOrdersResponsePayload(orderError, site)
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersAction(payload))
                }
            }
        }
    }

    suspend fun fetchOrdersSync(
        site: SiteModel,
        offset: Int,
        filterByStatus: String? = null
    ): FetchOrdersResponsePayload {
        val url = WOOCOMMERCE.orders.pathV3
        val params = mutableMapOf(
            "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
            "offset" to offset.toString(),
            "_fields" to ORDER_FIELDS
        ).putIfNotEmpty(
            "status" to filterByStatus
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<OrderDto>::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val orderModels = response.data?.map { orderDto ->
                    orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                }.orEmpty()

                val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                FetchOrdersResponsePayload(
                    site, orderModels, filterByStatus, offset > 0, canLoadMore
                )
            }

            is WPAPIResponse.Error -> FetchOrdersResponsePayload(
                error = wpAPINetworkErrorToOrderError(response.error),
                site = site
            )
        }
    }

    /**
     * Fetches orders from the API.
     *
     * Makes a GET call to `/wp-json/wc/v3/orders` retrieving a list of orders for the given site and parameters.
     *
     * @param site The WooCommerce [SiteModel] the orders belong to
     * @param count The number of orders to fetch
     * @param page The page number to fetch
     * @param orderBy The field to order the results by
     * @param sortOrder The order to sort the results by
     * @param statusFilter The status to filter the results by
     */
    @Suppress("LongParameterList")
    suspend fun fetchOrders(
        site: SiteModel,
        count: Int,
        page: Int,
        orderBy: OrderBy,
        sortOrder: SortOrder,
        statusFilter: String?
    ): FetchOrdersResponsePayload {
        val url = WOOCOMMERCE.orders.pathV3
        val params = mutableMapOf(
            "per_page" to count.toString(),
            "page" to page.toString(),
            "orderby" to orderBy.value,
            "order" to sortOrder.value,
            "_fields" to ORDER_FIELDS
        ).putIfNotEmpty(
            "status" to statusFilter
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            params = params,
            clazz = Array<OrderDto>::class.java
        )

        when (response) {
            is WPAPIResponse.Success -> {
                val orderModels = response.data?.map { orderDto ->
                    orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                }.orEmpty()

                val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                return FetchOrdersResponsePayload(
                    site = site,
                    ordersWithMeta = orderModels,
                    loadedMore = page > 1,
                    canLoadMore = canLoadMore
                )
            }
            is WPAPIResponse.Error -> {
                val orderError = wpAPINetworkErrorToOrderError(response.error)
                return FetchOrdersResponsePayload(orderError, site)
            }
        }
    }

    /**
     * Fetches orders from the API, but only requests `id` and `date_created_gmt` fields be returned. This is
     * used to determine what orders should be fetched (either existing orders that have since changed or new
     * orders not yet downloaded).
     *
     * Makes a GET call to `/wp-json/wc/v3/orders` retrieving a list of orders for the given
     * WooCommerce [SiteModel].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDER_LIST] action with the resulting list of order summaries.
     *
     * @param listDescriptor The [WCOrderListDescriptor] that describes the type of list being fetched and
     * the optional parameters in effect.
     * @param offset Used to retrieve older orders
     */
    fun fetchOrderListSummaries(
        listDescriptor: WCOrderListDescriptor,
        offset: Long,
        requestStartTime: Calendar
    ) {
        coroutineEngine.launch(T.API, this, "fetchOrderListSummaries") {
            val url = WOOCOMMERCE.orders.pathV3
            val networkPageSize = listDescriptor.config.networkPageSize
            val params = mutableMapOf(
                "per_page" to networkPageSize.toString(),
                "offset" to offset.toString(),
                "_fields" to "id,date_created_gmt,date_modified_gmt"
            ).putIfNotEmpty(
                "search" to listDescriptor.searchQuery,
                "before" to listDescriptor.beforeFilter,
                "after" to listDescriptor.afterFilter,
                "customer" to listDescriptor.customerId?.toString(),
                "product" to listDescriptor.productId?.toString(),
                "exclude" to listDescriptor.excludedIds?.joinToString(),
                "status" to listDescriptor.statusFilter.takeUnless { it.isNullOrBlank() }
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = listDescriptor.site,
                path = url,
                clazz = Array<OrderSummaryApiResponse>::class.java,
                params = params
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val orderSummaries = response.data?.map {
                        orderResponseToOrderSummaryModel(it).apply {
                            localSiteId = listDescriptor.site.id
                        }
                    }.orEmpty()

                    val canLoadMore = orderSummaries.size == networkPageSize

                    val payload = FetchOrderListResponsePayload(
                        listDescriptor = listDescriptor,
                        orderSummaries = orderSummaries,
                        loadedMore = offset > 0,
                        canLoadMore = canLoadMore,
                        requestStartTime = requestStartTime
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderListAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = wpAPINetworkErrorToOrderError(response.error)
                    val payload = FetchOrderListResponsePayload(
                        error = orderError,
                        listDescriptor = listDescriptor,
                        requestStartTime = requestStartTime
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrderListAction(payload))
                }
            }
        }
    }

    suspend fun fetchOrdersListFirstPage(
        listDescriptor: WCOrderListDescriptor
    ): WooPayload<List<Pair<OrderEntity, List<WCMetaData>>>> {
        val url = WOOCOMMERCE.orders.pathV3
        val networkPageSize = listDescriptor.config.networkPageSize
        val params = mutableMapOf(
            "per_page" to networkPageSize.toString(),
            "offset" to "0",
            "_fields" to ORDER_FIELDS
        ).putIfNotEmpty(
            "search" to listDescriptor.searchQuery,
            "before" to listDescriptor.beforeFilter,
            "after" to listDescriptor.afterFilter,
            "customer" to listDescriptor.customerId?.toString(),
            "product" to listDescriptor.productId?.toString(),
            "exclude" to listDescriptor.excludedIds?.joinToString(),
            "status" to listDescriptor.statusFilter.takeUnless { it.isNullOrBlank() }
        )

        return wooNetwork.executeGetGsonRequest(
            site = listDescriptor.site,
            path = url,
            clazz = Array<OrderDto>::class.java,
            params = params
        ).toWooPayload {
            it.map { orderDto ->
                orderDtoMapper.toDatabaseEntity(orderDto, listDescriptor.site.localId())
            }
        }
    }

    /**
     * Requests orders from the API that match the provided list of [orderIds] by making a GET call to
     * `/wp-json/wc/v3/orders`
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS_BY_IDS] action with the resulting list of orders.
     *
     * @param site The WooCommerce [SiteModel] the orders belong to
     * @param orderIds A list of remote order identifiers to fetch from the API
     */
    fun fetchOrdersByIds(site: SiteModel, orderIds: List<Long>) {
        coroutineEngine.launch(T.API, this, "fetchOrdersByIds") {
            val url = WOOCOMMERCE.orders.pathV3
            val params = mapOf(
                "per_page" to orderIds.size.toString(),
                "include" to orderIds.map { it }.joinToString(),
                "_fields" to ORDER_FIELDS
            )

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = Array<OrderDto>::class.java,
                params = params
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val orderModels = response.data?.map { orderDto ->
                        orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                    }.orEmpty()

                    val payload = FetchOrdersByIdsResponsePayload(
                        site = site,
                        orderIds = orderIds,
                        fetchedOrders = orderModels
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersByIdsAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = wpAPINetworkErrorToOrderError(response.error)
                    val payload = FetchOrdersByIdsResponsePayload(
                        error = orderError, site = site, orderIds = orderIds
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersByIdsAction(payload))
                }
            }
        }
    }

    /**
     * Makes a GET call to `/wp-json/wc/v3/reports/orders/totals` retrieving a list of available
     * order status options for the given WooCommerce [SiteModel].
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS] action with the resulting list of order status labels.
     */
    fun fetchOrderStatusOptions(site: SiteModel) {
        coroutineEngine.launch(T.API, this, "fetchOrderStatusOptions") {
            val url = WOOCOMMERCE.reports.orders.totals.pathV3
            val params = emptyMap<String, String>()

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = Array<OrderStatusApiResponse>::class.java,
                params = params
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val orderStatusOptions = response.data?.map {
                        orderStatusResponseToOrderStatusModel(it, site)
                    }.orEmpty()

                    val payload = FetchOrderStatusOptionsResponsePayload(site, orderStatusOptions)
                    dispatcher.dispatch(
                        WCOrderActionBuilder.newFetchedOrderStatusOptionsAction(payload)
                    )
                }
                is WPAPIResponse.Error -> {
                    val orderError = wpAPINetworkErrorToOrderError(response.error)
                    val payload = FetchOrderStatusOptionsResponsePayload(orderError, site)
                    dispatcher.dispatch(
                        WCOrderActionBuilder.newFetchedOrderStatusOptionsAction(payload)
                    )
                }
            }
        }
    }

    /**
     * Makes a GET call to `/wp-json/wc/v3/orders` retrieving a list of orders for the given
     * WooCommerce [SiteModel] matching [searchQuery]
     *
     * The number of orders fetched is defined in [WCOrderStore.NUM_ORDERS_PER_FETCH]
     *
     * Dispatches a [WCOrderAction.SEARCHED_ORDERS] action with the resulting list of orders.
     *
     * @param [searchQuery] the keyword or phrase to match orders with
     */
    fun searchOrders(site: SiteModel, searchQuery: String, offset: Int) {
        coroutineEngine.launch(T.API, this, "searchOrders") {
            val url = WOOCOMMERCE.orders.pathV3
            val params = mutableMapOf(
                "per_page" to WCOrderStore.NUM_ORDERS_PER_FETCH.toString(),
                "offset" to offset.toString(),
                "_fields" to ORDER_FIELDS
            ).putIfNotEmpty("search" to searchQuery)

            val response = wooNetwork.executeGetGsonRequest(
                site = site,
                path = url,
                clazz = Array<OrderDto>::class.java,
                params = params
            )

            when (response) {
                is WPAPIResponse.Success -> {
                    val orderModels = response.data?.map { orderDto ->
                        orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                    }.orEmpty()

                    val canLoadMore = orderModels.size == WCOrderStore.NUM_ORDERS_PER_FETCH
                    val nextOffset = offset + orderModels.size

                    val payload = SearchOrdersResponsePayload(
                        site,
                        searchQuery,
                        canLoadMore,
                        nextOffset,
                        orderModels.map { it.first }
                    )
                    dispatcher.dispatch(WCOrderActionBuilder.newSearchedOrdersAction(payload))
                }
                is WPAPIResponse.Error -> {
                    val orderError = wpAPINetworkErrorToOrderError(response.error)
                    val payload = SearchOrdersResponsePayload(orderError, site, searchQuery)
                    dispatcher.dispatch(WCOrderActionBuilder.newSearchedOrdersAction(payload))
                }
            }
        }
    }

    /**
     * Makes a GET request to `/wp-json/wc/v3/orders/{remoteOrderId}` to fetch a single order by the remoteOrderId.
     *
     * @param [orderId] Unique server id of the order to fetch
     */
    suspend fun fetchSingleOrder(site: SiteModel, orderId: Long): RemoteOrderPayload.Fetching {
        val url = WOOCOMMERCE.orders.id(orderId).pathV3
        val params = mapOf("_fields" to ORDER_FIELDS)

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = OrderDto::class.java,
            params = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let { orderDto ->
                    val newModel = orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                    RemoteOrderPayload.Fetching(newModel, site)
                } ?: RemoteOrderPayload.Fetching(
                    OrderError(GENERIC_ERROR, "Success response with empty data"),
                    OrderEntity(
                        orderId = orderId,
                        localSiteId = site.localId()
                    ) to emptyList(),
                    site
                )
            }
            is WPAPIResponse.Error -> {
                val orderError = wpAPINetworkErrorToOrderError(response.error)
                RemoteOrderPayload.Fetching(
                    orderError,
                    OrderEntity(
                        orderId = orderId,
                        localSiteId = site.localId()
                    ) to emptyList(),
                    site
                )
            }
        }
    }

    /**
     * Makes a GET call to `/wp-json/wc/v3/reports/orders/totals` retrieving count of orders for
     * the given WooCommerce [SiteModel], broken down by order status.
     *
     * Dispatches a [WCOrderAction.FETCHED_ORDERS_COUNT] action with the resulting count.
     *
     * @param [filterByStatus] The order status to return a count for
     */
    fun fetchOrderCountSync(site: SiteModel, filterByStatus: String) {
        coroutineEngine.launch(T.API, this, "fetchOrderCount") {
            dispatcher.dispatch(WCOrderActionBuilder.newFetchedOrdersCountAction(
                doFetchOrderCount(site, filterByStatus))
            )
        }
    }

    suspend fun fetchOrderCountSync(site: SiteModel, filterByStatus: String?) =
        doFetchOrderCount(site, filterByStatus)

    /**
     * Makes a GET request to `/wp-json/wc/v3/orders` for a single order of a specific type (or any type) in order to
     * determine if there are any orders in the store.
     *
     *
     * @param [filterByStatus] Nullable. If not null, consider only orders with a matching order status.
     */
    suspend fun fetchHasOrders(
        site: SiteModel,
        filterByStatus: String? = null
    ): FetchHasOrdersResponsePayload {
        val statusFilter = if (filterByStatus.isNullOrBlank()) {
            "any"
        } else {
            filterByStatus
        }

        val url = WOOCOMMERCE.orders.pathV3
        val params = mapOf(
            "per_page" to "1",
            "offset" to "0",
            "status" to statusFilter
        )

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<OrderDto>::class.java,
            params = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    FetchHasOrdersResponsePayload(
                        site,
                        filterByStatus,
                        it.isNotEmpty()
                    )
                } ?: FetchHasOrdersResponsePayload(
                    OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                    site
                )
            }
            is WPAPIResponse.Error -> {
                val orderError = wpAPINetworkErrorToOrderError(response.error)
                FetchHasOrdersResponsePayload(
                    orderError,
                    site
                )
            }
        }
    }

    /**
     * Makes a PUT call to `/wp-json/wc/v3/orders/<id>` updating the order.
     */
    private suspend fun updateOrder(
        orderToUpdate: OrderEntity,
        site: SiteModel,
        updatePayload: Map<String, Any>
    ): RemoteOrderPayload.Updating {
        val url = WOOCOMMERCE.orders.id(orderToUpdate.orderId).pathV3

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            clazz = OrderDto::class.java,
            body = updatePayload.plus("_fields" to ORDER_FIELDS)
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let { orderDto ->
                    val newModel = orderDtoMapper.toDatabaseEntity(orderDto, site.localId())
                        .first
                        .copy(
                            orderId = orderToUpdate.orderId
                        )
                    RemoteOrderPayload.Updating(newModel, site)
                } ?: RemoteOrderPayload.Updating(
                    OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                    orderToUpdate,
                    site
                )
            }
            is WPAPIResponse.Error -> {
                val orderError = wpAPINetworkErrorToOrderError(response.error)
                RemoteOrderPayload.Updating(
                    orderError,
                    orderToUpdate,
                    site
                )
            }
        }
    }

    suspend fun updateOrderStatusAndPaymentMethod(
        orderToUpdate: OrderEntity,
        site: SiteModel,
        status: String,
        paymentMethodId: String? = null,
        paymentMethodTitle: String? = null,
    ): RemoteOrderPayload.Updating {
        val updatePayload = mutableMapOf<String, Any>()
        updatePayload["status"] = status
        paymentMethodId?.let {
            updatePayload["payment_method"] = paymentMethodId
        }
        paymentMethodTitle?.let {
            updatePayload["payment_method_title"] = paymentMethodTitle
        }
        return updateOrder(orderToUpdate, site, updatePayload)
    }

    suspend fun updateCustomerOrderNote(
        orderToUpdate: OrderEntity,
        site: SiteModel,
        newNotes: String
    ) =
        updateOrder(orderToUpdate, site, mapOf("customer_note" to newNotes))

    suspend fun updateBillingAddress(
        orderToUpdate: OrderEntity,
        site: SiteModel,
        billing: Billing
    ) =
        updateOrder(orderToUpdate, site, mapOf("billing" to billing))

    suspend fun updateShippingAddress(
        orderToUpdate: OrderEntity,
        site: SiteModel,
        shipping: Shipping
    ) =
        updateOrder(orderToUpdate, site, mapOf("shipping" to shipping))

    suspend fun updateBothOrderAddresses(
        orderToUpdate: OrderEntity,
        site: SiteModel,
        shipping: Shipping,
        billing: Billing
    ) = updateOrder(
        orderToUpdate, site,
        mapOf("shipping" to shipping, "billing" to billing)
    )

    /**
     * Makes a GET call to `/wp-json/wc/v3/orders/<id>/notes` retrieving a list of notes for the
     * given WooCommerce [SiteModel] and [OrderEntity].
     */
    suspend fun fetchOrderNotes(
        orderId: Long,
        site: SiteModel
    ): WooPayload<List<OrderNoteEntity>> {
        val url = WOOCOMMERCE.orders.id(orderId).notes.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<OrderNoteApiResponse>::class.java
        )

        return response.toWooPayload { orderNotesResponse ->
            orderNotesResponse.map {
                it.toDataModel(site.localId(), RemoteId(orderId))
            }
        }
    }

    /**
     * Makes a POST call to `/wp-json.wc/v3/orders/<id>/notes` saving the provided note for the
     * given WooCommerce [SiteModel] and [OrderEntity].
     */
    suspend fun postOrderNote(
        orderId: Long,
        site: SiteModel,
        note: String,
        isCustomerNote: Boolean
    ): WooPayload<OrderNoteEntity> {
        val url = WOOCOMMERCE.orders.id(orderId).notes.pathV3

        val body = mutableMapOf(
            "note" to note,
            "customer_note" to isCustomerNote,
            "added_by_user" to true
        )

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = OrderNoteApiResponse::class.java,
            body = body
        )

        return response.toWooPayload { orderNoteResponse ->
            orderNoteResponse.toDataModel(site.localId(), RemoteId(orderId))
        }
    }

    /**
     * Makes a GET call to `/wp-json/wc/v2/orders/<order_id>/shipment-trackings/`, retrieving
     * a list of shipment tracking objects for a single [OrderEntity].
     *
     * Note: This is not currently supported in v3, but will be in the short future.
     *
     */
    suspend fun fetchOrderShipmentTrackings(
        site: SiteModel,
        orderId: Long
    ): FetchOrderShipmentTrackingsResponsePayload {
        val url = WOOCOMMERCE.orders.id(orderId).shipment_trackings.pathV2
        val params = mapOf("_fields" to TRACKING_FIELDS)

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<OrderShipmentTrackingApiResponse>::class.java,
            params = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val trackings = response.data?.map {
                    orderShipmentTrackingResponseToModel(it).apply {
                        localSiteId = site.id
                        this.orderId = orderId
                    }
                }.orEmpty()

                FetchOrderShipmentTrackingsResponsePayload(site, orderId, trackings)
            }
            is WPAPIResponse.Error -> {
                val trackingsError = wpAPINetworkErrorToOrderError(response.error)
                FetchOrderShipmentTrackingsResponsePayload(trackingsError, site, orderId)
            }
        }
    }

    /**
     * Posts a new Order Shipment Tracking record to the API for an order.
     *
     * Makes a POST call to save a Shipment Tracking record.
     * The API calls for different fields depending on if the new record uses a custom provider or not, so this is
     * why there is an if-statement. Either way, the same standard [WCOrderShipmentTrackingModel] is returned.
     *
     * Note: This API does not currently support v3.
     */
    suspend fun addOrderShipmentTrackingForOrder(
        site: SiteModel,
        orderId: Long,
        tracking: WCOrderShipmentTrackingModel,
        isCustomProvider: Boolean
    ): AddOrderShipmentTrackingResponsePayload {
        val url = WOOCOMMERCE.orders.id(orderId).shipment_trackings.pathV2
        val body = if (isCustomProvider) {
            mutableMapOf(
                "custom_tracking_provider" to tracking.trackingProvider,
                "custom_tracking_link" to tracking.trackingLink
            )
        } else {
            mutableMapOf("tracking_provider" to tracking.trackingProvider)
        }
        body["tracking_number"] = tracking.trackingNumber
        body["date_shipped"] = tracking.dateShipped

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = OrderShipmentTrackingApiResponse::class.java,
            body = body
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val trackingModel = orderShipmentTrackingResponseToModel(it).apply {
                        this.orderId = orderId
                        localSiteId = site.id
                    }
                    AddOrderShipmentTrackingResponsePayload(site, orderId, trackingModel)
                } ?: AddOrderShipmentTrackingResponsePayload(
                    OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                    site,
                    orderId,
                    tracking
                )
            }
            is WPAPIResponse.Error -> {
                val trackingsError = wpAPINetworkErrorToOrderError(response.error)
                AddOrderShipmentTrackingResponsePayload(trackingsError, site, orderId, tracking)
            }
        }
    }

    /**
     * Deletes a single shipment tracking record for an order.
     *
     * Makes a POST call requesting a DELETE method on
     * `/wp-json/wc/v2/orders/<order_id>/shipment_trackings/<tracking_id>/`
     *
     * Note this is currently not supported in v3, but will be in the future.
     */
    suspend fun deleteShipmentTrackingForOrder(
        site: SiteModel,
        orderId: Long,
        tracking: WCOrderShipmentTrackingModel
    ): DeleteOrderShipmentTrackingResponsePayload {
        val url = WOOCOMMERCE.orders.id(orderId)
            .shipment_trackings.tracking(tracking.remoteTrackingId).pathV2

        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = url,
            clazz = OrderShipmentTrackingApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    val model = orderShipmentTrackingResponseToModel(it).apply {
                        localSiteId = site.id
                        this.orderId = orderId
                        id = tracking.id
                    }
                    DeleteOrderShipmentTrackingResponsePayload(
                        site,
                        orderId,
                        model
                    )
                } ?: DeleteOrderShipmentTrackingResponsePayload(
                    OrderError(type = GENERIC_ERROR, message = "Success response with empty data"),
                    site,
                    orderId,
                    tracking
                )
            }
            is WPAPIResponse.Error -> DeleteOrderShipmentTrackingResponsePayload(
                wpAPINetworkErrorToOrderError(response.error),
                site,
                orderId,
                tracking
            )
        }
    }

    /**
     * Fetches a list of shipment providers from the WooCommerce Shipment Tracking plugin.
     *
     * Makes a GET call to `/wp-json/wc/v2/orders/<order_id>/shipment-trackings/providers/`,
     * retrieving a list of shipment tracking provider objects. The `<order_id>`
     * argument is only needed because it is a requirement of the plugins API even though this data is not directly
     * related to shipment providers.
     */
    suspend fun fetchOrderShipmentProviders(
        site: SiteModel,
        order: OrderEntity
    ): FetchOrderShipmentProvidersResponsePayload {
        val url = WOOCOMMERCE.orders.id(order.orderId).shipment_trackings.providers.pathV2
        val params = emptyMap<String, String>()

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = JsonElement::class.java,
            params = params
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    try {
                        val providers = jsonResponseToShipmentProviderList(site, it)
                        FetchOrderShipmentProvidersResponsePayload(site, order, providers)
                    } catch (e: IllegalStateException) {
                        // we have at least once instance of the response being invalid json so we catch the exception
                        // https://github.com/wordpress-mobile/WordPress-FluxC-Android/issues/1331
                        AppLog.e(
                            T.UTILS,
                            "IllegalStateException parsing shipment provider list, response = $it"
                        )
                        FetchOrderShipmentProvidersResponsePayload(
                            OrderError(INVALID_RESPONSE, it.toString()),
                            site,
                            order
                        )
                    }
                } ?: FetchOrderShipmentProvidersResponsePayload(
                    OrderError(GENERIC_ERROR, "Success response with empty data"),
                    site,
                    order
                )
            }

            is WPAPIResponse.Error -> {
                val trackingError = wpAPINetworkErrorToOrderError(response.error)
                FetchOrderShipmentProvidersResponsePayload(trackingError, site, order)
            }
        }
    }

    suspend fun createOrder(
        site: SiteModel,
        request: UpdateOrderRequest,
        attributionSourceType: String?
    ): WooPayload<OrderEntity> {
        val url = WOOCOMMERCE.orders.pathV3
        val metaData = mapOf(
            "meta_data" to listOfNotNull(
                attributionSourceType?.let {
                    mapOf(
                        "key" to OrderAttributionInfoKeys.SOURCE_TYPE,
                        "value" to attributionSourceType
                    )
                }
            )
        )

        val body = request.toNetworkRequest() + metaData

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = OrderDto::class.java,
            body = body
        )

        return response.toWooPayload { orderDto ->
            orderDtoMapper.toDatabaseEntity(orderDto, site.localId()).first
        }
    }

    suspend fun updateOrder(
        site: SiteModel,
        orderId: Long,
        request: UpdateOrderRequest
    ): WooPayload<OrderEntity> {
        val url = WOOCOMMERCE.orders.id(orderId).pathV3
        val body = request.toNetworkRequest()

        val response = wooNetwork.executePutGsonRequest(
            site = site,
            path = url,
            clazz = OrderDto::class.java,
            body = body
        )

        return response.toWooPayload { orderDto ->
            orderDtoMapper.toDatabaseEntity(orderDto, site.localId()).first
        }
    }

    suspend fun deleteOrder(
        site: SiteModel,
        orderId: Long,
        trash: Boolean
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.orders.id(orderId).pathV3

        val response = wooNetwork.executeDeleteGsonRequest(
            site = site,
            path = url,
            clazz = Unit::class.java,
            params = mapOf("force" to trash.not().toString())
        )

        return when (response) {
            is WPAPIResponse.Success -> WooPayload(Unit)
            is WPAPIResponse.Error -> WooPayload(response.error.toWooError())
        }
    }

    /**
     * expirationDate: Formatted as yyyy-mm-dd.
     * expirationDays: A number, 0 is today, 1 is tomorrow, etc.
     * forceNew: Defaults to false, if true, creates a new receipt even if one already exists for the order.
     */
    suspend fun fetchOrdersReceipt(
        site: SiteModel,
        orderId: Long,
        expirationDate: String? = null,
        expirationDays: Int? = null,
        forceNew: Boolean? = null
    ): WooPayload<OrderReceiptResponse> {
        val params = mutableMapOf<String, String>().apply {
            expirationDate?.let { put("expiration_date", it) }
            expirationDays?.let { put("expiration_days", it.toString()) }
            forceNew?.let { put("force_new", it.toString()) }
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = WOOCOMMERCE.orders.id(orderId).receipt.pathV3,
            clazz = OrderReceiptResponse::class.java,
            body = params
        )

        return response.toWooPayload { it }
    }

    suspend fun sendOrderReceipt(
        site: SiteModel,
        orderId: Long,
    ): WooPayload<Unit> {
        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = WOOCOMMERCE.orders.id(orderId).actions.send_order_details.pathV3,
            clazz = Unit::class.java,
        )

        return response.toWooPayload { it }
    }

    private suspend fun doFetchOrderCount(site: SiteModel, filterByStatus: String?): FetchOrdersCountResponsePayload {
        val url = WOOCOMMERCE.reports.orders.totals.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<OrderCountApiResponse>::class.java,
            params = if (filterByStatus != null) mapOf("status" to filterByStatus) else emptyMap()
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val total = if (filterByStatus == null) {
                    response.data?.sumOf { it.total }
                } else {
                    response.data?.find { it.slug == filterByStatus }?.total
                }

                total?.let {
                    FetchOrdersCountResponsePayload(site, filterByStatus, it)
                } ?: run {
                    val orderError = OrderError(OrderErrorType.ORDER_STATUS_NOT_FOUND)
                    FetchOrdersCountResponsePayload(
                        orderError,
                        site,
                        filterByStatus
                    )
                }
            }

            is WPAPIResponse.Error -> {
                val orderError = wpAPINetworkErrorToOrderError(response.error)
                FetchOrdersCountResponsePayload(orderError, site, filterByStatus)
            }
        }
    }

    /**
    * Performs a batch update of order statuses via the WooCommerce REST API.
    *
    * This endpoint enables updating multiple orders to the same status in a single network request.
    * The WooCommerce API has a limit of 100 orders per batch update.
    *
    * @param site The site to perform the update on
    * @param orderIds List of order IDs to update. Error if exceeds [BATCH_UPDATE_LIMIT]
    * @param newStatus The new status to set for all specified orders
    * @return [BulkUpdateOrderStatusResponsePayload] containing either the update results or an error
    */
    suspend fun batchUpdateOrdersStatus(
        site: SiteModel,
        orderIds: List<Long>,
        newStatus: String
    ): BulkUpdateOrderStatusResponsePayload {
        // Check batch update limit
        if (orderIds.size > BATCH_UPDATE_LIMIT) {
            return BulkUpdateOrderStatusResponsePayload(
                error = OrderError(
                    type = OrderErrorType.BULK_UPDATE_LIMIT_EXCEEDED,
                    message = "Cannot update more than 100 orders at once"
                )
            )
        }

        val url = WOOCOMMERCE.orders.batch.pathV3
        val updateRequests = orderIds.map { orderId ->
            mapOf(
                "id" to orderId,
                "status" to newStatus
            )
        }

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = BatchOrderApiResponse::class.java,
            body = mapOf("update" to updateRequests)
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    BulkUpdateOrderStatusResponsePayload(it.update)
                } ?: BulkUpdateOrderStatusResponsePayload(
                    OrderError(GENERIC_ERROR, "Success response with empty data")
                )
            }

            is WPAPIResponse.Error -> {
                val orderError = wpAPINetworkErrorToOrderError(response.error)
                BulkUpdateOrderStatusResponsePayload(orderError)
            }
        }
    }

    private fun UpdateOrderRequest.toNetworkRequest(): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            customerId?.let { put("customer_id", it) }
            status?.let { put("status", it.statusKey) }
            lineItems?.let { put("line_items", it) }
            shippingAddress?.toDto()?.let { put("shipping", it) }
            billingAddress?.toDto()?.let { put("billing", it) }
            feeLines?.let { put("fee_lines", it) }
            shippingLines?.let { put("shipping_lines", it) }
            customerNote?.let { put("customer_note", it) }
            couponLines?.let { put("coupon_lines", it) }
            giftCard
                ?.takeIf { it.isNotEmpty() }
                ?.let { mapOf("code" to it) }
                ?.let { put("gift_cards", listOf(it)) }
        }
    }

    private fun orderResponseToOrderSummaryModel(response: OrderSummaryApiResponse): WCOrderSummaryModel {
        return WCOrderSummaryModel().apply {
            orderId = response.id ?: 0
            dateCreated = convertDateToUTCString(response.dateCreatedGmt)
            dateModified = convertDateToUTCString(response.dateModifiedGmt)
        }
    }

    private fun wpAPINetworkErrorToOrderError(wpAPINetworkError: WPAPINetworkError): OrderError {
        val orderErrorType = when {
            wpAPINetworkError.errorCode == "rest_invalid_param" -> OrderErrorType.INVALID_PARAM
            wpAPINetworkError.errorCode == "woocommerce_rest_shop_order_invalid_id" -> OrderErrorType.INVALID_ID
            wpAPINetworkError.errorCode == "rest_no_route" -> OrderErrorType.PLUGIN_NOT_ACTIVE
            wpAPINetworkError.type == BaseRequest.GenericErrorType.PARSE_ERROR -> OrderErrorType.PARSE_ERROR
            wpAPINetworkError.type == BaseRequest.GenericErrorType.TIMEOUT -> TIMEOUT_ERROR
            else -> OrderErrorType.fromString(wpAPINetworkError.errorCode.orEmpty())
        }
        return OrderError(orderErrorType, wpAPINetworkError.combinedErrorMessage)
    }

    private fun orderStatusResponseToOrderStatusModel(
        response: OrderStatusApiResponse,
        site: SiteModel
    ): WCOrderStatusModel {
        return WCOrderStatusModel().apply {
            localSiteId = site.id
            statusKey = response.slug ?: ""
            label = response.name ?: ""
            statusCount = response.total
        }
    }

    private fun orderShipmentTrackingResponseToModel(
        response: OrderShipmentTrackingApiResponse
    ): WCOrderShipmentTrackingModel {
        return WCOrderShipmentTrackingModel().apply {
            remoteTrackingId = response.tracking_id ?: ""
            trackingNumber = response.tracking_number ?: ""
            trackingProvider = response.tracking_provider ?: ""
            trackingLink = response.tracking_link ?: ""
            dateShipped = response.date_shipped ?: ""
        }
    }

    private fun convertDateToUTCString(date: String?): String =
        date?.let { DateUtils.formatGmtAsUtcDateString(it) } ?: "" // Store the date in UTC format

    private fun jsonResponseToShipmentProviderList(
        site: SiteModel,
        response: JsonElement
    ): List<WCOrderShipmentProviderModel> {
        val providers = mutableListOf<WCOrderShipmentProviderModel>()
        response.asJsonObject.entrySet()
            .forEach { countryEntry: MutableEntry<String, JsonElement> ->
                countryEntry.value.asJsonObject.entrySet().map { carrierEntry ->
                    carrierEntry?.let { carrier ->
                        val provider = WCOrderShipmentProviderModel().apply {
                            localSiteId = site.id
                            this.country = countryEntry.key
                            this.carrierName = carrier.key
                            this.carrierLink = carrier.value.asString
                        }
                        providers.add(provider)
                    }
                }
            }

        return providers
    }

    companion object {
        private val ORDER_FIELDS = arrayOf(
            "billing",
            "coupon_lines",
            "currency",
            "order_key",
            "customer_note",
            "date_created_gmt",
            "date_modified_gmt",
            "date_paid_gmt",
            "discount_total",
            "fee_lines",
            "tax_lines",
            "id",
            "line_items",
            "number",
            "payment_method",
            "payment_method_title",
            "prices_include_tax",
            "refunds",
            "shipping",
            "shipping_lines",
            "shipping_total",
            "status",
            "total",
            "total_tax",
            "meta_data",
            "payment_url",
            "is_editable",
            "needs_payment",
            "needs_processing",
            "shipping_tax"
        ).joinToString(separator = ",")

        private val TRACKING_FIELDS = arrayOf(
            "date_shipped",
            "tracking_id",
            "tracking_link",
            "tracking_number",
            "tracking_provider"
        ).joinToString(separator = ",")

        private const val BATCH_UPDATE_LIMIT = 100
    }

    enum class SortOrder(val value: String) {
        ASCENDING("asc"),
        DESCENDING("desc");
    }

    enum class OrderBy(val value: String) {
        DATE("date"),
        ID("id"),
        INCLUDE("include"),
        TITLE("title"),
        SLUG("slug");
    }
}
