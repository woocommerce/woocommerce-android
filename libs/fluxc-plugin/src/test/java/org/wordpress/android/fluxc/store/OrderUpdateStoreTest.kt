package org.wordpress.android.fluxc.store

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.OrderEntity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.order.FeeLineTaxStatus
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Billing
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderDto.Shipping
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderRestClient
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.dao.MetaDataDao
import org.wordpress.android.fluxc.persistence.dao.OrdersDaoDecorator
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.EMPTY_BILLING_EMAIL
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.INVALID_PARAM
import org.wordpress.android.fluxc.store.WCOrderStore.RemoteOrderPayload
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.OptimisticUpdateResult
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult
import org.wordpress.android.fluxc.tools.CoroutineEngine
import kotlin.coroutines.EmptyCoroutineContext

@ExperimentalCoroutinesApi
class OrderUpdateStoreTest {
    private lateinit var sut: OrderUpdateStore
    private lateinit var orderRestClient: OrderRestClient

    private val siteSqlUtils: SiteSqlUtils = mock {
        on { getSiteWithLocalId(any()) } doReturn site
    }
    private val orderSqlUtils: OrderSqlUtils = mock()
    private val ordersDaoDecorator: OrdersDaoDecorator = mock {
        onBlocking { getOrder(TEST_REMOTE_ORDER_ID, TEST_LOCAL_SITE_ID) } doReturn initialOrder
    }
    private val metaDataDao: MetaDataDao = mock()

    fun setUp(setMocks: suspend () -> Unit) = runBlocking {
        setMocks.invoke()
        sut = OrderUpdateStore(
                coroutineEngine = CoroutineEngine(
                        EmptyCoroutineContext,
                        mock()
                ),
                wcOrderRestClient = orderRestClient,
                ordersDaoDecorator = ordersDaoDecorator,
                siteSqlUtils = siteSqlUtils,
                orderSqlUtils = orderSqlUtils,
                metaDataDao = metaDataDao
        )
    }

//    Updating customer notes

    @Test
    fun `should optimistically update order customer notes`(): Unit = runBlocking {
        // given
        val updatedOrder = initialOrder.copy(
                customerNote = UPDATED_CUSTOMER_NOTE
        )

        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, UPDATED_CUSTOMER_NOTE) }.doReturn(
                        RemoteOrderPayload.Updating(
                                updatedOrder,
                                site
                        )
                )
            }
            whenever(ordersDaoDecorator.getOrder(TEST_REMOTE_ORDER_ID, TEST_LOCAL_SITE_ID)).thenReturn(
                    initialOrder
            )
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderId = TEST_REMOTE_ORDER_ID,
                site = site,
                newCustomerNote = UPDATED_CUSTOMER_NOTE
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged()),
                RemoteUpdateResult(OnOrderChanged())
        )
        verify(ordersDaoDecorator).insertOrUpdateOrder(argThat {
            customerNote == UPDATED_CUSTOMER_NOTE
        }, any())
    }

    @Test
    fun `should revert local customer notes update if remote update failed`(): Unit = runBlocking {
        // given
        val specificOrderError = "order error"
        setUp {
            orderRestClient = mock {
                onBlocking { updateCustomerOrderNote(initialOrder, site, UPDATED_CUSTOMER_NOTE) }.doReturn(
                        RemoteOrderPayload.Updating(
                                error = OrderError(message = specificOrderError),
                                initialOrder,
                                site
                        )
                )
            }
            whenever(ordersDaoDecorator.getOrder(TEST_REMOTE_ORDER_ID, TEST_LOCAL_SITE_ID)).thenReturn(
                    initialOrder
            )
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderId = initialOrder.orderId,
                site = site,
                newCustomerNote = UPDATED_CUSTOMER_NOTE
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged()),
                RemoteUpdateResult(
                        OnOrderChanged(
                                orderError = OrderError(message = specificOrderError)
                        )
                )
        )

        verify(ordersDaoDecorator).insertOrUpdateOrder(argThat {
            customerNote == INITIAL_CUSTOMER_NOTE
        }, any())
    }

    @Test
    fun `should emit optimistic update failure if order not found when updating status`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(ordersDaoDecorator.getOrder(any(), any())).thenReturn(null)
        }

        // when
        val results = sut.updateCustomerOrderNote(
                orderId = initialOrder.orderId,
                site = site,
                newCustomerNote = UPDATED_CUSTOMER_NOTE
        ).toList()

        // then
        assertThat(results).containsExactly(
                OptimisticUpdateResult(
                        event = OnOrderChanged(
                                orderError = OrderError(
                                        message = "Order with id ${initialOrder.orderId} not found"
                                )
                        )
                )
        )

        verifyNoInteractions(orderRestClient)
    }

    //    Updating addresses
    @Test
    fun `should optimistically update shipping and billing addresses`(): Unit = runBlocking {
        // given
        val updatedOrder = initialOrder.copy(
                shippingFirstName = UPDATED_SHIPPING_FIRST_NAME,
                billingFirstName = UPDATED_BILLING_FIRST_NAME
        )

        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateBothOrderAddresses(
                            initialOrder,
                            site,
                            emptyShippingDto.copy(first_name = UPDATED_SHIPPING_FIRST_NAME),
                            emptyBillingDto.copy(first_name = UPDATED_BILLING_FIRST_NAME)
                    )
                } doReturn (RemoteOrderPayload.Updating(updatedOrder, site))
            }
        }

        // when
        val results = sut.updateBothOrderAddresses(
                orderId = initialOrder.orderId,
                localSiteId = site.localId(),
                shippingAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME),
                billingAddress = emptyBilling.copy(firstName = UPDATED_BILLING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged()),
                RemoteUpdateResult(OnOrderChanged())
        )
        verify(ordersDaoDecorator).insertOrUpdateOrder(argThat {
            shippingFirstName == UPDATED_SHIPPING_FIRST_NAME &&
                    billingFirstName == UPDATED_BILLING_FIRST_NAME
        }, any())
    }

    @Test
    fun `should optimistically update shipping address`(): Unit = runBlocking {
        // given
        val updatedOrder = initialOrder.copy(
                shippingFirstName = UPDATED_SHIPPING_FIRST_NAME
        )

        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateShippingAddress(
                            initialOrder,
                            site,
                            emptyShippingDto.copy(first_name = UPDATED_SHIPPING_FIRST_NAME)
                    )
                } doReturn (RemoteOrderPayload.Updating(updatedOrder, site))
            }
        }

        // when
        val results = sut.updateOrderAddress(
                orderId = initialOrder.orderId,
                localSiteId = site.localId(),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged()),
                RemoteUpdateResult(OnOrderChanged())
        )
        verify(ordersDaoDecorator).insertOrUpdateOrder(argThat {
            shippingFirstName == UPDATED_SHIPPING_FIRST_NAME
        }, any())
    }

    @Test
    fun `should revert local shipping address update if remote update failed`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateShippingAddress(
                            initialOrder, site, emptyShippingDto.copy(first_name = UPDATED_SHIPPING_FIRST_NAME)
                    )
                }.doReturn(
                        RemoteOrderPayload.Updating(
                                error = OrderError(),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderAddress(
                initialOrder.orderId,
                site.localId(),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged()),
                RemoteUpdateResult(
                        OnOrderChanged(
                                orderError = OrderError(type = GENERIC_ERROR)
                        )
                )
        )

        verify(ordersDaoDecorator).insertOrUpdateOrder(argThat {
            shippingFirstName == INITIAL_SHIPPING_FIRST_NAME
        }, any())
    }

    @Test
    fun `should emit optimistic update failure if order not found on updating shipping address`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(ordersDaoDecorator.getOrder(any(), any())).thenReturn(null)
        }

        // when
        val results = sut.updateOrderAddress(
                initialOrder.orderId,
                site.localId(),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).containsExactly(
                OptimisticUpdateResult(
                        OnOrderChanged(
                                orderError = OrderError(
                                        message = "Order with id ${initialOrder.orderId} not found"
                                )
                        )
                )
        )
        verifyNoInteractions(orderRestClient)
    }

    @Test
    fun `should emit optimistic update failure if site not found on updating shipping address`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock()
            whenever(siteSqlUtils.getSiteWithLocalId(any())).thenReturn(null)
        }

        // when
        val results = sut.updateOrderAddress(
                initialOrder.orderId,
                site.localId(),
                newAddress = emptyShipping.copy(firstName = UPDATED_SHIPPING_FIRST_NAME)
        ).toList()

        // then
        assertThat(results).containsExactly(
                OptimisticUpdateResult(
                        event = OnOrderChanged(
                                orderError = OrderError(
                                        message = "Site with local id ${initialOrder.localSiteId} not found"
                                )
                        )
                )
        )

        verifyNoInteractions(orderRestClient)
    }

    @Test
    fun `should emit empty billing email address if its likely that that's the error`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateBillingAddress(
                            initialOrder, site, emptyBillingDto
                    )
                }.doReturn(
                        RemoteOrderPayload.Updating(
                                error = OrderError(type = INVALID_PARAM),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderAddress(
                initialOrder.orderId,
                site.localId(),
                newAddress = emptyBilling
        ).toList()

        // then
        assertThat(results[1].event.error.type).isEqualTo(EMPTY_BILLING_EMAIL)
    }

    @Test
    fun `should not emit empty billing email address if its not likely that that's the error`(): Unit = runBlocking {
        // given
        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateBillingAddress(
                            initialOrder, site, emptyBillingDto.copy(email = "custom@mail.com")
                    )
                }.doReturn(
                        RemoteOrderPayload.Updating(
                                error = OrderError(type = GENERIC_ERROR),
                                initialOrder,
                                site
                        )
                )
            }
        }

        // when
        val results = sut.updateOrderAddress(
                initialOrder.orderId,
                site.localId(),
                newAddress = emptyBilling.copy(email = "custom@mail.com")
        ).toList()

        // then
        assertThat(results[1].event.error.type).isEqualTo(GENERIC_ERROR)
    }

//      Simple payments

    @Test
    fun `should create simple payment with correct amount, tax status, and order status`(): Unit = runBlocking {
        // given
        val newOrder = initialOrder.copy(
                status = SIMPLE_PAYMENT_ORDER_STATUS.statusKey,
                customerNote = SIMPLE_PAYMENT_CUSTOMER_NOTE,
                feeLines = OrderUpdateStore.generateSimplePaymentFeeLineJson(
                        SIMPLE_PAYMENT_AMOUNT,
                        SIMPLE_PAYMENT_IS_TAXABLE,
                        SIMPLE_PAYMENT_FEE_ID
                ).toString()
        )

        setUp {
            orderRestClient = mock {
                onBlocking {
                    createOrder(any(), any(), anyOrNull())
                }.doReturn(
                    WooPayload(newOrder)
                )
            }
        }

        // when
        val result = sut.createSimplePayment(
            site,
            SIMPLE_PAYMENT_AMOUNT,
            SIMPLE_PAYMENT_IS_TAXABLE,
            SIMPLE_PAYMENT_ORDER_STATUS,
            SIMPLE_PAYMENT_CUSTOMER_NOTE,
        )

        // then
        assertThat(result.isError).isFalse()
        assertThat(result.model).isNotNull
        assertThat(result.model!!.getFeeLineList()).hasSize(1)
        assertThat(result.model!!.getFeeLineList()[0].total).isEqualTo(SIMPLE_PAYMENT_AMOUNT)
        assertThat(result.model!!.getFeeLineList()[0].taxStatus!!.value).isEqualTo(SIMPLE_PAYMENT_TAX_STATUS)
        assertThat(result.model!!.status).isEqualTo(SIMPLE_PAYMENT_ORDER_STATUS.statusKey)
        assertThat(result.model!!.customerNote).isEqualTo(SIMPLE_PAYMENT_CUSTOMER_NOTE)
    }

    @Test
    fun `should optimistically update simple payment`(): Unit = runBlocking {
        // given
        val updatedOrder = initialOrder.copy(
                customerNote = SIMPLE_PAYMENT_CUSTOMER_NOTE,
                billingEmail = SIMPLE_PAYMENT_BILLING_EMAIL,
                feeLines = OrderUpdateStore.generateSimplePaymentFeeLineJson(
                        SIMPLE_PAYMENT_AMOUNT,
                        SIMPLE_PAYMENT_IS_TAXABLE,
                        SIMPLE_PAYMENT_FEE_ID
                ).toString()
        )

        setUp {
            orderRestClient = mock {
                onBlocking {
                    updateOrder(any(), any(), any())
                }.doReturn(
                    WooPayload(updatedOrder)
                )
            }
            whenever(ordersDaoDecorator.getOrder(TEST_REMOTE_ORDER_ID, TEST_LOCAL_SITE_ID)).thenReturn(
                    updatedOrder
            )
        }

        // when
        val results = sut.updateSimplePayment(
                site = site,
                orderId = TEST_REMOTE_ORDER_ID,
                amount = SIMPLE_PAYMENT_AMOUNT,
                customerNote = SIMPLE_PAYMENT_CUSTOMER_NOTE,
                billingEmail = SIMPLE_PAYMENT_BILLING_EMAIL,
                isTaxable = SIMPLE_PAYMENT_IS_TAXABLE
        ).toList()

        // then
        assertThat(results).hasSize(2).containsExactly(
                OptimisticUpdateResult(OnOrderChanged()),
                RemoteUpdateResult(OnOrderChanged())
        )
        ordersDaoDecorator.getOrder(TEST_REMOTE_ORDER_ID, TEST_LOCAL_SITE_ID)?.let { order ->
            assertThat(order.billingEmail).isEqualTo(SIMPLE_PAYMENT_BILLING_EMAIL)
            assertThat(order.customerNote).isEqualTo(SIMPLE_PAYMENT_CUSTOMER_NOTE)
            assertThat(order.getFeeLineList()).hasSize(1)
            assertThat(order.getFeeLineList()[0].total).isEqualTo(SIMPLE_PAYMENT_AMOUNT)
        }
    }

    @Test
    fun `should delete local copy of order when delete request succeeds`(): Unit = runBlocking {
        setUp {
            orderRestClient = mock {
                onBlocking {
                    deleteOrder(any(), any(), any())
                }.doReturn(
                    WooPayload(Unit)
                )
            }
        }

        sut.deleteOrder(
            site = site,
            orderId = TEST_REMOTE_ORDER_ID
        )

        verify(ordersDaoDecorator).deleteOrder(site.localId(), TEST_REMOTE_ORDER_ID)
        verify(orderSqlUtils).deleteOrderSummaryById(site, TEST_REMOTE_ORDER_ID)
        verify(metaDataDao).deleteMetaData(site.localId(), TEST_REMOTE_ORDER_ID)
    }

    private companion object {
        const val TEST_REMOTE_ORDER_ID = 321L
        val TEST_LOCAL_SITE_ID = LocalId(654)
        const val INITIAL_CUSTOMER_NOTE = "original customer note"
        const val UPDATED_CUSTOMER_NOTE = "updated customer note"
        const val INITIAL_SHIPPING_FIRST_NAME = "original shipping first name"
        const val UPDATED_SHIPPING_FIRST_NAME = "updated shipping first name"
        const val UPDATED_BILLING_FIRST_NAME = "updated billing first name"

        const val SIMPLE_PAYMENT_FEE_ID = 1L
        const val SIMPLE_PAYMENT_AMOUNT = "10.00"
        const val SIMPLE_PAYMENT_CUSTOMER_NOTE = "Simple payment customer note"
        const val SIMPLE_PAYMENT_BILLING_EMAIL = "example@example.com"
        const val SIMPLE_PAYMENT_IS_TAXABLE = true
        val SIMPLE_PAYMENT_ORDER_STATUS = WCOrderStatusModel(CoreOrderStatus.PROCESSING.value)
        val SIMPLE_PAYMENT_TAX_STATUS = FeeLineTaxStatus.Taxable.value

        val initialOrder = OrderEntity(
                orderId = TEST_REMOTE_ORDER_ID,
                localSiteId = TEST_LOCAL_SITE_ID,
                customerNote = INITIAL_CUSTOMER_NOTE,
                shippingFirstName = INITIAL_SHIPPING_FIRST_NAME
        )

        val site = SiteModel().apply {
            id = TEST_LOCAL_SITE_ID.value
        }

        val emptyShipping = OrderAddress.Shipping("", "", "", "", "", "", "", "", "", "")
        val emptyBilling = OrderAddress.Billing("", "", "", "", "", "", "", "", "", "", "")
        val emptyShippingDto = Shipping("", "", "", "", "", "", "", "", "", "")
        val emptyBillingDto = Billing("", "", "", "", "", "", "", "", "", null, "")
    }
}
