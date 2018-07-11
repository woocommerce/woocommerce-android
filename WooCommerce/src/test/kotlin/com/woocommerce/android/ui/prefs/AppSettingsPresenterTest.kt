package com.woocommerce.android.ui.prefs

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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsPresenterTest {
    private val appSettingsContractView: AppSettingsContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()

    private lateinit var appSettingsPresenter: AppSettingsPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        appSettingsPresenter = spy(AppSettingsPresenter(dispatcher, accountStore))
        appSettingsPresenter.takeView(appSettingsContractView)

        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Prompts UI to show login screen on logout`() {
        appSettingsPresenter.logout()

        // Logging out needs to trigger both an account signout and stored WordPress.com site removal
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        assertEquals(AccountAction.SIGN_OUT, actionCaptor.firstValue.type)
        assertEquals(SiteAction.REMOVE_WPCOM_AND_JETPACK_SITES, actionCaptor.secondValue.type)

        // Simulate access token cleared, and the resulting OnAuthenticationChanged
        doReturn(false).whenever(accountStore).hasAccessToken()
        appSettingsPresenter.onAuthenticationChanged(OnAuthenticationChanged())

        verify(mainContractView).showLoginScreen()
    }
}
