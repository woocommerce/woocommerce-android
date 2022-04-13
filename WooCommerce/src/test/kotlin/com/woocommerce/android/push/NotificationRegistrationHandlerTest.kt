package com.woocommerce.android.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.AccountStore

class NotificationRegistrationHandlerTest {
    private lateinit var notificationRegistrationHandler: NotificationRegistrationHandler

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    @Before
    fun setUp() {
        notificationRegistrationHandler = NotificationRegistrationHandler(
            dispatcher = dispatcher,
            accountStore = accountStore,
            notificationStore = mock(),
            appPrefsWrapper = appPrefsWrapper,
            selectedSite = selectedSite
        )
    }

    @Test
    fun `do not register new fcm token if user is not logged in`() {
        doReturn(false).whenever(accountStore).hasAccessToken()

        val fcmToken = "123456"
        notificationRegistrationHandler.onNewFCMTokenReceived(token = fcmToken)

        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(appPrefsWrapper, never()).setFCMToken(any())
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `do not register new fcm token if site does not exist`() {
        doReturn(true).whenever(accountStore).hasAccessToken()
        doReturn(false).whenever(selectedSite).exists()

        val fcmToken = "123456"
        notificationRegistrationHandler.onNewFCMTokenReceived(token = fcmToken)

        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(selectedSite, atLeastOnce()).exists()
        verify(appPrefsWrapper, never()).setFCMToken(any())
        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `new fcm token is registered successfully only if user is logged in`() {
        doReturn(true).whenever(accountStore).hasAccessToken()
        doReturn(true).whenever(selectedSite).exists()

        val fcmToken = "123456"
        notificationRegistrationHandler.onNewFCMTokenReceived(token = fcmToken)

        verify(appPrefsWrapper, atLeastOnce()).setFCMToken(any())
        verify(dispatcher, atLeastOnce()).dispatch(any())
    }
}
