package com.woocommerce.android.ui.main

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MainPresenterTest {
    private val mainContractView: MainContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val errorHandler: MainErrorHandler = mock()

    private lateinit var mainPresenter: MainPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        mainPresenter = spy(MainPresenter(dispatcher, accountStore, siteStore, wooCommerceStore, errorHandler))
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
    fun `Triggers a selected site update after site info fetch`() {
        // Magic link login requires the presenter to fetch account and site info
        // Check that the final OnSiteChanged triggers a site update
        mainPresenter.onSiteChanged(OnSiteChanged(6))
        verify(mainContractView).updateSelectedSite()
    }

    @Test
    fun `Prompts UI to show login screen on logout`() {
        mainPresenter.logout()

        // Logging out needs to trigger both an account signout and stored WordPress.com site removal
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        assertEquals(AccountAction.SIGN_OUT, actionCaptor.firstValue.type)
        assertEquals(SiteAction.REMOVE_WPCOM_AND_JETPACK_SITES, actionCaptor.secondValue.type)

        // Simulate access token cleared, and the resulting OnAuthenticationChanged
        doReturn(false).whenever(accountStore).hasAccessToken()
        mainPresenter.onAuthenticationChanged(OnAuthenticationChanged())

        verify(mainContractView).showLoginScreen()
    }
}
