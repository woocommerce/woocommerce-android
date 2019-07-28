package com.woocommerce.android.ui.prefs

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.OnDeviceUnregistered
import kotlin.test.assertEquals

class AppSettingsPresenterTest {
    private val appSettingsContractView: AppSettingsContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val notificationStore: NotificationStore = mock()

    private lateinit var appSettingsPresenter: AppSettingsPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        appSettingsPresenter = spy(AppSettingsPresenter(dispatcher, accountStore, notificationStore))
        appSettingsPresenter.takeView(appSettingsContractView)

        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Verifies that logging out from settings results in signing out and settings closing`() {
        appSettingsPresenter.logout()

        // Logging out should first trigger device unregistration for push notifications
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(NotificationAction.UNREGISTER_DEVICE, actionCaptor.firstValue.type)

        // Simulate device unregistered for push notifications
        appSettingsPresenter.onDeviceUnregistered(OnDeviceUnregistered())

        // Unregistration should trigger both an account signout and stored WordPress.com site removal
        actionCaptor = argumentCaptor()
        verify(dispatcher, times(3)).dispatch(actionCaptor.capture())
        assertEquals(AccountAction.SIGN_OUT, actionCaptor.secondValue.type)
        assertEquals(SiteAction.REMOVE_WPCOM_AND_JETPACK_SITES, actionCaptor.thirdValue.type)

        // Simulate access token cleared, and the resulting OnAuthenticationChanged
        doReturn(false).whenever(accountStore).hasAccessToken()
        appSettingsPresenter.onAuthenticationChanged(OnAuthenticationChanged())

        verify(appSettingsContractView).finishLogout()
    }
}
