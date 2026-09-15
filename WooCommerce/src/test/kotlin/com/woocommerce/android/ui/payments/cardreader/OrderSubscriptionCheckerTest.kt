package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.model.Subscription
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.common.subscription.SubscriptionRepository
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.payments.cardreader.payment.OrderSubscriptionChecker
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.lenient
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.store.WooCommerceStore.WooPlugin.WOO_SUBSCRIPTIONS
import org.wordpress.android.fluxc.wp.site.SitePluginFixtures.createTestSitePlugin

@OptIn(ExperimentalCoroutinesApi::class)
class OrderSubscriptionCheckerTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val subscriptionRepository: SubscriptionRepository = mock()

    private val order = OrderTestUtils.generateTestOrder()

    private lateinit var checker: OrderSubscriptionChecker

    @Before
    fun setUp() {
        lenient().doReturn(createTestSitePlugin())
            .whenever(wooCommerceStore).getActiveSitePlugin(any(), eq(WOO_SUBSCRIPTIONS))
        checker = OrderSubscriptionChecker(selectedSite, wooCommerceStore, subscriptionRepository)
    }

    @Test
    fun `given order without products, when checking, then is free without hitting endpoint`() = testBlocking {
        val orderWithoutProducts = order.copy(items = emptyList())

        val result = checker.isOrderFreeOfSubscriptions(orderWithoutProducts)

        assertThat(result).isTrue()
        verify(subscriptionRepository, never()).fetchSubscriptionsByOrderId(any(), any())
    }

    @Test
    fun `given subscriptions plugin inactive, when checking, then is free without hitting endpoint`() = testBlocking {
        whenever(wooCommerceStore.getActiveSitePlugin(any(), eq(WOO_SUBSCRIPTIONS))).thenReturn(null)

        val result = checker.isOrderFreeOfSubscriptions(order)

        assertThat(result).isTrue()
        verify(subscriptionRepository, never()).fetchSubscriptionsByOrderId(any(), any())
    }

    @Test
    fun `given endpoint returns no subscriptions, when checking, then is free`() = testBlocking {
        whenever(subscriptionRepository.fetchSubscriptionsByOrderId(eq(order.id), any()))
            .thenReturn(WooResult(emptyList()))

        val result = checker.isOrderFreeOfSubscriptions(order)

        assertThat(result).isTrue()
    }

    @Test
    fun `given endpoint returns a subscription, when checking, then is not free`() = testBlocking {
        whenever(subscriptionRepository.fetchSubscriptionsByOrderId(eq(order.id), any()))
            .thenReturn(WooResult(listOf(mock<Subscription>())))

        val result = checker.isOrderFreeOfSubscriptions(order)

        assertThat(result).isFalse()
    }

    @Test
    fun `given endpoint errors, when checking, then fails closed`() = testBlocking {
        whenever(subscriptionRepository.fetchSubscriptionsByOrderId(eq(order.id), any()))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)))

        val result = checker.isOrderFreeOfSubscriptions(order)

        assertThat(result).isFalse()
    }

    @Test
    fun `given a successful lookup, when checking twice, then the endpoint is hit only once`() = testBlocking {
        whenever(subscriptionRepository.fetchSubscriptionsByOrderId(eq(order.id), any()))
            .thenReturn(WooResult(emptyList()))

        checker.isOrderFreeOfSubscriptions(order)
        checker.isOrderFreeOfSubscriptions(order)

        verify(subscriptionRepository, times(1)).fetchSubscriptionsByOrderId(eq(order.id), any())
    }

    @Test
    fun `given an errored lookup, when checking again, then the endpoint is retried`() = testBlocking {
        whenever(subscriptionRepository.fetchSubscriptionsByOrderId(eq(order.id), any()))
            .thenReturn(WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)))

        checker.isOrderFreeOfSubscriptions(order)
        checker.isOrderFreeOfSubscriptions(order)

        verify(subscriptionRepository, times(2)).fetchSubscriptionsByOrderId(eq(order.id), any())
    }
}
