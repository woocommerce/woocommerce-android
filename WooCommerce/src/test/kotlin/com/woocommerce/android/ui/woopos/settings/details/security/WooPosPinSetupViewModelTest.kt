package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockSession
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class WooPosPinSetupViewModelTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }
    private val store: WooPosLocalPINStore = mock()
    private val session: WooPosLockSession = mock()
    private val viewModel = WooPosPinSetupViewModel(selectedSite, store, session)

    @Test
    fun `given no Admin PIN, when four digits are entered once, then save before real session authentication`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(Result.success(emptyMap()))
        whenever(store.setPIN(1, WooPosLocalRole.ADMIN, "1234")).thenReturn(Result.success(Unit))

        // WHEN
        enter("1234")

        // THEN
        assertThat(viewModel.state.value.stage).isEqualTo(WooPosPinSetupState.Stage.SUCCESS)
        inOrder(store, session) {
            verify(store).getPINs(1)
            verify(store).setPIN(1, WooPosLocalRole.ADMIN, "1234")
            verify(session).initialize(unlockWith = "1234")
        }
    }

    @Test
    fun `given an existing Admin PIN, when entering another PIN, then it is never overwritten`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(
            Result.success(mapOf(WooPosLocalRole.ADMIN to WooPosPINHash.create("5678")))
        )

        // WHEN
        enter("1234")

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        verifyNoInteractions(session)
        assertReady()
    }

    @Test
    fun `given unreadable PIN storage, when entering a PIN, then failure is not treated as initial setup`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(Result.failure(IllegalStateException()))

        // WHEN
        enter("1234")

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        verifyNoInteractions(session)
        assertReady()
    }

    @Test
    fun `given save failure, when retrying, then only actual success reports saved`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(Result.success(emptyMap()))
        whenever(store.setPIN(1, WooPosLocalRole.ADMIN, "1234")).thenReturn(Result.failure(IllegalStateException()))

        // WHEN
        enter("1234")

        // THEN
        assertReady()
        verifyNoInteractions(session)

        // WHEN
        whenever(store.setPIN(1, WooPosLocalRole.ADMIN, "1234")).thenReturn(Result.success(Unit))
        enter("1234")

        // THEN
        assertThat(viewModel.state.value.stage).isEqualTo(WooPosPinSetupState.Stage.SUCCESS)
        verify(session).initialize(unlockWith = "1234")
    }

    @Test
    fun `given partial entry, when deleting resetting or entering invalid digits, then no partial PIN is saved`() = runTest {
        // WHEN
        enter("12")
        viewModel.onUIEvent(WooPosPinSetupUIEvent.DeleteTapped)
        assertThat(viewModel.state.value.enteredDigits).isEqualTo(1)
        viewModel.reset()
        viewModel.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(-1))
        viewModel.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(10))
        enter("123")

        // THEN
        assertThat(viewModel.state.value.enteredDigits).isEqualTo(3)
        verifyNoInteractions(store, session)
    }

    @Test
    fun `given a pending read, when the dialog closes, then the stale request cannot save or unlock`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(store.getPINs(1)).doSuspendableAnswer {
            gate.await()
            Result.success(emptyMap())
        }
        enter("1234")
        runCurrent()

        // WHEN
        viewModel.reset()
        gate.complete(Unit)
        runCurrent()

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        verifyNoInteractions(session)
        assertReady()
    }

    @Test
    fun `given a pending read, when the selected store changes, then no PIN is saved into the old store`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(store.getPINs(1)).doSuspendableAnswer {
            gate.await()
            Result.success(emptyMap())
        }
        enter("1234")
        runCurrent()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)
        runCurrent()

        // THEN
        verify(store, never()).setPIN(any(), any(), any())
        verifyNoInteractions(session)
        assertReady()
    }

    @Test
    fun `given a pending save, when the selected store changes, then saved PIN cannot grant new-store authority`() = runTest {
        // GIVEN
        val gate = CompletableDeferred<Unit>()
        whenever(store.getPINs(1)).thenReturn(Result.success(emptyMap()))
        whenever(store.setPIN(1, WooPosLocalRole.ADMIN, "1234")).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        enter("1234")
        runCurrent()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)
        runCurrent()

        // THEN
        verifyNoInteractions(session)
        assertReady()
    }

    @Test
    fun `given a committed save after dialog close, when it completes, then the real session receives the saved PIN`() =
        runTest {
            // GIVEN
            val gate = CompletableDeferred<Unit>()
            whenever(store.getPINs(1)).thenReturn(Result.success(emptyMap()))
            whenever(store.setPIN(1, WooPosLocalRole.ADMIN, "1234")).doSuspendableAnswer {
                gate.await()
                Result.success(Unit)
            }
            enter("1234")
            runCurrent()

            // WHEN
            viewModel.reset()
            gate.complete(Unit)
            runCurrent()

            // THEN
            verify(session).initialize(unlockWith = "1234")
            assertReady()
        }

    private fun enter(pin: String) = pin.forEach {
        viewModel.onUIEvent(WooPosPinSetupUIEvent.DigitTapped(it.digitToInt()))
    }

    private fun assertReady() {
        assertThat(viewModel.state.value).isEqualTo(WooPosPinSetupState())
    }
}
