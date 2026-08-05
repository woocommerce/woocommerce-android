@file:Suppress("DEPRECATION_ERROR")

package org.wordpress.android.fluxc.wc.order

import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.check
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.action.ListAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder.newFetchedOrderListAction
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.BatchOrderApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus.COMPLETED
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderNoteApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.toDataModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrderNotesDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.persistence.entity.OrderNoteEntity
import org.wordpress.android.fluxc.store.InsertOrder
import org.wordpress.android.fluxc.store.ListStore.MarkListsNeedRefreshPayload
import org.wordpress.android.fluxc.store.WCOrderFetcher
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.BulkUpdateOrderStatusResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchHasOrdersResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListResponsePayload
import org.wordpress.android.fluxc.store.WCOrderStore.HasOrdersResult
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val COD_PAYMENT_METHOD_ID = "cod"
private const val CUSTOM_PAYMENT_METHOD_TITLE = "Pay in Person"

@InternalCoroutinesApi
@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
internal class WCOrderStoreTest {
    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val orderFetcher: WCOrderFetcher = mock()
    private val orderRestClient: OrderRestClient = mock()
    lateinit var ordersDaoDecorator: OrdersDaoDecorator
    lateinit var orderNotesDao: OrderNotesDao
    lateinit var metaDataDao: MetaDataDao
    lateinit var orderStore: WCOrderStore
    private val insertOrder: InsertOrder = mock()
    private lateinit var dispatcher: Dispatcher

    @Before
    fun setUp() {
        dispatcher = spy(Dispatcher())
        ordersDaoDecorator = OrdersDaoDecorator(dispatcher, databaseRule.db.ordersDao)
        orderNotesDao = databaseRule.db.orderNotesDao
        metaDataDao = databaseRule.db.metaDataDao

        orderStore = WCOrderStore(
            dispatcher = dispatcher,
            wcOrderRestClient = orderRestClient,
            wcOrderFetcher = orderFetcher,
            coroutineEngine = initCoroutineEngine(),
            ordersDaoDecorator = ordersDaoDecorator,
            orderNotesDao = orderNotesDao,
            metaDataDao = metaDataDao,
            orderShipmentProvidersDao = databaseRule.db.orderShipmentProvidersDao,
            orderShipmentTrackingDao = databaseRule.db.orderShipmentTrackingDao,
            orderStatusDao = databaseRule.db.orderStatusDao,
            orderSummaryDao = databaseRule.db.orderSummaryDao,
            insertOrder = insertOrder
        )
    }

    @Test
    fun testSimpleInsertionAndRetrieval() {
        runBlocking {
            val orderModel = generateSampleOrder(42)
            ordersDaoDecorator.insertOrUpdateOrder(orderModel)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }

            val storedOrders = ordersDaoDecorator.getOrdersForSite(site.localId())
            assertEquals(1, storedOrders.size)
            assertEquals(42, storedOrders[0].orderId)
            assertEquals(orderModel, storedOrders[0])
        }
    }

    @Test
    fun testGetOrders() {
        runBlocking {
            val processingOrder = generateSampleOrder(3).saveToDb()
            generateSampleOrder(4, CoreOrderStatus.ON_HOLD.value).saveToDb()
            val cancelledOrder = generateSampleOrder(5, CoreOrderStatus.CANCELLED.value).saveToDb()

            val site = SiteModel().apply { id = processingOrder.localSiteId.value }

            val orderList = orderStore
                .getOrdersForSite(site, CoreOrderStatus.PROCESSING.value, CoreOrderStatus.CANCELLED.value)

            assertEquals(2, orderList.size)
            assertTrue(orderList.contains(processingOrder))
            assertTrue(orderList.contains(cancelledOrder))

            val fullOrderList = orderStore.getOrdersForSite(site)
            assertEquals(3, fullOrderList.size)
        }
    }

    private suspend fun OrderEntity.saveToDb(): OrderEntity {
        ordersDaoDecorator.insertOrUpdateOrder(this)
        return copy()
    }

    private fun insertOrUpdate(item: OrderEntity) {
        runBlocking {
            ordersDaoDecorator.insertOrUpdateOrder(item)
        }
    }

    @Test
    fun testGetOrderByLocalId() {
        runBlocking {
            val sampleOrder = generateSampleOrder(3)
            ordersDaoDecorator.insertOrUpdateOrder(sampleOrder)

            val site = SiteModel().apply { this.id = sampleOrder.localSiteId.value }

            val retrievedOrder = orderStore.getOrderByIdAndSite(sampleOrder.orderId, site)
            assertEquals(sampleOrder, retrievedOrder)

            // Non-existent ID should return null
            // assertNull(orderStore.getOrderByIdentifier(OrderIdentifier(WCOrderModel(id = 1955))))
        }
    }

    @Test
    fun testCustomOrderStatus() {
        runBlocking {
            val customStatus = "chronologically-incongruous"
            val customStatusOrder = generateSampleOrder(3, customStatus)
            ordersDaoDecorator.insertOrUpdateOrder(customStatusOrder)

            val site = SiteModel().apply { id = customStatusOrder.localSiteId.value }

            val orderList = orderStore.getOrdersForSite(site, customStatus)
            assertEquals(1, orderList.size)
            assertTrue(orderList.contains(customStatusOrder))

            val orderList2 = orderStore.getOrdersForSite(site, customStatus, CoreOrderStatus.CANCELLED.value)
            assertEquals(1, orderList2.size)
            assertTrue(orderList2.contains(customStatusOrder))

            val fullOrderList = orderStore.getOrdersForSite(site)
            assertEquals(1, fullOrderList.size)
        }
    }

    @Test
    fun testUpdateOrderStatus() = runBlocking {
        val orderModel = generateSampleOrder(42)
        ordersDaoDecorator.insertOrUpdateOrder(orderModel)
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val result = RemoteOrderPayload.Updating(orderModel.copy(status = CoreOrderStatus.REFUNDED.value), site)
        whenever(
            orderRestClient
                .updateOrderStatusAndPaymentDetails(eq(orderModel), eq(site), eq(CoreOrderStatus.REFUNDED.value), any())
        ).thenReturn(result)

        orderStore.updateOrderStatus(
            orderModel.orderId,
            site,
            WCOrderStatusModel(statusKey = CoreOrderStatus.REFUNDED.value)
        )
            .toList()

        with(orderStore.getOrderByIdAndSite(orderModel.orderId, site)!!) {
            // The version of the order model in the database should have the updated status
            assertEquals(CoreOrderStatus.REFUNDED.value, status)
            // Other fields should not be altered by the update
            assertEquals(orderModel.currency, currency)
        }
    }

    @Test
    fun `given a cached completed order, when fetching it as refunded, then the order list requires refresh`() =
        runBlocking {
            // GIVEN
            val cachedOrder = generateSampleOrder(42, CoreOrderStatus.COMPLETED.value).saveToDb()
            val site = SiteModel().apply { id = cachedOrder.localSiteId.value }
            val fetchedOrder = cachedOrder.copy(status = CoreOrderStatus.REFUNDED.value)
            whenever(orderRestClient.fetchSingleOrder(site, cachedOrder.orderId))
                .thenReturn(RemoteOrderPayload.Fetching(fetchedOrder to emptyList(), site))
            clearInvocations(dispatcher)

            // WHEN
            orderStore.fetchSingleOrder(site, cachedOrder.orderId)

            // THEN
            val captor = argumentCaptor<Action<*>>()
            verify(dispatcher, atLeastOnce()).dispatch(captor.capture())
            val refreshActions = captor.allValues
                .filter { it.type == ListAction.LIST_REQUIRES_REFRESH }
            assertThat(refreshActions).hasSize(1)
            assertThat(refreshActions.single().payload)
                .isEqualTo(WCOrderListDescriptor.calculateTypeIdentifier(site.id))
            Unit
        }

    @Test
    fun `given a cached order, when fetching content changes without a status change, then the list does not refresh`() =
        runBlocking {
            // GIVEN
            val cachedOrder = generateSampleOrder(42, CoreOrderStatus.COMPLETED.value).saveToDb()
            val site = SiteModel().apply { id = cachedOrder.localSiteId.value }
            val fetchedOrder = cachedOrder.copy(total = "15.0")
            whenever(orderRestClient.fetchSingleOrder(site, cachedOrder.orderId))
                .thenReturn(RemoteOrderPayload.Fetching(fetchedOrder to emptyList(), site))
            clearInvocations(dispatcher)

            // WHEN
            orderStore.fetchSingleOrder(site, cachedOrder.orderId)

            // THEN
            verify(dispatcher, never()).dispatch(any())
        }

    @Test
    fun `given a cached order, when fetching it fails, then the order list does not refresh`() = runBlocking {
        // GIVEN
        val cachedOrder = generateSampleOrder(42, CoreOrderStatus.COMPLETED.value).saveToDb()
        val site = SiteModel().apply { id = cachedOrder.localSiteId.value }
        whenever(orderRestClient.fetchSingleOrder(site, cachedOrder.orderId))
            .thenReturn(
                RemoteOrderPayload.Fetching(
                    error = OrderError(message = "Fetch failed"),
                    order = cachedOrder to emptyList(),
                    site = site
                )
            )
        clearInvocations(dispatcher)

        // WHEN
        orderStore.fetchSingleOrder(site, cachedOrder.orderId)

        // THEN
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `given no cached order, when fetching succeeds, then the order list does not refresh`() = runBlocking {
        // GIVEN
        val fetchedOrder = generateSampleOrder(42, CoreOrderStatus.REFUNDED.value)
        val site = SiteModel().apply { id = fetchedOrder.localSiteId.value }
        whenever(orderRestClient.fetchSingleOrder(site, fetchedOrder.orderId))
            .thenReturn(RemoteOrderPayload.Fetching(fetchedOrder to emptyList(), site))
        clearInvocations(dispatcher)

        // WHEN
        orderStore.fetchSingleOrder(site, fetchedOrder.orderId)

        // THEN
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `given payment details, when updateOrderStatusAndPaymentDetails, then pass correct payment details to wcOrderRestClient`() {
        runBlocking {
            // GIVEN
            val orderId = 42L
            val orderModel = generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            val newStatus = WCOrderStatusModel(statusKey = CoreOrderStatus.COMPLETED.value)
            val paymentMethodId = "cod"
            val paymentMethodTitle = "Cash on Delivery"
            val cashPaymentChangeDueAmount = "5.00"
            val paymentDetails = OrderRestClient.OrderUpdatePaymentDetails(
                paymentMethodId,
                paymentMethodTitle,
                cashPaymentChangeDueAmount
            )

            ordersDaoDecorator.insertOrUpdateOrder(orderModel)
            val result = RemoteOrderPayload.Updating(orderModel.copy(status = CoreOrderStatus.COMPLETED.value), site)

            whenever(
                orderRestClient.updateOrderStatusAndPaymentDetails(
                    orderModel,
                    site,
                    CoreOrderStatus.COMPLETED.value,
                    paymentDetails
                )
            ).thenReturn(result)

            // WHEN
            orderStore.updateOrderStatusAndPaymentDetails(
                orderId = orderId,
                site = site,
                newStatus = newStatus,
                newPaymentMethodId = paymentMethodId,
                newPaymentMethodTitle = paymentMethodTitle,
                cashPaymentChangeDueAmount = cashPaymentChangeDueAmount
            ).toList()

            // THEN
            verify(orderRestClient).updateOrderStatusAndPaymentDetails(
                eq(orderModel),
                eq(site),
                eq(newStatus.statusKey),
                eq(paymentDetails)
            )
        }
    }

    @Test
    fun testOrderErrorType() {
        assertEquals(OrderErrorType.INVALID_PARAM, OrderErrorType.fromString("invalid_param"))
        assertEquals(OrderErrorType.INVALID_PARAM, OrderErrorType.fromString("INVALID_PARAM"))
        assertEquals(OrderErrorType.GENERIC_ERROR, OrderErrorType.fromString(""))
    }

    @Test
    fun testGetOrderNotesForOrder() = runBlocking {
        val notesJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_notes.json")
        val orderId = 949L
        val localSiteId = 6
        val noteModels = getOrderNotesFromJsonString(notesJson, localSiteId, orderId)
        val orderModel = generateSampleOrder(orderId).copy(localSiteId = LocalId(localSiteId.toInt()))
        val site = SiteModel().apply { id = localSiteId }
        assertEquals(6, noteModels.size)
        orderNotesDao.insertNotes(noteModels[0])

        val retrievedNotes = orderStore.getOrderNotesForOrder(site, orderModel.orderId)
        assertEquals(1, retrievedNotes.size)
        assertEquals(noteModels[0], retrievedNotes[0])
    }

    @Test
    fun testOrderToIdentifierToOrder() {
        runBlocking {
            val site = SiteModel().apply { id = 6 }
            // Convert an order to identifier and restore it from the database
            generateSampleOrder(3).saveToDb().let { sampleOrder ->
                assertEquals(sampleOrder, orderStore.getOrderByIdAndSite(3, site))
            }

            // Attempt to restore an order that doesn't exist in the database
            generateSampleOrder(4).let {
                assertNull(orderStore.getOrderByIdAndSite(4, site))
            }

            // Restore an order that doesn't have a remote ID
            generateSampleOrder(0).saveToDb().let { draftOrder ->
                assertEquals(draftOrder, orderStore.getOrderByIdAndSite(0, site))
            }

            // Restore an order without a local ID by matching site and remote order IDs
            generateSampleOrder(3).let { duplicateRemoteOrder ->
                assertEquals(duplicateRemoteOrder, orderStore.getOrderByIdAndSite(3, site))
            }
        }
    }

    @Test
    fun testFetchingOnlyOutdatedOrMissingOrders() {
        runBlocking {
            val site = SiteModel().apply { id = 8 }

            val upToDate = setupUpToDateOrders(site)
            upToDate.orders.filterNotNull().forEach(::insertOrUpdate)
            assertThat(ordersDaoDecorator.getOrdersForSite(site.localId())).hasSize(10)

            val outdated = setupOutdatedOrders(site)
            outdated.orders.filterNotNull().forEach(::insertOrUpdate)
            assertThat(ordersDaoDecorator.getOrdersForSite(site.localId())).hasSize(20)

            val missing = setupMissingOrders()
            assertThat(ordersDaoDecorator.getOrdersForSite(site.localId())).hasSize(20)

            orderStore.onAction(
                newFetchedOrderListAction(
                    FetchOrderListResponsePayload(
                        WCOrderListDescriptor(site = site),
                        orderSummaries = upToDate.summaries + outdated.summaries + missing.summaries
                    )
                )
            )

            verify(orderFetcher).fetchOrders(eq(site), check { remoteIdsToFetch ->
                assertThat(remoteIdsToFetch).containsExactlyInAnyOrderElementsOf(
                    (outdated.summaries + missing.summaries).map { it.orderId.value }
                )
            })
        }
    }

    @Test
    fun `given fetch surfaces a new order, when fetch completes, then marks other lists of type for refresh`() =
        runBlocking {
            val site = SiteModel().apply { id = 8 }
            val descriptor = WCOrderListDescriptor(site = site, statusFilter = "processing")
            // Summaries not pre-inserted -> these orders are new to the device.
            val summaries = listOf(
                generateSampleOrderSummary(id = 8, remoteId = 1),
                generateSampleOrderSummary(id = 8, remoteId = 2)
            )
            clearInvocations(dispatcher)

            orderStore.onAction(
                newFetchedOrderListAction(FetchOrderListResponsePayload(descriptor, orderSummaries = summaries))
            )

            val captor = argumentCaptor<Action<*>>()
            verify(dispatcher, atLeastOnce()).dispatch(captor.capture())
            val markPayloads = captor.allValues
                .filter { it.type == ListAction.MARK_LISTS_OF_TYPE_NEED_REFRESH }
                .map { it.payload as MarkListsNeedRefreshPayload }
            assertThat(markPayloads).hasSize(1)
            assertThat(markPayloads.first().excludedDescriptor.uniqueIdentifier)
                .isEqualTo(descriptor.uniqueIdentifier)
            Unit
        }

    @Test
    fun `given fetch surfaces only known orders, when fetch completes, then does not mark lists for refresh`() =
        runBlocking {
            val site = SiteModel().apply { id = 8 }
            val summaries = listOf(generateSampleOrderSummary(id = 8, remoteId = 1))
            // Pre-insert the summary so the fetched order is already known.
            databaseRule.db.orderSummaryDao.upsertOrderSummaries(summaries)
            clearInvocations(dispatcher)

            orderStore.onAction(
                newFetchedOrderListAction(
                    FetchOrderListResponsePayload(WCOrderListDescriptor(site = site), orderSummaries = summaries)
                )
            )

            val captor = argumentCaptor<Action<*>>()
            verify(dispatcher, atLeastOnce()).dispatch(captor.capture())
            assertThat(
                captor.allValues.none { it.type == ListAction.MARK_LISTS_OF_TYPE_NEED_REFRESH }
            ).isTrue()
            Unit
        }

    @Test
    fun testUpdateOrderStatusRequestUpdatesLocalDatabase() = runBlocking {
        val orderModel = generateSampleOrder(42, orderStatus = CoreOrderStatus.PROCESSING.value)
            .saveToDb()
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val result = RemoteOrderPayload.Updating(orderModel.copy(status = CoreOrderStatus.COMPLETED.value), site)
        whenever(
            orderRestClient.updateOrderStatusAndPaymentDetails(
                eq(orderModel),
                eq(site),
                eq(CoreOrderStatus.COMPLETED.value),
                any()
            )
        )
            .thenReturn(result)

        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.status)
            .isEqualTo(CoreOrderStatus.PROCESSING.value)

        orderStore.updateOrderStatus(
            orderModel.orderId,
            site,
            WCOrderStatusModel(statusKey = CoreOrderStatus.COMPLETED.value)
        ).toList()

        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.status)
            .isEqualTo(CoreOrderStatus.COMPLETED.value)
        Unit
    }

    @Test
    fun testRevertLocalOrderUpdateIfRemoteUpdateFails() = runBlocking {
        val orderModel = generateSampleOrder(42, orderStatus = CoreOrderStatus.PROCESSING.value)
            .saveToDb()
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val error = OrderError()
        whenever(
            orderRestClient.updateOrderStatusAndPaymentDetails(
                any(),
                any(),
                any(),
                anyOrNull()
            )
        ).thenReturn(
            RemoteOrderPayload.Updating(
                error = error,
                order = orderModel,
                site = site
            )
        )

        val response = orderStore.updateOrderStatus(
            orderModel.orderId,
            site,
            WCOrderStatusModel(statusKey = CoreOrderStatus.COMPLETED.value)
        ).toList().last()

        // Ensure the error is sent in the response
        assertThat(response.event.error).isEqualTo(error)

        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.status)
            .isEqualTo(CoreOrderStatus.PROCESSING.value)
        Unit
    }

    @Test
    fun testUpdateOrderPaymentMethodRequestUpdatesLocalDatabase() = runBlocking {
        val orderModel = generateSampleOrder(
            42,
            orderStatus = CoreOrderStatus.PROCESSING.value,
            paymentMethod = "",
            paymentMethodTitle = ""
        )
            .saveToDb()
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        whenever(
            orderRestClient.updateOrderStatusAndPaymentDetails(
                orderModel,
                site,
                CoreOrderStatus.COMPLETED.value,
                OrderRestClient.OrderUpdatePaymentDetails(COD_PAYMENT_METHOD_ID, CUSTOM_PAYMENT_METHOD_TITLE)
            )
        ).thenReturn(
            RemoteOrderPayload.Updating(
                orderModel.copy(
                    status = CoreOrderStatus.COMPLETED.value,
                    paymentMethod = COD_PAYMENT_METHOD_ID,
                    paymentMethodTitle = CUSTOM_PAYMENT_METHOD_TITLE
                ),
                site
            )
        )

        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethod)
            .isEqualTo("")
        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethodTitle)
            .isEqualTo("")

        orderStore.updateOrderStatusAndPaymentDetails(
            orderModel.orderId,
            site,
            WCOrderStatusModel(statusKey = CoreOrderStatus.COMPLETED.value),
            newPaymentMethodId = COD_PAYMENT_METHOD_ID,
            newPaymentMethodTitle = CUSTOM_PAYMENT_METHOD_TITLE
        ).toList()

        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethod)
            .isEqualTo(COD_PAYMENT_METHOD_ID)
        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethodTitle)
            .isEqualTo(CUSTOM_PAYMENT_METHOD_TITLE)
        Unit
    }

    @Test
    fun testRevertLocalPaymentMethodIfRemoteUpdateFails() = runBlocking {
        val orderModel = generateSampleOrder(
            42,
            orderStatus = CoreOrderStatus.PROCESSING.value,
            paymentMethod = "",
            paymentMethodTitle = ""
        )
            .saveToDb()
        val site = SiteModel().apply { id = orderModel.localSiteId.value }
        val error = OrderError()
        whenever(
            orderRestClient.updateOrderStatusAndPaymentDetails(
                orderModel,
                site,
                CoreOrderStatus.COMPLETED.value,
                OrderRestClient.OrderUpdatePaymentDetails(COD_PAYMENT_METHOD_ID, CUSTOM_PAYMENT_METHOD_TITLE)
            )
        ).thenReturn(
            RemoteOrderPayload.Updating(
                error = error,
                order = orderModel,
                site = site
            )
        )

        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethod)
            .isEqualTo("")
        assertThat(ordersDaoDecorator.getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethodTitle)
            .isEqualTo("")

        val response = orderStore.updateOrderStatusAndPaymentDetails(
            orderModel.orderId,
            site,
            WCOrderStatusModel(statusKey = CoreOrderStatus.COMPLETED.value),
            newPaymentMethodId = COD_PAYMENT_METHOD_ID,
            newPaymentMethodTitle = CUSTOM_PAYMENT_METHOD_TITLE
        ).toList().last()

        // Ensure the error is sent in the response
        assertThat(response.event.error).isEqualTo(error)

        assertThat(
            ordersDaoDecorator
                .getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethod
        ).isEqualTo("")
        assertThat(
            ordersDaoDecorator
                .getOrder(orderModel.orderId, orderModel.localSiteId)?.paymentMethodTitle
        ).isEqualTo("")
        Unit
    }

    @Test
    fun testObserveOrdersCount() {
        runBlocking {
            val siteId = 5
            val site = SiteModel().apply { id = siteId }
            // When inserting 3 PROCESSING and 1 COMPLETED orders
            for (i in 1L..3L) {
                generateSampleOrder(
                    siteId = siteId,
                    orderId = i,
                    orderStatus = CoreOrderStatus.PROCESSING.value
                ).saveToDb()
            }

            generateSampleOrder(
                siteId = siteId,
                orderId = 4L,
                orderStatus = CoreOrderStatus.COMPLETED.value
            ).saveToDb()

            // Then PROCESSING orders count = 3
            var count = orderStore.observeOrderCountForSite(
                site,
                listOf(CoreOrderStatus.PROCESSING.value)
            ).first()

            assertThat(count).isEqualTo(3)

            count = orderStore.observeOrderCountForSite(
                site,
                listOf(CoreOrderStatus.COMPLETED.value)
            ).first()

            // Then COMPLETED orders count = 1
            assertThat(count).isEqualTo(1)
        }
    }

    @Test
    fun testHasOrdersWithoutLocalOrders() {
        runBlocking {
            // Given there are NO orders in the local database
            val orderModel = generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            val hasOrdersResponse = FetchHasOrdersResponsePayload(site = site, hasOrders = false)
            whenever(orderRestClient.fetchHasOrders(any(), anyOrNull()))
                .thenReturn(hasOrdersResponse)

            // When checking if the store has orders
            val result = orderStore.hasOrders(site)

            // Then check with the API if the store has orders
            verify(orderRestClient).fetchHasOrders(site, null)
            assertThat(result).isInstanceOf(HasOrdersResult.Success::class.java)
            (result as? HasOrdersResult.Success)?.let { success ->
                assertThat(success.hasOrders).isEqualTo(hasOrdersResponse.hasOrders)
            }
        }
    }

    @Test
    fun testHasOrdersWithLocalOrders() {
        runBlocking {
            // Given there are orders in the local database
            val orderModel = generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            orderModel.saveToDb()

            // When checking if the store has orders
            val result = orderStore.hasOrders(site)

            // Then use the database as proof that the store has orders and avoid
            // fetching data from the API
            verify(orderRestClient, never()).fetchHasOrders(site, null)
            assertThat(result).isInstanceOf(HasOrdersResult.Success::class.java)
            (result as? HasOrdersResult.Success)?.let { success ->
                assertThat(success.hasOrders).isEqualTo(true)
            }
        }
    }

    @Test
    fun testFetchOrdersReceipt() {
        runBlocking {
            val orderModel = generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            val orderId = 42L
            val expirationDate = "2021-01-05"
            val expirationDays = 30
            val forceNew = true

            orderStore.fetchOrdersReceipt(
                site,
                orderId,
                expirationDate,
                expirationDays,
                forceNew
            )

            verify(orderRestClient).fetchOrdersReceipt(
                site,
                orderId,
                expirationDate,
                expirationDays,
                forceNew
            )
        }
    }

    @Test
    fun testSendOrderReceipt() {
        runBlocking {
            val orderModel = generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            val orderId = 42L

            orderStore.sendOrderReceipt(site, orderId)

            verify(orderRestClient).sendOrderReceipt(site, orderId)
        }
    }

    @Test
    fun `given valid site and orderId, when sendOrderPOSSpecificReceipt is called, then sendOrderPOSSpecificReceipt in the rest client is triggered`() {
        runBlocking {
            // GIVEN
            val orderModel = generateSampleOrder(42)
            val site = SiteModel().apply { id = orderModel.localSiteId.value }
            val orderId = 42L
            val email = "test@example.com"

            // WHEN
            orderStore.sendOrderPOSSpecificReceipt(site, orderId, email, forceEmailUpdate = true, templateId = null)

            // THEN
            verify(orderRestClient).sendOrderPOSSpecificReceipt(site, orderId, email, true, null)
        }
    }

    @Test
    fun `given successful response for all orders when batch updating status then returns successful orders`() {
        runBlocking {
            // Given
            val site = SiteModel().apply { id = 1 }
            val orderIds = listOf(1L, 2L, 3L)
            val newStatus = COMPLETED.value

            // Create mocked OrderDto objects for success responses
            val order1 = mock<OrderDto>().apply {
                whenever(id).thenReturn(1L)
                whenever(status).thenReturn(COMPLETED.value)
            }
            val order2 = mock<OrderDto>().apply {
                whenever(id).thenReturn(2L)
                whenever(status).thenReturn(COMPLETED.value)
            }
            val order3 = mock<OrderDto>().apply {
                whenever(id).thenReturn(3L)
                whenever(status).thenReturn(COMPLETED.value)
            }

            val successResponses = listOf(
                BatchOrderApiResponse.OrderResponse.Success(order1),
                BatchOrderApiResponse.OrderResponse.Success(order2),
                BatchOrderApiResponse.OrderResponse.Success(order3)
            )

            whenever(orderRestClient.batchUpdateOrdersStatus(site, orderIds, newStatus))
                .thenReturn(BulkUpdateOrderStatusResponsePayload(successResponses))

            // When
            val result = orderStore.batchUpdateOrdersStatus(
                site,
                orderIds,
                WCOrderStatusModel(statusKey = COMPLETED.value)
            )

            // Then
            assertThat(result.isError).isFalse()
            result.model?.let { updateResult ->
                assertEquals(orderIds, updateResult.updatedOrders)
                assertTrue(updateResult.failedOrders.isEmpty())
            }
        }
    }

    @Test
    fun `given mixed response when batch updating status then returns successful and failed orders`() {
        runBlocking {
            // Given
            val site = SiteModel().apply { id = 1 }
            val orderIds = listOf(1L, 2L, 3L)
            val newStatus = COMPLETED.value

            // Mock successful orders
            val order1 = mock<OrderDto>().apply {
                whenever(id).thenReturn(1L)
                whenever(status).thenReturn(COMPLETED.value)
            }
            val order3 = mock<OrderDto>().apply {
                whenever(id).thenReturn(3L)
                whenever(status).thenReturn(COMPLETED.value)
            }

            val mixedResponses = listOf(
                BatchOrderApiResponse.OrderResponse.Success(order1),
                BatchOrderApiResponse.OrderResponse.Error(
                    id = 2L,
                    error = BatchOrderApiResponse.ErrorResponse(
                        code = "woocommerce_rest_shop_order_invalid_id",
                        message = "Invalid ID.",
                        data = BatchOrderApiResponse.ErrorData(status = 400)
                    )
                ),
                BatchOrderApiResponse.OrderResponse.Success(order3)
            )

            whenever(orderRestClient.batchUpdateOrdersStatus(site, orderIds, newStatus))
                .thenReturn(BulkUpdateOrderStatusResponsePayload(mixedResponses))

            // When
            val result = orderStore.batchUpdateOrdersStatus(
                site,
                orderIds,
                WCOrderStatusModel(statusKey = COMPLETED.value)
            )

            // Then
            assertThat(result.isError).isFalse()
            result.model?.let { updateResult ->
                assertEquals(listOf(1L, 3L), updateResult.updatedOrders)
                assertEquals(1, updateResult.failedOrders.size)
                with(updateResult.failedOrders[0]) {
                    assertEquals(2L, id)
                    assertEquals("woocommerce_rest_shop_order_invalid_id", errorCode)
                    assertEquals("Invalid ID.", errorMessage)
                    assertEquals(400, errorStatus)
                }
            }
        }
    }

    /* HELPER */

    @Suppress("LongParameterList")
    private fun generateSampleOrder(
        orderId: Long,
        orderStatus: String = CoreOrderStatus.PROCESSING.value,
        siteId: Int = 6,
        modified: String = "1955-11-05T14:15:00Z",
        paymentMethod: String = "",
        paymentMethodTitle: String = ""
    ): OrderEntity {
        return OrderEntity(
            orderId = orderId,
            localSiteId = LocalId(siteId),
            status = orderStatus,
            dateModified = modified,
            dateCreated = "1955-11-05T14:15:00Z",
            datePaid = "1956-11-05T14:15:00Z",
            currency = "USD",
            total = "10.0",
            paymentMethod = paymentMethod,
            paymentMethodTitle = paymentMethodTitle,
        )
    }

    private fun generateSampleOrderSummary(
        id: Number,
        remoteId: Number,
        modified: String = "1955-11-05T14:15:00Z"
    ): WCOrderSummaryModel {
        return WCOrderSummaryModel(
            siteId = LocalId(id.toInt()),
            orderId = RemoteId(remoteId.toLong()),
            dateCreated = "1955-11-05T14:15:00Z",
        ).apply {
            dateModified = modified
        }
    }

    private fun getOrderNotesFromJsonString(json: String, localSiteId: Int, orderId: Long): List<OrderNoteEntity> {
        val responseType = object : TypeToken<List<OrderNoteApiResponse>>() {}.type
        val converted = Gson().fromJson(json, responseType) as? List<OrderNoteApiResponse> ?: emptyList()
        return converted.map {
            it.toDataModel(localSiteId = LocalId(localSiteId), orderId = RemoteId(orderId))
        }
    }

    private fun setupMissingOrders(): MutableMap<WCOrderSummaryModel, OrderEntity?> {
        return mutableMapOf<WCOrderSummaryModel, OrderEntity?>().apply {
            (21L..30L).forEach { index ->
                put(
                    generateSampleOrderSummary(
                        id = index,
                        remoteId = index
                    ),
                    null
                )
            }
        }
    }

    private fun setupOutdatedOrders(site: SiteModel) =
        mutableMapOf<WCOrderSummaryModel, OrderEntity>().apply {
            val baselineDate = "2021-01-05T12:00:00Z"
            val oneDayAfterBaselineDate = "2021-01-06T12:00:00Z"
            (11L..20L).forEach { index ->
                put(
                    generateSampleOrderSummary(
                        id = index,
                        remoteId = index,
                        modified = oneDayAfterBaselineDate
                    ),
                    generateSampleOrder(
                        siteId = site.id,
                        orderId = index,
                        modified = baselineDate
                    )
                )
            }
        }

    private fun setupUpToDateOrders(site: SiteModel) =
        mutableMapOf<WCOrderSummaryModel, OrderEntity>().apply {
            val baselineDate = "2021-01-05T12:00:00Z"
            (1L..10L).forEach { index ->
                put(
                    generateSampleOrderSummary(
                        id = index,
                        remoteId = index,
                        modified = baselineDate
                    ),
                    generateSampleOrder(
                        siteId = site.id,
                        orderId = index,
                        modified = baselineDate
                    )
                )
            }
        }

    private val Map<WCOrderSummaryModel, OrderEntity?>.summaries
        get() = keys.toList()

    private val Map<WCOrderSummaryModel, OrderEntity?>.orders
        get() = values.toList()
}
