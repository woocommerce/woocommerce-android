package com.woocommerce.android.ui.main

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore.*
import org.wordpress.android.fluxc.store.WooCommerceStore
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
    private val selectedSite: SelectedSite = mock()
    private val productImageMap: ProductImageMap = mock()
    private val appPrefs: AppPrefs = mock()

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
                appPrefs
            )
        )
        mainPresenter.takeView(mainContractView)
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(selectedSite).exists()
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
        val filter = CoreOrderStatus.PROCESSING.value
        mainPresenter.takeView(mainContractView)
        mainPresenter.fetchUnfilledOrderCount()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        mainPresenter.onOrderChanged(
            OnOrderChanged(totalOrders, filter).apply {
                causeOfChange = FETCH_ORDERS_COUNT
                canLoadMore = true
            }
        )

        verify(mainContractView).showOrderBadge(totalOrders)
    }

    @Test
    fun `Hides orders badge correctly`() {
        val totalOrders = 0
        val filter = CoreOrderStatus.PROCESSING.value
        mainPresenter.takeView(mainContractView)
        mainPresenter.fetchUnfilledOrderCount()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        mainPresenter.onOrderChanged(
            OnOrderChanged(totalOrders, filter).apply {
                causeOfChange = FETCH_ORDERS_COUNT
            }
        )

        verify(mainContractView).hideOrderBadge()
    }

    @Test
    fun `Hides orders badge on error correctly`() {
        val totalOrders = 25
        val filter = CoreOrderStatus.PROCESSING.value
        mainPresenter.takeView(mainContractView)
        mainPresenter.fetchUnfilledOrderCount()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        mainPresenter.onOrderChanged(
            OnOrderChanged(totalOrders, filter).apply {
                causeOfChange = FETCH_ORDERS_COUNT
                error = OrderError()
            }
        )

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
}
