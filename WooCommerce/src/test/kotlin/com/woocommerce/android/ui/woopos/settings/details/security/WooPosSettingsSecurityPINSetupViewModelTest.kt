package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.ADMIN
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.CASHIER
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSessionState
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINError
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.AccessUnavailable
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Error
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Page
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Rejected
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupViewModel.Companion.FILLED_DOTS_HOLD_MILLIS
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupViewModel.Companion.MINIMUM_SAVING_MILLIS
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupViewModel.Companion.PIN_SETUP_REJECTION_PAUSE_MILLIS
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
import org.mockito.kotlin.eq
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
    private val session: WooPosAccessSession = mock {
        on { state }.thenReturn(sessionState)
        on { allows(any()) }.thenAnswer { sessionState.value.allows(it.getArgument(0)) }
        on { approvalScope() }.thenReturn(WooPosAccessSession.ApprovalScope(1, 1))
    }
    private val viewModel = WooPosSettingsSecurityPINSetupViewModel(selectedSite, store, session).apply { start(ADMIN) }

    @Test
    fun `given a Cashier change, when confirmed, then the replacement is written once`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(eq(1), eq(CASHIER), any())).thenReturn(Result.success(Unit))
        whenever(store.replacePIN(eq(1), eq(CASHIER), any(), any())).thenReturn(Result.success(Unit))
        viewModel.start(CASHIER, changingPIN = true)

        // WHEN
        chooseAndConfirm("9012")

        // THEN
        verify(store).replacePIN(eq(1), eq(CASHIER), any(), any())
        verify(store, never()).createPIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Saved(CASHIER))
    }

    @Test
    fun `given an Admin change without an approval scope, when confirmed, then storage is unchanged`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(session.approvalScope()).thenReturn(null)
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        viewModel.start(ADMIN, changingPIN = true)

        // WHEN
        chooseAndConfirm("9012")

        // THEN
        verify(store, never()).replacePIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

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
        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
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
        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
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
        assertThat(viewModel.state.value).isEqualTo(Rejected(ADMIN, Page.CONFIRM))

        // WHEN
        advanceTimeBy(PIN_SETUP_REJECTION_PAUSE_MILLIS)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.MISMATCH))

        // WHEN
        enter("1")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 1))
        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
    }

    @Test
    fun `when confirmation matches, then save starts at once and success follows the loading presentation`() =
        runTest {
            // GIVEN
            givenNoPINs()
            whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).thenReturn(Result.success(Unit))
            choose("1234")

            // WHEN
            enter("1234")

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Confirm(ADMIN, enteredDigits = 4))
            inOrder(store, session) {
                verify(store).createPIN(eq(1), eq(ADMIN), eq("1234"), any())
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
            verify(store).createPIN(any(), any(), any(), any())
        }

    @Test
    fun `given a slow save, when loading time has passed, then success waits for the actual save`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        givenNoPINs()
        whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).doSuspendableAnswer {
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
            whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any()))
                .thenReturn(Result.failure(IllegalStateException()))
            choose("1234")

            // WHEN
            enter("1234")

            // THEN
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
            advanceUntilIdle()
            assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
            verify(session, never()).unlock(any())

            // WHEN
            whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).thenReturn(Result.success(Unit))
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
        whenever(store.createPIN(eq(1), eq(ADMIN), any(), any()))
            .thenReturn(Result.failure(WooPosPINError.AlreadyExists))

        // WHEN
        chooseAndConfirm("1234")

        // THEN
        verify(store).createPIN(eq(1), eq(ADMIN), any(), any())
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
    }

    @Test
    fun `given unreadable PIN storage, when confirming a PIN, then failure is not treated as initial setup`() =
        runTest {
            // GIVEN
            whenever(store.createPIN(eq(1), eq(ADMIN), any(), any()))
                .thenReturn(Result.failure(WooPosPINError.StorageFailure))

            // WHEN
            chooseAndConfirm("1234")

            // THEN
            verify(store).createPIN(eq(1), eq(ADMIN), any(), any())
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
        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
    }

    @Test
    fun `given partial confirmation, when a digit is deleted, then confirmation keeps the remaining digits`() =
        runTest {
            // GIVEN
            givenNoPINs()
            whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).thenReturn(Result.success(Unit))
            choose("1234")
            enter("19")

            // WHEN
            viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.DeleteTapped)
            enter("234")
            advanceUntilIdle()

            // THEN
            verify(store).createPIN(eq(1), eq(ADMIN), eq("1234"), any())
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
        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
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
    fun `given pending validation, when the dialog closes, then its result cannot reopen or save`() = runTest {
        givenAdminPIN()
        val gate = CompletableDeferred<Unit>()
        whenever(store.validatePINUniqueness(eq(1), eq(CASHIER), any())).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        viewModel.start(CASHIER)
        enter("5678")
        runCurrent()

        viewModel.reset()
        gate.complete(Unit)
        advanceUntilIdle()
        enter("5678")
        advanceUntilIdle()

        verify(store, never()).createPIN(any(), any(), any(), any())
        verify(store, never()).replacePIN(any(), any(), any(), any())
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER))
    }

    @Test
    fun `given pending validation, when the store changes, then later input cannot retarget it`() = runTest {
        givenAdminPIN()
        val gate = CompletableDeferred<Unit>()
        whenever(store.validatePINUniqueness(eq(1), eq(CASHIER), any())).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        viewModel.start(CASHIER)
        enter("5678")
        runCurrent()

        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)
        advanceUntilIdle()
        enter("5678")
        advanceUntilIdle()

        verify(store, never()).createPIN(any(), any(), any(), any())
        verify(store, never()).replacePIN(any(), any(), any(), any())
        verify(store, never()).validatePINUniqueness(eq(2), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
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
            assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
        }

    @Test
    fun `given a chosen PIN, when the selected store changes before confirmation, then nothing is saved`() = runTest {
        // GIVEN
        givenNoPINs()
        choose("1234")

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        enter("1234")
        advanceUntilIdle()

        // THEN
        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    @Test
    fun `given a fast save, when the selected store changes during loading, then success is not shown`() = runTest {
        // GIVEN
        givenNoPINs()
        whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).thenReturn(Result.success(Unit))
        choose("1234")
        enter("1234")

        // WHEN
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS)
        runCurrent()
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        advanceUntilIdle()

        // THEN
        verify(store).createPIN(eq(1), eq(ADMIN), eq("1234"), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
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
            verify(store).createPIN(any(), any(), any(), any())
            assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
        }

    @Test
    fun `when the PIN is saved but Admin access is unavailable, then setup does not show success`() = runTest {
        // GIVEN
        givenNoPINs(WooPosAccessSessionState.Locked(WooPosPINEntryStatus.StorageUnavailable))
        whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).thenReturn(Result.success(Unit))

        // WHEN
        chooseAndConfirm("1234")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
        verify(session).unlock("1234")
        verify(store, never()).removePIN(any(), any(), any())
    }

    @Test
    fun `given an Admin session, when a Cashier PIN is confirmed, then it is saved without changing the session`() =
        runTest {
            // GIVEN
            givenAdminPIN()
            whenever(store.createPIN(eq(1), eq(CASHIER), eq("5678"), any())).thenReturn(Result.success(Unit))
            viewModel.start(CASHIER)

            // WHEN
            chooseAndConfirm("5678")

            // THEN
            verify(store).createPIN(eq(1), eq(CASHIER), eq("5678"), any())
            verify(store, never()).getPINs(1)
            verify(session, never()).unlock(any())
            assertThat(viewModel.state.value).isEqualTo(Saved(CASHIER))
        }

    @Test
    fun `given POS locks during the Cashier save, when the save completes, then setup reports the saved PIN`() =
        runTest {
            // GIVEN
            givenAdminPIN()
            val gate = CompletableDeferred<Unit>()
            whenever(store.createPIN(eq(1), eq(CASHIER), eq("5678"), any())).doSuspendableAnswer {
                gate.await()
                Result.success(Unit)
            }
            viewModel.start(CASHIER)
            choose("5678")
            enter("5678")

            // WHEN
            sessionState.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Idle)
            gate.complete(Unit)
            advanceUntilIdle()

            // THEN
            verify(session, never()).unlock(any())
            assertThat(sessionState.value).isEqualTo(WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Idle))
            assertThat(viewModel.state.value).isEqualTo(Saved(CASHIER))
        }

    @Test
    fun `when the chosen Cashier PIN matches the Admin PIN, then Choose reports it before confirmation`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(1, CASHIER, "1234"))
            .thenReturn(Result.failure(WooPosPINError.MatchesOtherRole))
        viewModel.start(CASHIER)

        // WHEN
        enter("1234")
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Rejected(CASHIER, Page.CHOOSE))
        advanceUntilIdle()
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.PIN_TAKEN))
        verify(store, never()).createPIN(any(), any(), any(), any())
    }

    @Test
    fun `given a PIN becomes a duplicate after Choose, when confirmed, then Choose reports the new conflict`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.createPIN(eq(1), eq(CASHIER), eq("1234"), any()))
            .thenReturn(Result.failure(WooPosPINError.MatchesOtherRole))
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
            givenAdminPIN()
            whenever(store.createPIN(eq(1), eq(CASHIER), eq("9012"), any()))
                .thenReturn(Result.failure(WooPosPINError.AlreadyExists))
            viewModel.start(CASHIER)

            // WHEN
            chooseAndConfirm("9012")

            // THEN
            verify(store).createPIN(eq(1), eq(CASHIER), eq("9012"), any())
            verify(session, never()).unlock(any())
            assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.SAVE_FAILED))
        }

    @Test
    fun `given a Cashier session, when a Cashier PIN is chosen, then it is rejected without reading PIN storage`() = runTest {
        // GIVEN
        givenAdminPIN()
        sessionState.value = WooPosAccessSessionState.Unlocked(CASHIER)
        viewModel.start(CASHIER)

        // WHEN
        enter("5678")
        advanceUntilIdle()

        // THEN
        verify(store, never()).validatePINUniqueness(any(), any(), any())
        verify(store, never()).createPIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    @Test
    fun `given POS locked during entry, when a Cashier PIN is confirmed, then nothing is saved`() = runTest {
        // GIVEN
        givenAdminPIN()
        sessionState.value = WooPosAccessSessionState.Unlocked(ADMIN)
        viewModel.start(CASHIER)
        choose("5678")

        // WHEN
        sessionState.value = WooPosAccessSessionState.Locked(WooPosPINEntryStatus.Idle)
        enter("5678")
        advanceUntilIdle()

        // THEN
        verify(store, never()).createPIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
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
        verifyNoInteractions(store)
    }

    @Test
    fun `given a partial entry, when setup starts again for the same role, then the entry is kept`() = runTest {
        // GIVEN
        viewModel.start(ADMIN)
        enter("12")

        // WHEN
        viewModel.start(ADMIN)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, enteredDigits = 2))
    }

    @Test
    fun `given no PINs, when a restored Cashier setup is confirmed, then nothing unlocks`() = runTest {
        // GIVEN
        givenNoPINs()
        whenever(store.validatePINUniqueness(1, CASHIER, "5678")).thenReturn(Result.success(Unit))
        whenever(store.createPIN(eq(1), eq(CASHIER), eq("5678"), any()))
            .thenReturn(Result.failure(WooPosPINError.AdminRequired))
        viewModel.start(CASHIER)

        // WHEN
        chooseAndConfirm("5678")

        // THEN
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.SAVE_FAILED))
    }

    @Test
    fun `when saving leaves the session unprotected, then setup does not show success`() = runTest {
        // GIVEN
        givenNoPINs(WooPosAccessSessionState.Unprotected)
        whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).thenReturn(Result.success(Unit))

        // WHEN
        chooseAndConfirm("1234")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    @Test
    fun `given an Admin PIN change, when the new PIN is confirmed, then it replaces the saved PIN once`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        whenever(store.replacePIN(eq(1), eq(ADMIN), eq("9012"), any())).thenReturn(Result.success(Unit))
        viewModel.start(ADMIN, changingPIN = true)

        // WHEN
        chooseAndConfirm("9012")
        enter("9012")
        advanceUntilIdle()

        // THEN
        verify(store).replacePIN(eq(1), eq(ADMIN), eq("9012"), any())
        verify(store, never()).createPIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
    }

    @Test
    fun `given a Cashier PIN change, when confirmation differs, then the saved PIN is not replaced`() = runTest {
        // GIVEN
        givenAdminPIN()
        viewModel.start(CASHIER, changingPIN = true)
        choose("9012")

        // WHEN
        enter("3456")
        advanceUntilIdle()

        // THEN
        verify(store, never()).replacePIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Choose(CASHIER, error = Error.MISMATCH))
    }

    @Test
    fun `given an Admin change, when the proposed PIN matches Cashier, then Choose shows PIN taken`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(1, ADMIN, "5678"))
            .thenReturn(Result.failure(WooPosPINError.MatchesOtherRole))
        viewModel.start(ADMIN, changingPIN = true)

        // WHEN
        enter("5678")
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.PIN_TAKEN))
        verify(store, never()).replacePIN(any(), any(), any(), any())
    }

    @Test
    fun `given an Admin change, when replacement storage fails, then the editor stays available to retry`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        whenever(store.replacePIN(eq(1), eq(ADMIN), eq("9012"), any()))
            .thenReturn(Result.failure(WooPosPINError.StorageFailure))
        viewModel.start(ADMIN, changingPIN = true)

        // WHEN
        chooseAndConfirm("9012")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN, error = Error.SAVE_FAILED))
    }

    @Test
    fun `given a previously approved editor, when the access scope changes, then replacement is blocked`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        viewModel.start(ADMIN, changingPIN = true)
        choose("9012")
        whenever(session.approvalScope()).thenReturn(WooPosAccessSession.ApprovalScope(1, 2))

        // WHEN
        enter("9012")
        advanceUntilIdle()

        // THEN
        verify(store, never()).replacePIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    @Test
    fun `given a confirmed Admin change, when the screen is recreated, then confirmation is preserved`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        viewModel.start(ADMIN, changingPIN = true)
        choose("9012")

        // WHEN
        viewModel.start(ADMIN, changingPIN = true)

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Confirm(ADMIN))
    }

    @Test
    fun `given an Admin change confirmation, when Back chooses another PIN, then replacement remains authorized`() = runTest {
        // GIVEN
        givenAdminPIN()
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        whenever(store.replacePIN(eq(1), eq(ADMIN), eq("3456"), any())).thenReturn(Result.success(Unit))
        viewModel.start(ADMIN, changingPIN = true)
        choose("9012")

        // WHEN
        viewModel.onUIEvent(WooPosSettingsSecurityPINSetupUIEvent.BackTapped)
        chooseAndConfirm("3456")

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
        verify(store).replacePIN(eq(1), eq(ADMIN), eq("3456"), any())
    }

    @Test
    fun `given an approved editor scope, when composition starts after the scope changes, then it cannot gain new authority`() =
        runTest {
            // GIVEN
            givenAdminPIN()
            whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
            viewModel.start(ADMIN, changingPIN = true)
            whenever(session.approvalScope()).thenReturn(WooPosAccessSession.ApprovalScope(1, 2))

            // WHEN
            viewModel.start(ADMIN, changingPIN = true)
            chooseAndConfirm("9012")

            // THEN
            verify(store, never()).replacePIN(any(), any(), any(), any())
            assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
        }

    @Test
    fun `given unopened setup, when input arrives after reset, then it cannot create a PIN`() = runTest {
        viewModel.reset()

        enter("1234")
        advanceUntilIdle()
        enter("1234")
        advanceUntilIdle()

        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(Choose(ADMIN))
    }

    @Test
    fun `given Admin setup, when the site changes before entry, then input cannot retarget setup`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })

        enter("1234")
        advanceUntilIdle()
        enter("1234")
        advanceUntilIdle()

        verifyNoInteractions(store)
        verify(session, never()).unlock(any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    @Test
    fun `given invalidated setup, when explicitly reopened on the new site, then only that site is saved`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        enter("1")
        whenever(store.createPIN(eq(2), eq(ADMIN), any(), any())).thenReturn(Result.success(Unit))
        whenever(session.unlock(any())).doSuspendableAnswer {
            sessionState.value = WooPosAccessSessionState.Unlocked(ADMIN)
        }

        viewModel.start(ADMIN)
        chooseAndConfirm("1234")

        verify(store).createPIN(eq(2), eq(ADMIN), any(), any())
        verify(store, never()).createPIN(eq(1), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
    }

    @Test
    fun `given an expired change, when more input arrives, then it cannot become initial creation`() = runTest {
        givenAdminPIN()
        viewModel.start(ADMIN, changingPIN = true)
        whenever(session.approvalScope()).thenReturn(WooPosAccessSession.ApprovalScope(1, 2))
        enter("1")
        sessionState.value = WooPosAccessSessionState.Unprotected

        chooseAndConfirm("9012")

        verify(store, never()).createPIN(any(), any(), any(), any())
        verify(store, never()).replacePIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    @Test
    fun `given an expired change, when explicitly reopened, then the new editor still replaces only`() = runTest {
        givenAdminPIN()
        viewModel.start(ADMIN, changingPIN = true)
        whenever(session.approvalScope()).thenReturn(WooPosAccessSession.ApprovalScope(1, 2))
        enter("1")
        whenever(store.validatePINUniqueness(eq(1), eq(ADMIN), any())).thenReturn(Result.success(Unit))
        whenever(store.replacePIN(eq(1), eq(ADMIN), any(), any())).thenReturn(Result.success(Unit))

        viewModel.start(ADMIN, changingPIN = true)
        chooseAndConfirm("9012")

        verify(store).replacePIN(eq(1), eq(ADMIN), any(), any())
        verify(store, never()).createPIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(Saved(ADMIN))
    }

    @Test
    fun `given a removed target PIN, when replacement fails stale, then retry requires reopening`() = runTest {
        givenAdminPIN()
        viewModel.start(CASHIER, changingPIN = true)
        whenever(store.replacePIN(eq(1), eq(CASHIER), any(), any()))
            .thenReturn(Result.failure(WooPosPINError.StaleRequest))

        chooseAndConfirm("9012")
        chooseAndConfirm("9012")

        verify(store).replacePIN(eq(1), eq(CASHIER), any(), any())
        verify(store, never()).createPIN(any(), any(), any(), any())
        assertThat(viewModel.state.value).isEqualTo(AccessUnavailable)
    }

    private suspend fun givenAdminPIN() {
        whenever(store.validatePINUniqueness(eq(1), eq(CASHIER), any())).thenReturn(Result.success(Unit))
        sessionState.value = WooPosAccessSessionState.Unlocked(ADMIN)
    }

    private suspend fun givenNoPINs(
        accessAfterUnlock: WooPosAccessSessionState = WooPosAccessSessionState.Unlocked(ADMIN),
    ) {
        whenever(session.unlock(any())).doSuspendableAnswer { sessionState.value = accessAfterUnlock }
    }

    private suspend fun givenPendingSave(): CompletableDeferred<Unit> {
        val gate = CompletableDeferred<Unit>()
        givenNoPINs()
        whenever(store.createPIN(eq(1), eq(ADMIN), eq("1234"), any())).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        return gate
    }

    private fun TestScope.choose(pin: String) {
        enter(pin)
        advanceTimeBy(FILLED_DOTS_HOLD_MILLIS)
        runCurrent()
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
