package com.woocommerce.android.push

import com.nhaarman.mockitokotlin2.*
import com.woocommerce.android.util.PreferencesWrapper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore

@RunWith(RobolectricTestRunner::class)
class NotificationRegistrationHandlerTest {
    private lateinit var notificationRegistrationHandler: NotificationRegistrationHandler

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val preferencesWrapper: PreferencesWrapper = mock()
    private val actionCaptor: KArgumentCaptor<Action<*>> = argumentCaptor()

    @Before
    fun setUp() {
        notificationRegistrationHandler = NotificationRegistrationHandler(
            dispatcher = dispatcher,
            accountStore = accountStore,
            notificationStore = mock(),
            preferencesWrapper = preferencesWrapper,
            selectedSite = mock()
        )
    }

    @Test
    fun `do not register new fcm token if user is not logged in`() {
        doReturn(false).whenever(accountStore).hasAccessToken()

        val fcmToken = "123456"
        notificationRegistrationHandler.onNewFCMTokenReceived(token = fcmToken)

        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(preferencesWrapper, never()).setFCMToken(any())
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `new fcm token is registered successfully only if user is logged in`() {
        doReturn(true).whenever(accountStore).hasAccessToken()

        val fcmToken = "123456"
        notificationRegistrationHandler.onNewFCMTokenReceived(token = fcmToken)

        verify(preferencesWrapper, atLeastOnce()).setFCMToken(any())
        verify(dispatcher, atLeastOnce()).dispatch(any())
    }
}
