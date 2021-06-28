package com.woocommerce.android.ui.prefs

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.util.CoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.action.SiteAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.NotificationStore.OnDeviceUnregistered
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AppSettingsPresenterTest {
    @Rule @JvmField
    val coroutinesTestRule = CoroutineTestRule()

    private val appSettingsContractView: AppSettingsContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountStore: AccountStore = mock()
    private val cardReaderManager: CardReaderManager = mock()

    private lateinit var appSettingsPresenter: AppSettingsPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        appSettingsPresenter = AppSettingsPresenter(dispatcher, accountStore, cardReaderManager, mock())
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

    @Test
    fun `cleanPaymentsData with initialized manager should disconnect reader`() {
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            whenever(cardReaderManager.isInitialized).thenReturn(true)

            // WHEN
            appSettingsPresenter.clearCardReaderData()

            // THEN
            verify(cardReaderManager).clearCachedCredentials()
            verify(cardReaderManager).disconnectReader()
        }
    }

    @Test
    fun `cleanPaymentsData with not initialized manager should not disconnect reader`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            // GIVEN
            whenever(cardReaderManager.isInitialized).thenReturn(false)

            // WHEN
            appSettingsPresenter.clearCardReaderData()

            // THEN
            verify(cardReaderManager, never()).clearCachedCredentials()
            verify(cardReaderManager, never()).disconnectReader()
        }
}
