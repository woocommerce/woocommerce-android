package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.payments.cardreader.ClearCardReaderDataAction
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AppSettingsPresenterTest : BaseUnitTest(StandardTestDispatcher()) {
    private val appSettingsContractView: AppSettingsContract.View = mock()
    private val replacementView: AppSettingsContract.View = mock()

    private val accountRepository: AccountRepository = mock()
    private val clearCardReaderDataAction: ClearCardReaderDataAction = mock()

    private lateinit var appSettingsPresenter: AppSettingsPresenter

    @Before
    fun setup() {
        appSettingsPresenter = AppSettingsPresenter(
            accountRepository,
            mock(),
            clearCardReaderDataAction
        )
        appSettingsPresenter.takeView(appSettingsContractView)
    }

    @Test
    fun `given logout is requested, when presenter starts logout, then shows loading dialog`() = testBlocking {
        // GIVEN
        val logoutGate = CompletableDeferred<Unit>()
        whenever(accountRepository.logout()).doSuspendableAnswer {
            logoutGate.await()
            true
        }

        // WHEN
        appSettingsPresenter.logout()
        runCurrent()

        // THEN
        verify(appSettingsContractView).showLogoutProgressDialog()
        verify(appSettingsContractView, never()).finishLogout()

        logoutGate.complete(Unit)
        runCurrent()
    }

    @Test
    fun `given logout succeeds, when presenter logs out, then clears payments data and finishes logout`() = testBlocking {
        // GIVEN
        whenever(accountRepository.logout()).thenReturn(true)

        // WHEN
        appSettingsPresenter.logout()
        runCurrent()

        // THEN
        verify(appSettingsContractView).showLogoutProgressDialog()
        verify(clearCardReaderDataAction).invoke()
        verify(appSettingsContractView).finishLogout()
        verify(appSettingsContractView, never()).hideLogoutProgressDialog()
    }

    @Test
    fun `given logout throws, when presenter logs out, then hides loading dialog and stays on screen`() = testBlocking {
        // GIVEN
        whenever(accountRepository.logout()).doSuspendableAnswer {
            throw IllegalStateException("boom")
        }

        // WHEN
        appSettingsPresenter.logout()
        runCurrent()

        // THEN
        verify(appSettingsContractView).showLogoutProgressDialog()
        verify(appSettingsContractView).hideLogoutProgressDialog()
        verify(appSettingsContractView, never()).finishLogout()
        verify(clearCardReaderDataAction, never()).invoke()
    }

    @Test
    fun `given logout is in progress, when replacement view is taken, then replacement view shows loading`() =
        testBlocking {
            // GIVEN
            val logoutGate = CompletableDeferred<Unit>()
            whenever(accountRepository.logout()).doSuspendableAnswer {
                logoutGate.await()
                true
            }

            // WHEN
            appSettingsPresenter.logout()
            runCurrent()
            appSettingsPresenter.dropView(appSettingsContractView)
            appSettingsPresenter.takeView(replacementView)

            // THEN
            verify(replacementView).showLogoutProgressDialog()

            logoutGate.complete(Unit)
            runCurrent()
        }

    @Test
    fun `given logout succeeds while no view is attached, when replacement view is taken, then finishes logout`() =
        testBlocking {
            // GIVEN
            val logoutGate = CompletableDeferred<Unit>()
            whenever(accountRepository.logout()).doSuspendableAnswer {
                logoutGate.await()
                true
            }

            // WHEN
            appSettingsPresenter.logout()
            runCurrent()
            appSettingsPresenter.dropView(appSettingsContractView)
            logoutGate.complete(Unit)
            runCurrent()
            appSettingsPresenter.takeView(replacementView)

            // THEN
            verify(replacementView).finishLogout()
        }

    @Test
    fun `given replacement view is attached, when original view drops, then replacement view remains attached`() =
        testBlocking {
            // WHEN
            appSettingsPresenter.takeView(replacementView)
            appSettingsPresenter.dropView(appSettingsContractView)
            whenever(accountRepository.logout()).thenReturn(false)
            appSettingsPresenter.logout()
            runCurrent()

            // THEN
            verify(replacementView).showLogoutProgressDialog()
            verify(replacementView).hideLogoutProgressDialog()
        }
}
