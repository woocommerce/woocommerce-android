package org.wordpress.android.fluxc.store

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.ListActionBuilder
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.SERVER_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.BatchOrderApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.OrderBy
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient.SortOrder
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.store.ListStore.FetchedListItemsPayload
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType
import org.wordpress.android.fluxc.store.ListStore.OnListDataFailure
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.PARSE_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.TIMEOUT_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrdersStatusResult.FailedOrder
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LargeClass", "LongParameterList", "TooManyFunctions")
@Singleton
class WCOrderStore @Inject constructor(
    dispatcher: Dispatcher,
    private val wcOrderRestClient: OrderRestClient,
    private val wcOrderFetcher: WCOrderFetcher,
    private val coroutineEngine: CoroutineEngine,
    private val ordersDaoDecorator: OrdersDaoDecorator,
    private val orderNotesDao: OrderNotesDao,
    private val metaDataDao: MetaDataDao,
    private val insertOrder: InsertOrder
) : Store(dispatcher) {
    companion object {
        const val NUM_ORDERS_PER_FETCH = 15
    }

    class FetchOrdersPayload(
        var site: SiteModel,
        var statusFilter: String? = null,
        var loadMore: Boolean = false
    ) : Payload<BaseNetworkError>()

    class FetchOrderListPayload(
        val listDescriptor: WCOrderListDescriptor,
        val offset: Long,
        val requestStartTime: Calendar = Calendar.getInstance()
    ) : Payload<BaseNetworkError>()

    class FetchOrdersByIdsPayload(
        val site: SiteModel,
        val orderIds: List<Long>
    ) : Payload<BaseNetworkError>()

    data class FetchOrdersResponsePayload(
        val site: SiteModel,
        val ordersWithMeta: List<Pair<OrderEntity, List<WCMetaData>>> = emptyList(),
        val statusFilter: String? = null,
        val loadedMore: Boolean = false,
        val canLoadMore: Boolean = false
    ) : Payload<OrderError>() {
        val orders = ordersWithMeta.map { it.first }

        constructor(error: OrderError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class FetchOrderListResponsePayload(
        val listDescriptor: WCOrderListDescriptor,
        var orderSummaries: List<WCOrderSummaryModel> = emptyList(),
        var loadedMore: Boolean = false,
        var canLoadMore: Boolean = false,
        val requestStartTime: Calendar
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            listDescriptor: WCOrderListDescriptor,
            requestStartTime: Calendar
        ) : this(listDescriptor, requestStartTime = requestStartTime) {
            this.error = error
        }
    }

    class FetchOrdersByIdsResponsePayload(
        val site: SiteModel,
        var orderIds: List<Long>,
        var fetchedOrders: List<Pair<OrderEntity, List<WCMetaData>>> = emptyList()
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            orderIds: List<Long>
        ) : this(site = site, orderIds = orderIds) {
            this.error = error
        }
    }

    class SearchOrdersPayload(
        var site: SiteModel,
        var searchQuery: String,
        var offset: Int
    ) : Payload<BaseNetworkError>()

    class SearchOrdersResponsePayload(
        var site: SiteModel,
        var searchQuery: String,
        var canLoadMore: Boolean = false,
        var offset: Int = 0,
        var orders: List<OrderEntity> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, query: String) : this(site, query) {
            this.error = error
        }
    }

    class FetchOrdersCountPayload(
        var site: SiteModel,
        var statusFilter: String
    ) : Payload<BaseNetworkError>()

    class FetchOrdersCountResponsePayload(
        var site: SiteModel,
        var statusFilter: String?,
        var count: Int = 0
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, statusFilter: String?) : this(site, statusFilter) {
            this.error = error
        }
    }

    class FetchHasOrdersResponsePayload(
        var site: SiteModel,
        var statusFilter: String? = null,
        var hasOrders: Boolean = false
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class UpdateOrderStatusPayload(
        val order: OrderEntity,
        val site: SiteModel,
        val status: String
    ) : Payload<BaseNetworkError>()

    sealed class RemoteOrderPayload : Payload<OrderError>() {
        abstract val site: SiteModel
        abstract val order: OrderEntity

        data class Fetching(
            val orderWithMeta: Pair<OrderEntity, List<WCMetaData>>,
            override val site: SiteModel
        ) : RemoteOrderPayload() {
            override val order = orderWithMeta.first

            constructor(
                error: OrderError,
                order: Pair<OrderEntity, List<WCMetaData>>,
                site: SiteModel
            ) : this(order, site) {
                this.error = error
            }
        }

        data class Updating(
            override val order: OrderEntity,
            override val site: SiteModel
        ) : RemoteOrderPayload() {
            constructor(
                error: OrderError,
                order: OrderEntity,
                site: SiteModel
            ) : this(order, site) {
                this.error = error
            }
        }
    }

    sealed class UpdateOrderResult {
        abstract val event: OnOrderChanged

        data class OptimisticUpdateResult(override val event: OnOrderChanged) : UpdateOrderResult()
        data class RemoteUpdateResult(override val event: OnOrderChanged) : UpdateOrderResult()
    }

    class FetchOrderStatusOptionsPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchOrderStatusOptionsResponsePayload(
        val site: SiteModel,
        val labels: List<WCOrderStatusModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel) : this(site) {
            this.error = error
        }
    }

    class FetchOrderShipmentTrackingsResponsePayload(
        var site: SiteModel,
        var orderId: Long,
        var trackings: List<WCOrderShipmentTrackingModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, orderId: Long) :
            this(site, orderId) {
            this.error = error
        }
    }

    class AddOrderShipmentTrackingPayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel,
        val isCustomProvider: Boolean
    ) : Payload<BaseNetworkError>()

    class AddOrderShipmentTrackingResponsePayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel?
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            orderId: Long,
            tracking: WCOrderShipmentTrackingModel
        ) : this(site, orderId, tracking) {
            this.error = error
        }
    }

    class DeleteOrderShipmentTrackingPayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel
    ) : Payload<BaseNetworkError>()

    class DeleteOrderShipmentTrackingResponsePayload(
        val site: SiteModel,
        var orderId: Long,
        val tracking: WCOrderShipmentTrackingModel?
    ) : Payload<OrderError>() {
        constructor(
            error: OrderError,
            site: SiteModel,
            orderId: Long,
            tracking: WCOrderShipmentTrackingModel?
        ) : this(site, orderId, tracking) {
            this.error = error
        }
    }

    class FetchOrderShipmentProvidersPayload(
        val site: SiteModel,
        val order: OrderEntity
    ) : Payload<BaseNetworkError>()

    class FetchOrderShipmentProvidersResponsePayload(
        val site: SiteModel,
        val order: OrderEntity,
        val providers: List<WCOrderShipmentProviderModel> = emptyList()
    ) : Payload<OrderError>() {
        constructor(error: OrderError, site: SiteModel, order: OrderEntity) : this(site, order) {
            this.error = error
        }
    }

    class BulkUpdateOrderStatusResponsePayload(
        val response: List<BatchOrderApiResponse.OrderResponse>
    ) : Payload<OrderError>() {
        constructor(error: OrderError) : this(emptyList()) {
            this.error = error
        }
    }

    data class UpdateOrdersStatusResult(
        val updatedOrders: List<Long> = emptyList(),
        val failedOrders: List<FailedOrder> = emptyList()
    ) {
        data class FailedOrder(
            val id: Long,
            val errorCode: String,
            val errorMessage: String,
            val errorStatus: Int
        )
    }

    data class OrderError(val type: OrderErrorType = GENERIC_ERROR, val message: String = "") : OnChangedError

    enum class OrderErrorType {
        INVALID_PARAM,
        INVALID_ID,
        ORDER_STATUS_NOT_FOUND,
        PLUGIN_NOT_ACTIVE,
        INVALID_RESPONSE,
        GENERIC_ERROR,
        PARSE_ERROR,
        TIMEOUT_ERROR,
        EMPTY_BILLING_EMAIL,
        BULK_UPDATE_LIMIT_EXCEEDED;

        companion object {
            private val reverseMap = values().associateBy(OrderErrorType::name)
            fun fromString(type: String) = reverseMap[type.uppercase(Locale.US)] ?: GENERIC_ERROR
        }
    }

    sealed class HasOrdersResult {
        data class Success(val hasOrders: Boolean) : HasOrdersResult()
        data class Failure(val error: OrderError) : HasOrdersResult()
    }

    sealed class OrdersCountResult {
        data class Success(val count: Int) : OrdersCountResult()
        data class Failure(val error: OrderError) : OrdersCountResult()
    }

    // OnChanged events
    data class OnOrderChanged(
        val statusFilter: String? = null,
        val canLoadMore: Boolean = false,
        val causeOfChange: WCOrderAction? = null,
        private val orderError: OrderError? = null
    ) : OnChanged<OrderError>() {
        init {
            super.error = orderError
        }
    }

    /**
     * Emitted after fetching a list of Order summaries from the network.
     */
    class OnOrderSummariesFetched(
        val listDescriptor: WCOrderListDescriptor,
        val duration: Long
    ) : OnChanged<OrderError>()

    class OnOrdersFetchedByIds(
        val site: SiteModel,
        val orderIds: List<Long>
    ) : OnChanged<OrderError>()

    class OnOrdersSearched(
        var searchQuery: String = "",
        var canLoadMore: Boolean = false,
        var nextOffset: Int = 0,
        var searchResults: List<OrderEntity> = emptyList()
    ) : OnChanged<OrderError>()

    class OnOrderStatusOptionsChanged(
        var rowsAffected: Int
    ) : OnChanged<OrderError>()

    class OnOrderShipmentProvidersChanged(
        var rowsAffected: Int
    ) : OnChanged<OrderError>()

    override fun onRegister() = AppLog.d(API, "WCOrderStore onRegister")

    /**
     * Given a [SiteModel] and optional statuses, returns all orders for that site matching any of those statuses.
     */
    suspend fun getOrdersForSite(site: SiteModel, vararg status: String) = if (status.isEmpty()) {
        ordersDaoDecorator.getOrdersForSite(site.localId())
    } else {
        ordersDaoDecorator.getOrdersForSite(site.localId(), status = status.asList())
    }

    suspend fun getPaidOrdersForSiteDesc(siteModel: SiteModel) =
        ordersDaoDecorator.getPaidOrdersForSiteDesc(siteModel.localId())

    /**
     * Observe the changes to orders for a given [SiteModel]
     *
     * @param site the current site
     * @param statuses an optional list of statuses to filter the list of orders, pass an empty list to include all
     *                 orders
     */
    fun observeOrdersForSite(site: SiteModel, statuses: List<String> = emptyList()): Flow<List<OrderEntity>> {
        return if (statuses.isEmpty()) {
            ordersDaoDecorator.observeOrdersForSite(site.localId())
        } else {
            ordersDaoDecorator.observeOrdersForSite(site.localId(), statuses)
        }
    }

    /**
     * Observe the changes to the number of orders for a given [SiteModel]
     *
     * @param site the current site
     * @param statuses a list of statuses to filter the list of orders, pass null to include all orders
     */
    fun observeOrderCountForSite(
        site: SiteModel,
        statuses: List<String>? = null
    ): Flow<Int> = ordersDaoDecorator.observeOrderCountForSite(site.localId(), statuses)

    fun getOrdersForDescriptor(
        orderListDescriptor: WCOrderListDescriptor,
        orderIds: List<Long>
    ): Map<Long, OrderEntity> {
        val orders = ordersDaoDecorator.getOrdersForSiteByRemoteIds(orderListDescriptor.site.localId(), orderIds)
        return orders.associateBy { it.orderId }
    }

    fun getOrderSummariesByRemoteOrderIds(
        site: SiteModel,
        orderIds: List<Long>
    ): Map<RemoteId, WCOrderSummaryModel> {
        val orderSummaries = OrderSqlUtils.getOrderSummariesForRemoteIds(site, orderIds)
        return orderSummaries.associateBy { RemoteId(it.orderId) }
    }

    /**
     * Given an order id and [SiteModel],
     * returns the corresponding order from the database as a [OrderEntity].
     */
    suspend fun getOrderByIdAndSite(orderId: Long, site: SiteModel): OrderEntity? {
        return ordersDaoDecorator.getOrder(orderId, site.localId())
    }

    /**
     * Given an order id and [SiteModel],
     * returns the metadata from the database for an order
     */
    suspend fun getOrderMetadata(orderId: Long, site: SiteModel): List<WCMetaData> {
        return metaDataDao.getMetaData(site.localId(), orderId).map { it.toDomainModel() }
    }

    /**
     * Given an order id and [SiteModel],
     * returns the displayable metadata from the database for an order
     */
    suspend fun getDisplayableOrderMetadata(orderId: Long, site: SiteModel): List<WCMetaData> {
        return metaDataDao.getDisplayableMetaData(site.localId(), orderId).map { it.toDomainModel() }
    }

    /**
     * Given an order id and [SiteModel],
     * returns whether there is metadata in the database for an order
     */
    suspend fun hasOrderMetadata(orderId: Long, site: SiteModel): Boolean {
        return metaDataDao.hasMetaData(orderId, site.localId())
    }

    /**
     * Given an order id and [SiteModel],
     * returns whether there is displayable metadata in the database for an order
     */
    suspend fun hasDisplayableOrderMetadata(orderId: Long, site: SiteModel): Boolean {
        return metaDataDao.getDisplayableMetaDataCount(site.localId(), orderId) > 0
    }

    /**
     * Returns the notes belonging to supplied [OrderEntity] as a list of [OrderNoteEntity].
     */
    suspend fun getOrderNotesForOrder(site: SiteModel, orderId: Long): List<OrderNoteEntity> =
            orderNotesDao.queryNotesOfOrder(localSiteId = site.localId(), orderId = RemoteId(orderId))

    /**
     * Returns the order status options available for the provided site [SiteModel] as a list of [WCOrderStatusModel].
     */
    fun getOrderStatusOptionsForSite(site: SiteModel): List<WCOrderStatusModel> =
        OrderSqlUtils.getOrderStatusOptionsForSite(site)

    /**
     * Returns the order status as a [WCOrderStatusModel] that matches the provided order status key.
     */
    fun getOrderStatusForSiteAndKey(site: SiteModel, key: String): WCOrderStatusModel? =
        OrderSqlUtils.getOrderStatusOptionForSiteByKey(site, key)

    /**
     * Returns shipment trackings as list of [WCOrderShipmentTrackingModel] for a single [OrderEntity]
     */
    fun getShipmentTrackingsForOrder(site: SiteModel, orderId: Long): List<WCOrderShipmentTrackingModel> =
        OrderSqlUtils.getShipmentTrackingsForOrder(site, orderId)

    fun getShipmentTrackingByTrackingNumber(site: SiteModel, orderId: Long, trackingNumber: String) =
        OrderSqlUtils.getShipmentTrackingByTrackingNumber(site, orderId, trackingNumber)

    /**
     * Returns the shipment providers as a list of [WCOrderShipmentProviderModel]
     */
    fun getShipmentProvidersForSite(site: SiteModel): List<WCOrderShipmentProviderModel> =
        OrderSqlUtils.getOrderShipmentProvidersForSite(site)

    @Suppress("ComplexMethod", "UseCheckOrError")
    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? WCOrderAction ?: return
        when (actionType) {
            // remote actions
            FETCH_ORDERS -> fetchOrders(action.payload as FetchOrdersPayload)
            WCOrderAction.FETCH_ORDER_LIST -> fetchOrderList(action.payload as FetchOrderListPayload)
            WCOrderAction.FETCH_ORDERS_BY_IDS -> fetchOrdersByIds(action.payload as FetchOrdersByIdsPayload)
            WCOrderAction.FETCH_ORDERS_COUNT -> fetchOrdersCount(action.payload as FetchOrdersCountPayload)
            WCOrderAction.UPDATE_ORDER_STATUS ->
                throw IllegalStateException("Invalid action. Use suspendable updateOrderStatus(..) directly")
            WCOrderAction.SEARCH_ORDERS -> searchOrders(action.payload as SearchOrdersPayload)
            WCOrderAction.FETCH_ORDER_STATUS_OPTIONS ->
                fetchOrderStatusOptions(action.payload as FetchOrderStatusOptionsPayload)

            // remote responses
            WCOrderAction.FETCHED_ORDERS -> handleFetchOrdersCompleted(action.payload as FetchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_LIST ->
                handleFetchOrderListCompleted(action.payload as FetchOrderListResponsePayload)
            WCOrderAction.FETCHED_ORDERS_BY_IDS ->
                handleFetchOrderByIdsCompleted(action.payload as FetchOrdersByIdsResponsePayload)
            WCOrderAction.FETCHED_ORDERS_COUNT ->
                handleFetchOrdersCountCompleted(action.payload as FetchOrdersCountResponsePayload)
            WCOrderAction.SEARCHED_ORDERS -> handleSearchOrdersCompleted(action.payload as SearchOrdersResponsePayload)
            WCOrderAction.FETCHED_ORDER_STATUS_OPTIONS ->
                handleFetchOrderStatusOptionsCompleted(action.payload as FetchOrderStatusOptionsResponsePayload)
        }
    }

    private fun fetchOrders(payload: FetchOrdersPayload) {
        val offset = if (payload.loadMore) {
            ordersDaoDecorator.getOrderCountForSite(payload.site.localId())
        } else {
            0
        }
        wcOrderRestClient.fetchOrders(payload.site, offset, payload.statusFilter)
    }

    private fun fetchOrderList(payload: FetchOrderListPayload) {
        wcOrderRestClient.fetchOrderListSummaries(
            listDescriptor = payload.listDescriptor,
            offset = payload.offset,
            requestStartTime = payload.requestStartTime
        )
    }

    private fun fetchOrdersByIds(payload: FetchOrdersByIdsPayload) {
        payload.orderIds.chunked(NUM_ORDERS_PER_FETCH).forEach { idsToFetch ->
            wcOrderRestClient.fetchOrdersByIds(payload.site, idsToFetch)
        }
    }

    private fun searchOrders(payload: SearchOrdersPayload) {
        wcOrderRestClient.searchOrders(payload.site, payload.searchQuery, payload.offset)
    }

    private fun fetchOrdersCount(payload: FetchOrdersCountPayload) {
        with(payload) { wcOrderRestClient.fetchOrderCountSync(site, statusFilter) }
    }

    suspend fun fetchOrdersCount(site: SiteModel, filterByStatus: String? = null): OrdersCountResult {
        return coroutineEngine.withDefaultContext(API, this, "checkIfHasOrders") {
            val result = wcOrderRestClient.fetchOrderCountSync(site, filterByStatus)
            return@withDefaultContext if (result.isError) {
                 OrdersCountResult.Failure(result.error)
            } else {
                OrdersCountResult.Success(result.count)
            }
        }
    }

    suspend fun hasOrders(site: SiteModel): HasOrdersResult {
        return coroutineEngine.withDefaultContext(API, this, "checkIfHasOrders") {
            val ordersCount = ordersDaoDecorator.getOrderCountForSite(site.localId())
            return@withDefaultContext if (ordersCount > 0) {
                HasOrdersResult.Success(true)
            } else {
                fetchHasOrders(site, null)
            }
        }
    }

    suspend fun fetchHasOrders(site: SiteModel, status: String?): HasOrdersResult {
        return coroutineEngine.withDefaultContext(API, this, "fetchHasOrders") {
            val result = wcOrderRestClient.fetchHasOrders(site, status)

            return@withDefaultContext if (result.isError) {
                HasOrdersResult.Failure(result.error)
            } else {
                HasOrdersResult.Success(result.hasOrders)
            }
        }
    }

    suspend fun fetchSingleOrder(site: SiteModel, remoteOrderId: Long): OnOrderChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchSingleOrder") {
            val result = wcOrderRestClient.fetchSingleOrder(site, remoteOrderId)

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                insertOrder(site.localId(), result.orderWithMeta)
                OnOrderChanged()
            }
        }
    }

    suspend fun fetchSingleOrderSync(site: SiteModel, remoteOrderId: Long): WooResult<OrderEntity> {
        return coroutineEngine.withDefaultContext(API, this, "fetchSingleOrderSync") {
            val result = wcOrderRestClient.fetchSingleOrder(site, remoteOrderId)

            return@withDefaultContext if (result.isError) {
                WooResult(WooError(API_ERROR, SERVER_ERROR, result.error.message))
            } else {
                insertOrder(site.localId(), result.orderWithMeta)
                WooResult(result.order)
            }
        }
    }

    @Suppress("SpreadOperator")
    suspend fun fetchOrders(
        site: SiteModel,
        count: Int = NUM_ORDERS_PER_FETCH,
        page: Int = 1,
        orderBy: OrderBy = OrderBy.DATE,
        sortOrder: SortOrder = SortOrder.DESCENDING,
        statusFilter: String? = null,
        deleteOldData: Boolean = page == 1
    ): WooResult<List<OrderEntity>> {
        return coroutineEngine.withDefaultContext(API, this, "fetchOrders") {
            val result = wcOrderRestClient.fetchOrders(site, count, page, orderBy, sortOrder, statusFilter)

            return@withDefaultContext if (result.isError) {
                WooResult(WooError(API_ERROR, SERVER_ERROR, result.error.message))
            } else {
                if (deleteOldData) {
                    ordersDaoDecorator.deleteOrdersForSite(site.localId())
                }
                insertOrder(site.localId(), *result.ordersWithMeta.toTypedArray())

                WooResult(result.orders)
            }
        }
    }

    suspend fun updateOrderStatus(
        orderId: Long,
        site: SiteModel,
        newStatus: WCOrderStatusModel
    ): Flow<UpdateOrderResult> =
        updateOrderStatusAndPaymentMethod(orderId, site, newStatus, null, null)

    suspend fun updateOrderStatusAndPaymentMethod(
        orderId: Long,
        site: SiteModel,
        newStatus: WCOrderStatusModel,
        newPaymentMethodId: String?,
        newPaymentMethodTitle: String?,
    ): Flow<UpdateOrderResult> {
        return coroutineEngine.flowWithDefaultContext(API, this, "updateOrderStatusAndPaymentMethod") {
            val orderModel = ordersDaoDecorator.getOrder(orderId, site.localId())

            if (orderModel != null) {
                optimisticallyUpdateOrder(
                    orderId,
                    site.localId(),
                    newStatus.statusKey,
                    newPaymentMethodId,
                    newPaymentMethodTitle,
                    OrdersDaoDecorator.ListUpdateStrategy.INVALIDATE
                )

                val optimisticUpdateResult = OnOrderChanged(
                    causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS
                )

                emit(OptimisticUpdateResult(optimisticUpdateResult))

                // Ensure the code gets executed even when the VM dies - eg. when the client app marks an order as
                // completed and navigates to a different screen.
                val remoteUpdateResult: OnOrderChanged = withContext(NonCancellable) {
                    val remotePayload = wcOrderRestClient.updateOrderStatusAndPaymentMethod(
                        orderModel,
                        site,
                        newStatus.statusKey,
                        newPaymentMethodId,
                        newPaymentMethodTitle,
                    )
                    if (remotePayload.isError) {
                        revertOptimisticOrderUpdate(remotePayload)
                    } else {
                        ordersDaoDecorator.insertOrUpdateOrder(
                            remotePayload.order,
                            // Re-fetch the list to ensure the order is correctly placed even when a filter is applied
                            OrdersDaoDecorator.ListUpdateStrategy.REFRESH
                        )
                        OnOrderChanged()
                    }.copy(causeOfChange = WCOrderAction.UPDATE_ORDER_STATUS)
                }

                emit(RemoteUpdateResult(remoteUpdateResult))
                // Needs to remain here until all event bus observables are removed from the client code
                emitChange(remoteUpdateResult)
            } else {
                emit(
                    OptimisticUpdateResult(
                        OnOrderChanged(
                            orderError = OrderError(
                                message = "Order with id $orderId not found"
                            )
                        )
                    )
                )
            }
        }
    }

    suspend fun fetchOrdersReceipt(
        site: SiteModel,
        orderId: Long,
        expirationDate: String? = null,
        expirationDays: Int? = null,
        forceNew: Boolean? = null
    ) = wcOrderRestClient.fetchOrdersReceipt(
        site,
        orderId,
        expirationDate,
        expirationDays,
        forceNew
    )

    suspend fun sendOrderReceipt(
        site: SiteModel,
        orderId: Long,
    ) = wcOrderRestClient.sendOrderReceipt(
        site,
        orderId
    )

    private suspend fun optimisticallyUpdateOrder(
        orderId: Long,
        localSiteId: LocalId,
        newStatus: String,
        newPaymentMethodId: String? = null,
        newPaymentMethodTitle: String? = null,
        listUpdateStrategy: OrdersDaoDecorator.ListUpdateStrategy = OrdersDaoDecorator.ListUpdateStrategy.DEFAULT
    ) {
        val updatedOrder = ordersDaoDecorator.getOrder(orderId, localSiteId)!!.let {
            it.copy(
                status = newStatus,
                paymentMethod = newPaymentMethodId ?: it.paymentMethod,
                paymentMethodTitle = newPaymentMethodTitle ?: it.paymentMethodTitle
            )
        }
        ordersDaoDecorator.insertOrUpdateOrder(updatedOrder, listUpdateStrategy)
    }

    @Suppress("SpreadOperator")
    suspend fun fetchOrderNotes(
        site: SiteModel,
        orderId: Long
    ): WooResult<List<OrderNoteEntity>> {
        return coroutineEngine.withDefaultContext(API, this, "fetchOrderNotes") {
            val result = wcOrderRestClient.fetchOrderNotes(orderId, site)

            return@withDefaultContext result.let {
                if (!it.isError) {
                    orderNotesDao.insertNotes(*it.result!!.toTypedArray())
                }
                result.asWooResult()
            }
        }
    }

    suspend fun postOrderNote(
        site: SiteModel,
        orderId: Long,
        note: String,
        isCustomerNote: Boolean
    ): WooResult<OrderNoteEntity> {
        return coroutineEngine.withDefaultContext(API, this, "postOrderNote") {
            val result = wcOrderRestClient.postOrderNote(orderId, site, note, isCustomerNote)

            return@withDefaultContext if (result.isError) {
                result.asWooResult()
            } else {
                orderNotesDao.insertNotes(result.result!!)
                result.asWooResult()
            }
        }
    }

    private fun fetchOrderStatusOptions(payload: FetchOrderStatusOptionsPayload) {
        wcOrderRestClient.fetchOrderStatusOptions(payload.site)
    }

    suspend fun fetchOrderShipmentTrackings(orderId: Long, site: SiteModel): OnOrderChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchOrderShipmentTrackings") {
            val result = wcOrderRestClient.fetchOrderShipmentTrackings(site, orderId)
            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                // Calculate which existing records should be deleted because they no longer exist in the payload
                val existingTrackings = OrderSqlUtils.getShipmentTrackingsForOrder(
                    result.site,
                    result.orderId
                )
                val deleteTrackings = mutableListOf<WCOrderShipmentTrackingModel>()
                existingTrackings.iterator().forEach { existing ->
                    var exists = false
                    result.trackings.iterator().forEach nti@{ newTracking ->
                        if (newTracking.remoteTrackingId == existing.remoteTrackingId) {
                            exists = true
                            return@nti
                        }
                    }
                    if (!exists) deleteTrackings.add(existing)
                }
                var rowsAffected = deleteTrackings.sumBy { OrderSqlUtils.deleteOrderShipmentTrackingById(it) }

                // Save new shipment trackings to the database
                rowsAffected += result.trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
                OnOrderChanged()
            }
        }
    }

    suspend fun addOrderShipmentTracking(payload: AddOrderShipmentTrackingPayload): OnOrderChanged {
        return coroutineEngine.withDefaultContext(API, this, "addOrderShipmentTracking") {
            val result = with(payload) {
                wcOrderRestClient.addOrderShipmentTrackingForOrder(
                    site, orderId, tracking, isCustomProvider
                )
            }

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                result.tracking?.let { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
                OnOrderChanged()
            }
        }
    }

    suspend fun deleteOrderShipmentTracking(payload: DeleteOrderShipmentTrackingPayload): OnOrderChanged {
        return coroutineEngine.withDefaultContext(API, this, "deleteOrderShipmentTracking") {
            val result = with(payload) {
                wcOrderRestClient.deleteShipmentTrackingForOrder(site, orderId, tracking)
            }

            return@withDefaultContext if (result.isError) {
                OnOrderChanged(orderError = result.error)
            } else {
                // Remove the record from the database and send response
                result.tracking?.let { OrderSqlUtils.deleteOrderShipmentTrackingById(it) }
                OnOrderChanged()
            }
        }
    }

    suspend fun fetchOrderShipmentProviders(
        payload: FetchOrderShipmentProvidersPayload
    ): OnOrderShipmentProvidersChanged {
        return coroutineEngine.withDefaultContext(API, this, "fetchOrderShipmentProviders") {
            val result = with(payload) {
                wcOrderRestClient.fetchOrderShipmentProviders(site, order)
            }

            return@withDefaultContext if (result.isError) {
                OnOrderShipmentProvidersChanged(0).also { it.error = result.error }
            } else {
                // Delete all providers from the db
                OrderSqlUtils.deleteOrderShipmentProvidersForSite(payload.site)

                // Add new list to the database
                val rowsAffected = result.providers.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentProvider(it) }
                OnOrderShipmentProvidersChanged(rowsAffected)
            }
        }
    }

    @Suppress("SpreadOperator")
    private fun handleFetchOrdersCompleted(payload: FetchOrdersResponsePayload) {
        coroutineEngine.launch(API, this, "handleFetchOrdersCompleted") {
            val onOrderChanged: OnOrderChanged = if (payload.isError) {
                OnOrderChanged(orderError = payload.error)
            } else {
                // Clear existing uploading orders if this is a fresh fetch (loadMore = false in the original request)
                // This is the simplest way of keeping our local orders in sync with remote orders
                // (in case of deletions, or if the user manual changed some order IDs).
                if (!payload.loadedMore) {
                    ordersDaoDecorator.deleteOrdersForSite(payload.site.localId())
                    orderNotesDao.deleteOrderNotesForSite(payload.site.localId())
                    OrderSqlUtils.deleteOrderShipmentTrackingsForSite(payload.site)
                }

                insertOrder(payload.site.localId(), *payload.ordersWithMeta.toTypedArray())
                OnOrderChanged(payload.statusFilter, canLoadMore = payload.canLoadMore)
            }.copy(causeOfChange = FETCH_ORDERS)

            emitChange(onOrderChanged)
        }
    }

    @Suppress("ForbiddenComment")
    private fun handleFetchOrderListCompleted(payload: FetchOrderListResponsePayload) {
        // TODO: Ideally we would have a separate process that prunes the following
        // tables of defunct records:
        // - WCOrderModel
        // - WCOrderNoteModel
        // - WCOrderShipmentTrackingModel
        if (!payload.isError) {
            // Save order summaries to the db
            OrderSqlUtils.insertOrUpdateOrderSummaries(payload.orderSummaries)

            // Fetch outdated or missing orders
            fetchOutdatedOrMissingOrders(payload.listDescriptor.site, payload.orderSummaries)
        }

        val duration = Calendar.getInstance().timeInMillis - payload.requestStartTime.timeInMillis
        emitChange(OnOrderSummariesFetched(listDescriptor = payload.listDescriptor, duration = duration))

        mDispatcher.dispatch(ListActionBuilder.newFetchedListItemsAction(FetchedListItemsPayload(
            listDescriptor = payload.listDescriptor,
            remoteItemIds = payload.orderSummaries.map { it.orderId },
            loadedMore = payload.loadedMore,
            canLoadMore = payload.canLoadMore,
            error = payload.error?.let { fetchError ->
                ListError(
                    type = when (fetchError.type) {
                        PARSE_ERROR -> ListErrorType.PARSE_ERROR
                        TIMEOUT_ERROR -> ListErrorType.TIMEOUT_ERROR
                        else -> ListErrorType.GENERIC_ERROR
                    },
                    message = fetchError.message
                )
            }
        )))
    }

    private fun fetchOutdatedOrMissingOrders(site: SiteModel, fetchedSummaries: List<WCOrderSummaryModel>) {
        val fetchedSummariesIds = fetchedSummaries.map { it.orderId }
        val localOrdersForFetchedSummaries =
            ordersDaoDecorator.getOrdersForSiteByRemoteIds(site.localId(), fetchedSummariesIds)

        val idsToFetch = outdatedOrdersIds(fetchedSummaries, localOrdersForFetchedSummaries)
            .plus(missingOrdersIds(fetchedSummariesIds, localOrdersForFetchedSummaries))

        wcOrderFetcher.fetchOrders(site = site, orderIds = idsToFetch)
    }

    private fun outdatedOrdersIds(
        fetchedSummaries: List<WCOrderSummaryModel>,
        localOrdersForSiteByRemoteIds: List<OrderEntity>
    ): List<Long> {
        val summaryModifiedDates = fetchedSummaries.associate { it.orderId to it.dateModified }

        return localOrdersForSiteByRemoteIds.filter { order ->
            order.dateModified != summaryModifiedDates[order.orderId]
        }.map(OrderEntity::orderId)
    }

    private fun missingOrdersIds(
        fetchedSummariesIds: List<Long>,
        localOrdersForSiteByRemoteIds: List<OrderEntity>
    ): List<Long> {
        return fetchedSummariesIds.minus(
            localOrdersForSiteByRemoteIds.map(OrderEntity::orderId)
        )
    }

    @Suppress("SpreadOperator")
    private fun handleFetchOrderByIdsCompleted(payload: FetchOrdersByIdsResponsePayload) {
        coroutineEngine.launch(API, this, "handleFetchOrderByIdsCompleted") {
            val onOrdersFetchedByIds = if (payload.isError) {
                OnOrdersFetchedByIds(payload.site, payload.orderIds).apply { error = payload.error }
            } else {
                OnOrdersFetchedByIds(
                    payload.site,
                    payload.fetchedOrders.map { it.first }.map { it.orderId })
            }

            // Notify listeners that the list of orders has changed (only call this if there is no error)
            val listTypeIdentifier = WCOrderListDescriptor.calculateTypeIdentifier(localSiteId = payload.site.id)

            if (!payload.isError) {
                // Save the list of orders to the database

                insertOrder(payload.site.localId(), *payload.fetchedOrders.toTypedArray())

                mDispatcher.dispatch(
                    ListActionBuilder.newListDataInvalidatedAction(
                        listTypeIdentifier
                    )
                )
            } else {
                val errorType = if(payload.error.type == PARSE_ERROR){
                    ListErrorType.PARSE_ERROR
                } else {
                    ListErrorType.GENERIC_ERROR
                }

                mDispatcher.dispatch(
                    ListActionBuilder.newListDataFailureAction(
                            OnListDataFailure(listTypeIdentifier).apply {
                                error = ListError(
                                    errorType,
                                    payload.error.message
                                )
                            }
                    )
                )
            }
            emitChange(onOrdersFetchedByIds)
        }
    }

    private fun handleSearchOrdersCompleted(payload: SearchOrdersResponsePayload) {
        val onOrdersSearched = if (payload.isError) {
            OnOrdersSearched(payload.searchQuery)
        } else {
            OnOrdersSearched(payload.searchQuery, payload.canLoadMore, payload.offset, payload.orders)
        }
        emitChange(onOrdersSearched)
    }

    /**
     * This is a response to a request to retrieve only the count of orders matching a filter. These
     * results are not stored in the database.
     */
    private fun handleFetchOrdersCountCompleted(payload: FetchOrdersCountResponsePayload) {
        val onOrderChanged = if (payload.isError) {
            OnOrderChanged(orderError = payload.error)
        } else {
            with(payload) {
                OnOrderChanged(statusFilter = statusFilter)
            }
        }.copy(causeOfChange = WCOrderAction.FETCH_ORDERS_COUNT)
        emitChange(onOrderChanged)
    }

    private suspend fun revertOptimisticOrderUpdate(payload: RemoteOrderPayload.Updating): OnOrderChanged {
        optimisticallyUpdateOrder(
            payload.order.orderId,
            payload.order.localSiteId,
            payload.order.status,
            payload.order.paymentMethod,
            payload.order.paymentMethodTitle
        )
        return OnOrderChanged(orderError = payload.error)
    }

    private fun handleFetchOrderStatusOptionsCompleted(payload: FetchOrderStatusOptionsResponsePayload) {
        val onOrderStatusLabelsChanged: OnOrderStatusOptionsChanged

        if (payload.isError) {
            onOrderStatusLabelsChanged = OnOrderStatusOptionsChanged(0).also { it.error = payload.error }
        } else {
            onOrderStatusLabelsChanged = onOrderStatusOptionsChanged(payload)
        }

        emitChange(onOrderStatusLabelsChanged)
    }

    private fun onOrderStatusOptionsChanged(
        payload: FetchOrderStatusOptionsResponsePayload
    ): OnOrderStatusOptionsChanged {
        val existingOptions = OrderSqlUtils.getOrderStatusOptionsForSite(payload.site)
        var rowsAffected = addOrUpdateOptions(payload, existingOptions).sumBy {
            OrderSqlUtils.insertOrUpdateOrderStatusOption(it)
        }
        rowsAffected += deleteOptions(payload, existingOptions).sumBy {
            OrderSqlUtils.deleteOrderStatusOption(it)
        }
        return OnOrderStatusOptionsChanged(rowsAffected)
    }

    @Suppress("NestedBlockDepth")
    private fun addOrUpdateOptions(
        payload: FetchOrderStatusOptionsResponsePayload,
        existingOptions: List<WCOrderStatusModel>
    ): List<WCOrderStatusModel> {
        val addOrUpdateOptions = mutableListOf<WCOrderStatusModel>()
        payload.labels.iterator().forEach { newOption ->
            var exists = false
            existingOptions.iterator().forEach eoi@{ existingOption ->
                if (newOption.statusKey == existingOption.statusKey) {
                    exists = true
                    if (newOption.label != existingOption.label ||
                        newOption.statusCount != existingOption.statusCount) {
                        addOrUpdateOptions.add(newOption)
                    }
                    return@eoi
                }
            }
            if (!exists) addOrUpdateOptions.add(newOption)
        }
        return addOrUpdateOptions
    }

    private fun deleteOptions(
        payload: FetchOrderStatusOptionsResponsePayload,
        existingOptions: List<WCOrderStatusModel>
    ): List<WCOrderStatusModel> {
        val deleteOptions = mutableListOf<WCOrderStatusModel>()
        existingOptions.iterator().forEach { existingOption ->
            var exists = false
            payload.labels.iterator().forEach noi@{ newOption ->
                if (newOption.statusKey == existingOption.statusKey) {
                    exists = true
                    return@noi
                }
            }
            if (!exists) deleteOptions.add(existingOption)
        }
        return deleteOptions
    }

    suspend fun fetchOrdersListFirstPage(
        listDescriptor: WCOrderListDescriptor,
        deleteOldData: Boolean = false
    ): WooResult<List<OrderEntity>> {
        val response = wcOrderRestClient.fetchOrdersListFirstPage(listDescriptor)
        return if (response.isError) {
            WooResult(WooError(API_ERROR, SERVER_ERROR, response.error.message))
        } else {
            val result = response.result.orEmpty()
            val orders = result.map { it.first }

            orders.map { order ->
                WCOrderSummaryModel().apply {
                    localSiteId = listDescriptor.site.localId().value
                    orderId = order.orderId
                    dateCreated = order.dateCreated
                    dateModified = order.dateModified
                }
            }.let { orderSummaries ->
                OrderSqlUtils.insertOrUpdateOrderSummaries(orderSummaries)
            }

            if (deleteOldData) {
                ordersDaoDecorator.deleteOrdersForSite(listDescriptor.site.localId())
            }

            @Suppress("SpreadOperator")
            insertOrder(listDescriptor.site.localId(), *result.toTypedArray())
            WooResult(orders)
        }
    }

    @Suppress("NestedBlockDepth")
    suspend fun batchUpdateOrdersStatus(
        site: SiteModel,
        orderIds: List<Long>,
        newStatus: WCOrderStatusModel
    ): WooResult<UpdateOrdersStatusResult> {
        val result = wcOrderRestClient.batchUpdateOrdersStatus(site, orderIds, newStatus.statusKey)

        return if (!result.isError) {
            val orders = result.response
            val updatedOrders = mutableListOf<Long>()
            val failedOrders = mutableListOf<FailedOrder>()

            orders.forEach { response ->
                when (response) {
                    is BatchOrderApiResponse.OrderResponse.Success -> {
                        response.order.id?.let { updatedOrders.add(it) }
                    }

                    is BatchOrderApiResponse.OrderResponse.Error -> {
                        failedOrders.add(
                            FailedOrder(
                                id = response.id,
                                errorCode = response.error.code,
                                errorMessage = response.error.message,
                                errorStatus = response.error.data.status
                            )
                        )
                    }
                }
            }

            WooResult(UpdateOrdersStatusResult(updatedOrders, failedOrders))
        } else {
            WooResult(WooError(API_ERROR, SERVER_ERROR, result.error.message))
        }
    }
}
