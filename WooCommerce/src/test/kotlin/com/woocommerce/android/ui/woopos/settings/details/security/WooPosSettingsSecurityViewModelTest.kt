package com.woocommerce.android.ui.woopos.settings.details.security

import androidx.lifecycle.viewModelScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.ADMIN
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.CASHIER
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSessionState
import com.woocommerce.android.ui.woopos.roles.pin.WooPosLocalPINStore
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINError
import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINHash
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityState.Protected
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSettingsSecurityViewModelTest {
    @get:Rule
    val coroutineRule = WooPosCoroutineTestRule()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }
    private val store: WooPosLocalPINStore = mock()
    private val sessionState = MutableStateFlow<WooPosAccessSessionState>(WooPosAccessSessionState.Unprotected)
    private val session: WooPosAccessSession = mock { on { state }.thenReturn(sessionState) }

    @Test
    fun `given no PINs, when Security opens, then it offers access control setup`() = runTest {
        // GIVEN
        givenPINs()

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosSettingsSecurityState.Off)
    }

    @Test
    fun `given only an Admin PIN, when Admin opens Security, then Cashier setup is offered`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Protected(hasCashierPIN = false, removalFailed = false))
    }

    @Test
    fun `given both PINs, when Admin opens Security, then the Cashier PIN is shown as set`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN, CASHIER)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Protected(hasCashierPIN = true, removalFailed = false))
    }

    @Test
    fun `given a Cashier session, when Security opens, then PIN controls are unavailable`() = runTest {
        // GIVEN
        givenPINs(ADMIN, CASHIER)
        sessionState.value = WooPosAccessSessionState.Unlocked(CASHIER)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosSettingsSecurityState.Restricted)
    }

    @Test
    fun `given unreadable PIN storage, when Security opens, then it never offers setup`() = runTest {
        // GIVEN
        whenever(store.getPINs(1)).thenReturn(Result.failure(WooPosPINError.StorageFailure))

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosSettingsSecurityState.Unavailable)
    }

    @Test
    fun `when the first Admin PIN unlocks the session, then Security shows protection on`() = runTest {
        // GIVEN
        givenPINs()
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        givenAdminSession(ADMIN)
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Protected(hasCashierPIN = false, removalFailed = false))
    }

    @Test
    fun `given a saved Cashier PIN, when Security refreshes, then the Cashier PIN is shown as set`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        givenPINs(ADMIN, CASHIER)
        viewModel.refresh()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Protected(hasCashierPIN = true, removalFailed = false))
    }

    @Test
    fun `when protection is turned off, then both PINs are removed and setup is offered again`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN, CASHIER)
        whenever(store.removePIN(1, ADMIN)).doSuspendableAnswer {
            givenPINs()
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onProtectionTurnedOff()
        advanceUntilIdle()

        // THEN
        verify(store).removePIN(1, ADMIN)
        verify(session).initialize()
        assertThat(viewModel.state.value).isEqualTo(WooPosSettingsSecurityState.Off)
    }

    @Test
    fun `given removal failure, when protection is turned off, then protection stays on with an error`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN, CASHIER)
        whenever(store.removePIN(1, ADMIN)).thenReturn(Result.failure(WooPosPINError.StorageFailure))
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onProtectionTurnedOff()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(Protected(hasCashierPIN = true, removalFailed = true))
    }

    @Test
    fun `given a pending removal, when the toggle is switched again, then PINs are removed once`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN)
        val gate = CompletableDeferred<Unit>()
        whenever(store.removePIN(1, ADMIN)).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onProtectionTurnedOff()
        viewModel.onProtectionTurnedOff()
        gate.complete(Unit)
        advanceUntilIdle()

        // THEN
        verify(store, times(1)).removePIN(any(), any())
    }

    @Test
    fun `given a pending removal, when the selected store changes, then the old result is not shown`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN)
        val gate = CompletableDeferred<Unit>()
        whenever(store.removePIN(1, ADMIN)).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onProtectionTurnedOff()

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        gate.complete(Unit)
        advanceUntilIdle()

        // THEN
        verify(session, never()).initialize()
        assertThat(viewModel.state.value).isEqualTo(Protected(hasCashierPIN = false, removalFailed = false))
    }

    @Test
    fun `given a Cashier session, when the toggle is switched off, then no PIN is removed`() = runTest {
        // GIVEN
        givenPINs(ADMIN, CASHIER)
        sessionState.value = WooPosAccessSessionState.Unlocked(CASHIER)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        viewModel.onProtectionTurnedOff()
        advanceUntilIdle()

        // THEN
        verify(store, never()).removePIN(any(), any())
    }

    @Test
    fun `given Admin Security, when the session becomes Cashier, then PIN controls close and stay ignored`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN, CASHIER)
        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        sessionState.value = WooPosAccessSessionState.Unlocked(CASHIER)
        advanceUntilIdle()
        viewModel.onProtectionTurnedOff()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosSettingsSecurityState.Restricted)
        verify(store, never()).removePIN(any(), any())
    }

    @Test
    fun `given a pending removal, when Settings closes, then the session still reads the removed PINs`() = runTest {
        // GIVEN
        givenAdminSession(ADMIN)
        val gate = CompletableDeferred<Unit>()
        whenever(store.removePIN(1, ADMIN)).doSuspendableAnswer {
            gate.await()
            Result.success(Unit)
        }
        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onProtectionTurnedOff()

        // WHEN
        viewModel.viewModelScope.cancel()
        gate.complete(Unit)
        advanceUntilIdle()

        // THEN
        verify(session).initialize()
    }

    @Test
    fun `given no selected store, when Security opens, then it never offers setup`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(null)

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        assertThat(viewModel.state.value).isEqualTo(WooPosSettingsSecurityState.Unavailable)
    }

    private fun createViewModel() = WooPosSettingsSecurityViewModel(selectedSite, store, session)

    private suspend fun givenAdminSession(vararg pins: WooPosLocalRole) {
        givenPINs(*pins)
        sessionState.value = WooPosAccessSessionState.Unlocked(ADMIN)
    }

    private suspend fun givenPINs(vararg roles: WooPosLocalRole) {
        whenever(store.getPINs(1)).thenReturn(Result.success(roles.associateWith { HASH }))
    }

    private companion object {
        val HASH: WooPosPINHash = WooPosPINHash.create("1234")
    }
}
