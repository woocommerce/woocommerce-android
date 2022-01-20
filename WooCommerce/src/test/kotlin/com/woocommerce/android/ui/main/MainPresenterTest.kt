package com.woocommerce.android.ui.main

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.*
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MainPresenterTest : BaseUnitTest() {
    private val mainContractView: MainContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val notificationStore: NotificationStore = mock()
    private val selectedSite: SelectedSite = mock {
        val siteModel = SiteModel()
        on { get() } doReturn siteModel
        on { getIfExists() } doReturn siteModel
        on { exists() } doReturn true
    }
    private val productImageMap: ProductImageMap = mock()
    private val appPrefs: AppPrefs = mock()

    private val wcOrderStore: WCOrderStore = mock {
        on { observeOrdersForSite(any(), any()) } doReturn emptyFlow()
    }

    private lateinit var mainPresenter: MainPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        mainPresenter = spy(
            MainPresenter(
                dispatcher,
                accountStore,
                siteStore,
                wooCommerceStore,
                notificationStore,
                selectedSite,
                productImageMap,
                appPrefs,
                wcOrderStore
            )
        )
        mainPresenter.takeView(mainContractView)
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Reports AccountStore token status correctly`() {
        assertFalse(mainPresenter.userIsLoggedIn())

        doReturn(true).whenever(accountStore).hasAccessToken()
        assertTrue(mainPresenter.userIsLoggedIn())
    }

    @Test
    fun `Handles token from magic link correctly`() {
        // Storing a token with the presenter should trigger a dispatch
        mainPresenter.storeMagicLinkToken("a-token")
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(AccountAction.UPDATE_ACCESS_TOKEN, actionCaptor.firstValue.type)

        // Pretend the token has been stored
        doReturn(true).whenever(accountStore).hasAccessToken()

        // Check that the resulting OnAuthenticationChanged ends up notifying the View
        mainPresenter.onAuthenticationChanged(OnAuthenticationChanged())
        verify(mainContractView).notifyTokenUpdated()
    }

    @Test
    fun `Triggers a selected site update after site info fetch`() = testBlocking {
        whenever(wooCommerceStore.fetchWooCommerceSites()).thenReturn(WooResult())

        // Magic link login requires the presenter to fetch account and site info
        // Trigger the beginning of magic link flow to put the presenter in 'magic link' mode
        mainPresenter.storeMagicLinkToken("a-token")

        // Check that the final OnSiteChanged triggers a site update
        mainPresenter.onAccountChanged(
            OnAccountChanged().apply {
                causeOfChange = AccountAction.FETCH_SETTINGS
            }
        )
        verify(mainContractView).updateSelectedSite()
    }

    @Test
    fun `Requests orders to fulfill count correctly`() {
        mainPresenter.takeView(mainContractView)
        mainPresenter.fetchUnfilledOrderCount()

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_ORDERS_COUNT, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchOrdersCountPayload
        assertEquals(CoreOrderStatus.PROCESSING.value, payload.statusFilter)
    }

    @Test
    fun `Displays unfilled orders count correctly`() {
        val totalOrders = 25
        whenever(wcOrderStore.observeOrdersForSite(any(), eq(listOf(CoreOrderStatus.PROCESSING.value)))).doReturn(
            flowOf(generateFakeOrders(totalOrders))
        )

        mainPresenter.takeView(mainContractView)

        verify(mainContractView).showOrderBadge(totalOrders)
    }

    @Test
    fun `Hides orders badge correctly`() {
        whenever(wcOrderStore.observeOrdersForSite(any(), eq(listOf(CoreOrderStatus.PROCESSING.value)))).doReturn(
            flowOf(emptyList())
        )

        mainPresenter.takeView(mainContractView)

        verify(mainContractView).hideOrderBadge()
    }

    @Test
    fun `Hides orders badge on error correctly`() {
        mainPresenter.takeView(mainContractView)

        mainPresenter.onOrderChanged(
            OnOrderChanged(
                statusFilter = CoreOrderStatus.PROCESSING.value,
                causeOfChange = FETCH_ORDERS_COUNT,
                orderError = WCOrderStore.OrderError(),
            )
        )

        verify(mainContractView).hideOrderBadge()
    }

    @Test
    fun `Updates orders badge on new unfilled orders`() = runBlocking {
        val initialOrderCount = 25
        val postUpdateOrderCount = 30
        val fakeObserveResult = MutableSharedFlow<List<WCOrderModel>>()
        whenever(wcOrderStore.observeOrdersForSite(any(), eq(listOf(CoreOrderStatus.PROCESSING.value)))).doReturn(
            fakeObserveResult
        )
        mainPresenter.takeView(mainContractView)

        fakeObserveResult.emit(generateFakeOrders(initialOrderCount))
        verify(mainContractView).showOrderBadge(initialOrderCount)

        fakeObserveResult.emit(generateFakeOrders(postUpdateOrderCount))
        verify(mainContractView).showOrderBadge(postUpdateOrderCount)

        fakeObserveResult.emit(emptyList())
        verify(mainContractView).hideOrderBadge()
    }

    @Test
    fun `Handles database downgrade correctly`() = testBlocking {
        if (FeatureFlag.DB_DOWNGRADE.isEnabled()) {
            whenever(wooCommerceStore.fetchWooCommerceSites()).thenReturn(WooResult())
            mainPresenter.takeView(mainContractView)
            mainPresenter.fetchSitesAfterDowngrade()
            verify(mainContractView).showProgressDialog(any())
            verify(mainContractView).updateSelectedSite()
        }
    }

    private fun generateFakeOrders(size: Int): List<WCOrderModel> {
        return mutableListOf<WCOrderModel>().also { list ->
            repeat(size) { repeatCount ->
                list.add(OrderTestUtils.generateOrder())
            }
        }
    }
}
