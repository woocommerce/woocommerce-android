package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.store.NotificationStore.OnDeviceUnregistered
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class AppSettingsPresenterTest : BaseUnitTest() {
    private val appSettingsContractView: AppSettingsContract.View = mock()

    private val dispatcher: Dispatcher = mock()
    private val accountRepository: AccountRepository = mock()
    private val clearCardReaderDataAction: ClearCardReaderDataAction = mock()

    private lateinit var appSettingsPresenter: AppSettingsPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        appSettingsPresenter = AppSettingsPresenter(
            dispatcher,
            accountRepository,
            mock(),
            clearCardReaderDataAction
        )
        appSettingsPresenter.takeView(appSettingsContractView)

        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Verifies that logging out from settings results in signing out and settings closing`() = testBlocking {
        whenever(accountRepository.logout()).thenReturn(true)

        appSettingsPresenter.logout()

        // Logging out should first trigger device unregistration for push notifications
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(NotificationAction.UNREGISTER_DEVICE, actionCaptor.firstValue.type)

        // Simulate device unregistered for push notifications
        appSettingsPresenter.onDeviceUnregistered(OnDeviceUnregistered())

        // Unregistration should trigger logout
        verify(accountRepository).logout()

        // Check UI
        verify(appSettingsContractView).finishLogout()
    }

    @Test
    fun `cleanPaymentsData with initialized manager should disconnect reader`() {
        testBlocking {
            appSettingsPresenter.logout()

            verify(clearCardReaderDataAction).invoke()
        }
    }
}
