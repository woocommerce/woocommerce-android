package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.ADMIN
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.CASHIER
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSessionState
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINError
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupViewModel.Companion.FILLED_DOTS_HOLD_MILLIS
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupViewModel.Companion.MINIMUM_SAVING_MILLIS
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSettingsSecurityPINSetupViewModelTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }
    private val store: WooPosLocalPINStore = mock()
    private val sessionState = MutableStateFlow<WooPosAccessSessionState>(WooPosAccessSessionState.Unprotected)
    private val session: WooPosAccessSession = mock { on { state }.thenReturn(sessionState) }
    private val viewModel = WooPosSettingsSecurityPINSetupViewModel(selectedSite, store, session)

    @Test
    fun `when four digits are chosen, then the dots hold once before confirmation opens`() = runTest {
        // WHEN
        enter("12345")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 4))

        // WHEN
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS - 1)
        enter("6")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 4))

        // WHEN
        advanceTimeBy(1)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Confirm(ADMIN))
        verifyNoInteractions(store, session)
    }

    @Test
    fun `given confirmation, when Back is tapped, then the chosen PIN is discarded`() = runTest {
        // GIVEN
        choose("1234")
        enter("12")

        // WHEN
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))

        // WHEN
        choose("5678")
        enter("1234")
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.MISMATCH))
        verifyNoInteractions(store, session)
    }

    @Test
    fun `given Choose, when Back is tapped, then it is ignored`() = runTest {
        // GIVEN
        enter("12")

        // WHEN
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 2))
    }

    @Test
    fun `when confirmation differs, then Choose shows the mismatch and the next digit clears it`() = runTest {
        // GIVEN
        choose("1234")

        // WHEN
        enter("4321")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Confirm(ADMIN, enteredDigits = 4))

        // WHEN
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.MISMATCH))

        // WHEN
        enter("1")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 1))
        verifyNoInteractions(store, session)
    }

    @Test
    fun `when confirmation matches, then save starts at once and success follows the loading presentation`() =
        runTest {
            // GIVEN
            givenNoPINs()
            whenever(store.setPIN(1, ADMIN, "1234")).thenReturn(Result.success(Unit))
            choose("1234")

            // WHEN
            enter("1234")

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Confirm(ADMIN, enteredDigits = 4))
            inOrder(store, session) {
                verify(store).getPINs(1)
                verify(store).setPIN(1, ADMIN, "1234")
                verify(session).unlock("1234")
            }

            // WHEN
            advanceTimeBy(FILLED_DOTS_HOLD_MILLIS)
            runCurrent()

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Saving(ADMIN))

            // WHEN
            advanceTimeBy(MINIMUM_SAVING_MILLIS - 1)

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Saving(ADMIN))

            // WHEN
            advanceTimeBy(1)
            runCurrent()

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
            verify(store).setPIN(any(), any(), any())
        }

    @Test
    fun `given a slow save, when loading time has passed, then success waits for the actual save`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        givenNoPINs()
        whenever(store.setPIN(1, ADMIN, "1234")).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        choose("1234")
        enter("1234")

        // WHEN
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS + MINIMUM_SAVING_MILLIS + 1)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Saving(ADMIN))
        verify(session, never()).unlock(any())

        // WHEN
        gate.complete(Unit)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
        verify(session).unlock("1234")
    }

    @Test
    fun `given save failure, when confirmation matches, then failure shows at once and a retry can succeed`() =
        runTest {
            // GIVEN
            givenNoPINs()
            whenever(store.setPIN(1, ADMIN, "1234")).thenReturn(Result.failure(IllegalStateException()))
            choose("1234")

            // WHEN
            enter("1234")

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
            advanceUntilIdle()
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
            verify(session, never()).unlock(any())

            // WHEN
            whenever(store.setPIN(1, ADMIN, "1234")).thenReturn(Result.success(Unit))
            choose("1234")
            enter("1234")
            advanceUntilIdle()

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
            verify(session).unlock("1234")
        }

    @Test
    fun `given an existing Admin PIN, when confirming another PIN, then it is never overwritten`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(
            Result.success(mapOf(ADMIN to WooPosPINHash.create("5678")))
        )

        // WHEN
        chooseAndConfirm("1234")

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
    }

    @Test
    fun `given unreadable PIN storage, when confirming a PIN, then failure is not treated as initial setup`() =
        runTest {
            // GIVEN
            whenever(store.getPINs(1)).thenReturn(Result.failure(IllegalStateException()))

            // WHEN
            chooseAndConfirm("1234")

            // THEN
            verify(store, never()).setPIN(any(), any(), any())
            verify(session, never()).unlock(any())
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
        }

    @Test
    fun `given partial entry, when a digit is deleted, then the entry shrinks without saving`() = runTest {
        // GIVEN
        enter("12")

        // WHEN
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 1))
        verifyNoInteractions(store, session)
    }

    @Test
    fun `given partial confirmation, when a digit is deleted, then confirmation keeps the remaining digits`() =
        runTest {
            // GIVEN
            givenNoPINs()
            whenever(store.setPIN(1, ADMIN, "1234")).thenReturn(Result.success(Unit))
            choose("1234")
            enter("19")

            // WHEN
            viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped)
            enter("234")
            advanceUntilIdle()

            // THEN
            verify(store).setPIN(1, ADMIN, "1234")
            assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
        }

    @Test
    fun `when digits outside 0 to 9 are tapped, then they are ignored`() = runTest {
        // WHEN
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(-1))
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(10))

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
    }

    @Test
    fun `given no selected store, when confirmation matches, then nothing is saved and Choose restarts`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        chooseAndConfirm("1234")

        // THEN
        verifyNoInteractions(store, session)
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
    }

    @Test
    fun `given a held choice, when the dialog closes, then confirmation never opens`() = runTest {
        // GIVEN
        enter("1234")

        // WHEN
        viewModel.reset()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
    }

    @Test
    fun `given a pending read, when the dialog closes, then the stale request cannot save unlock or show loading`() =
        runTest {
            // GIVEN
            val gate = CompletableDeferred<Unit>()
            whenever(store.getPINs(1)).doSuspendableAnswer {
                gate.await()
                Result.success(emptyMap())
            }
            choose("1234")
            enter("1234")

            // WHEN
            viewModel.reset()
            advanceUntilIdle()
            gate.complete(Unit)
            runCurrent()

            // THEN
            verify(store, never()).setPIN(any(), any(), any())
            verify(session, never()).unlock(any())
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
        }

    @Test
    fun `given a pending read, when the selected store changes, then no PIN is saved into the old store`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(store.getPINs(1)).doSuspendableAnswer {
            gate.await()
            Result.success(emptyMap())
        }
        choose("1234")
        enter("1234")

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)
        advanceUntilIdle()

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
    }

    @Test
    fun `given a pending save, when the selected store changes, then saved PIN cannot grant new-store authority`() =
        runTest {
            // GIVEN
            val gate = givenPendingSave()
            choose("1234")
            enter("1234")

            // WHEN
            whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
            gate.complete(Unit)
            advanceUntilIdle()

            // THEN
            verify(session, never()).unlock(any())
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
        }

    @Test
    fun `given a committed save after dialog close, when it completes, then the session receives the saved PIN`() =
        runTest {
            // GIVEN
            val gate = givenPendingSave()
            choose("1234")
            enter("1234")

            // WHEN
            viewModel.reset()
            gate.complete(Unit)
            advanceUntilIdle()

            // THEN
            verify(session).unlock("1234")
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
        }

    @Test
    fun `given a committed save, when Settings scope is cancelled, then the session receives the saved PIN`() =
        runTest {
            // GIVEN
            val gate = givenPendingSave()
            choose("1234")
            enter("1234")

            // WHEN
            viewModel.viewModelScope.cancel()
            gate.complete(Unit)
            runCurrent()

            // THEN
            verify(session).unlock("1234")
        }

    @Test
    fun `given a matching confirmation, when more input arrives during saving, then the PIN is saved once`() =
        runTest {
            // GIVEN
            val gate = givenPendingSave()
            choose("1234")
            enter("1234")

            // WHEN
            enter("1234")
            viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped)
            viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped)
            advanceTimeBy(FILLED_DOTS_HOLD_MILLIS + 1)
            enter("1234")
            gate.complete(Unit)
            advanceUntilIdle()

            // THEN
            verify(store).setPIN(any(), any(), any())
            assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
        }

    @Test
    fun `given an Admin session, when a Cashier PIN is confirmed, then it is saved without changing the session`() =
        runTest {
            // GIVEN
            givenAdminPIN()
            whenever(store.setPIN(1, CASHIER, "5678")).thenReturn(Result.success(Unit))
            viewModel.start(CASHIER)

            // WHEN
            chooseAndConfirm("5678")

            // THEN
            verify(store).setPIN(1, CASHIER, "5678")
            verify(session, never()).unlock(any())
            assertThat(viewModel.state.value).isEqualTo(Saved(CASHIER))
        }

    @Test
    fun `when the Cashier PIN matches the Admin PIN, then Choose explains that the PIN is taken`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.setPIN(1, CASHIER, "1234")).thenReturn(Result.failure(WooPosPINError.MatchesOtherRole))
        viewModel.start(CASHIER)

        // WHEN
        chooseAndConfirm("1234")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.PIN_TAKEN))
    }

    @Test
    fun `given an existing Cashier PIN, when another Cashier PIN is confirmed, then it is never overwritten`() =
        runTest {
            // GIVEN
            whenever(store.getPINs(1)).thenReturn(
                Result.success(
                    mapOf(ADMIN to WooPosPINHash.create("1234"), CASHIER to WooPosPINHash.create("5678"))
                )
            )
            viewModel.start(CASHIER)

            // WHEN
            chooseAndConfirm("9012")

            // THEN
            verify(store, never()).setPIN(any(), any(), any())
            assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.SAVE_FAILED))
        }

    @Test
    fun `given a Cashier session, when a Cashier PIN is confirmed, then nothing is saved`() = runTest {
        // GIVEN
        givenAdminPIN()
        sessionState.value = WooPosAccessSessionState.Unlocked(CASHIER)
        viewModel.start(CASHIER)

        // WHEN
        chooseAndConfirm("5678")

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.SAVE_FAILED))
    }

    @Test
    fun `given POS locked during entry, when a Cashier PIN is confirmed, then nothing is saved`() = runTest {
        // GIVEN
        givenAdminPIN()
        sessionState.value = WooPosAccessSessionState.Unlocked(ADMIN)
        viewModel.start(CASHIER)
        choose("5678")

        // WHEN
        sessionState.value = WooPosAccessSessionState.Locked(Status.Idle)
        enter("5678")
        advanceUntilIdle()

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.SAVE_FAILED))
    }

    @Test
    fun `given a held Admin choice, when Cashier setup starts, then the old entry is discarded`() = runTest {
        // GIVEN
        enter("1234")

        // WHEN
        viewModel.start(CASHIER)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER))
        verifyNoInteractions(store, session)
    }

    private suspend fun givenAdminPIN() {
        whenever(store.getPINs(1)).thenReturn(Result.success(mapOf(ADMIN to WooPosPINHash.create("1234"))))
        sessionState.value = WooPosAccessSessionState.Unlocked(ADMIN)
    }

    private suspend fun givenNoPINs() {
        whenever(store.getPINs(1)).thenReturn(Result.success(emptyMap()))
    }

    private suspend fun givenPendingSave(): CompletableDeferred<Unit> {
        val gate = CompletableDeferred<Unit>()
        givenNoPINs()
        whenever(store.setPIN(1, ADMIN, "1234")).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        return gate
    }

    private fun TestScope.choose(pin: String) {
        enter(pin)
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS)
        runCurrent()
        assertThat(viewModel.state.value).isEqualTo(Confirm(viewModel.state.value.role))
    }

    private fun TestScope.chooseAndConfirm(pin: String) {
        choose(pin)
        enter(pin)
        advanceUntilIdle()
    }

    private fun enter(pin: String) = pin.forEach {
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DigitTapped(it.digitToInt()))
    }
}
