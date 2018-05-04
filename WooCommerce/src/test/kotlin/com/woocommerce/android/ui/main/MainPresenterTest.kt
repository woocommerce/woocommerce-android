package com.woocommerce.android.ui.main

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.WooCommerceStore

class MainPresenterTest {
    private val mainContractView: MainContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val siteStore: SiteStore = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private lateinit var mainPresenter: MainPresenter

    @Before
    fun setup() {
        mainPresenter = spy(MainPresenter(dispatcher, accountStore, siteStore, wooCommerceStore))
        mainPresenter.takeView(mainContractView)
    }

    @Test
    fun `Reports AccountStore token status correctly`() {
        Assert.assertFalse(mainPresenter.userIsLoggedIn())

        doReturn(true).whenever(accountStore).hasAccessToken()
        Assert.assertTrue(mainPresenter.userIsLoggedIn())
    }

    @Test
    fun `Handles token from magic link correctly`() {
        // Storing a token with the presenter should trigger a dispatch
        mainPresenter.storeMagicLinkToken("a-token")
        verify(dispatcher, times(1)).dispatch(any<Action<UpdateTokenPayload>>())

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
}
