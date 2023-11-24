package com.woocommerce.android.ui.main

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationError
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType.INVALID_TOKEN
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class MainPresenterTest : BaseUnitTest() {
    private val mainContractView: MainContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val productImageMap: ProductImageMap = mock()
    private val appPrefs: AppPrefsWrapper = mock()
    private val clearCardReaderDataAction: ClearCardReaderDataAction = mock()

    private val accountRepository: AccountRepository = mock()
    private val processingOrdersCount = MutableStateFlow<Int?>(null)
    private val observeProcessingOrdersCount: ObserveProcessingOrdersCount = mock {
        on { invoke() } doReturn processingOrdersCount
    }

    private lateinit var mainPresenter: MainPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        mainPresenter = spy(
            MainPresenter(
                dispatcher = dispatcher,
                wooCommerceStore = wooCommerceStore,
                productImageMap = productImageMap,
                appPrefsWrapper = appPrefs,
                clearCardReaderDataAction = clearCardReaderDataAction,
                accountRepository = accountRepository,
                tracks = mock(),
                observeProcessingOrdersCount = observeProcessingOrdersCount
            )
        )
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Reports AccountStore token status correctly`() {
        doReturn(false).whenever(accountRepository).isUserLoggedIn()
        assertFalse(mainPresenter.userIsLoggedIn())

        doReturn(true).whenever(accountRepository).isUserLoggedIn()
        assertTrue(mainPresenter.userIsLoggedIn())
    }

    @Test
    fun `Handles token from magic link correctly`() {
        mainPresenter.takeView(mainContractView)
        // Storing a token with the presenter should trigger a dispatch
        mainPresenter.storeMagicLinkToken("a-token")
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(AccountAction.UPDATE_ACCESS_TOKEN, actionCaptor.firstValue.type)

        // Pretend the token has been stored
        doReturn(true).whenever(accountRepository).isUserLoggedIn()

        // Check that the resulting OnAuthenticationChanged ends up notifying the View
        mainPresenter.onAuthenticationChanged(OnAuthenticationChanged())
        verify(mainContractView).notifyTokenUpdated()
    }

    @Test
    fun `Triggers a selected site update after site info fetch`() = testBlocking {
        whenever(wooCommerceStore.fetchWooCommerceSites()).thenReturn(WooResult())

        mainPresenter.takeView(mainContractView)

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
    fun `Handles database downgrade correctly`() = testBlocking {
        if (FeatureFlag.DB_DOWNGRADE.isEnabled()) {
            whenever(wooCommerceStore.fetchWooCommerceSites()).thenReturn(WooResult())
            mainPresenter.takeView(mainContractView)
            mainPresenter.fetchSitesAfterDowngrade()
            verify(mainContractView).showProgressDialog(any())
            verify(mainContractView).updateSelectedSite()
        }
    }

    @Test
    fun `when selected site changes, then card reader data is cleared`() = testBlocking {
        mainPresenter.selectedSiteChanged(site = SiteModel())

        verify(clearCardReaderDataAction).invoke()
    }

    @Test
    fun `when processing orders observer returns a positive value, then update the badge`() = testBlocking {
        processingOrdersCount.value = 1

        mainPresenter.takeView(mainContractView)

        verify(mainContractView).showOrderBadge(1)
    }

    @Test
    fun `when processing orders observer returns a null value, then hide the badge`() = testBlocking {
        processingOrdersCount.value = null

        mainPresenter.takeView(mainContractView)

        verify(mainContractView).hideOrderBadge()
    }

    @Test
    fun `Handles invalid token error correctly`() = testBlocking {
        mainPresenter.takeView(mainContractView)

        // Check that the resulting OnAuthenticationChanged ends up logging user out and restarting the View
        val event = OnAuthenticationChanged()
        event.error = AuthenticationError(INVALID_TOKEN, "Invalid token")
        mainPresenter.onAuthenticationChanged(event)

        verify(accountRepository).logout()
        verify(mainContractView).restart()
    }
}
