package com.woocommerce.android.ui.woopos.roles.approval

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosAccessSession
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLocalRateLimiterTestFixture
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import com.woocommerce.android.ui.woopos.roles.pin.WooPosClearLocalPINData
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosManagerApprovalViewModelTest {
    @get:Rule
    val coroutines = WooPosCoroutineTestRule()

    private val flags: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.WOO_POS_ROLES) }.thenReturn(true)
    }
    private val site: SelectedSite = mock {
        on { getOrNull() }.thenReturn(SiteModel().apply { id = 1 })
    }
    private val fixture = WooPosLocalRateLimiterTestFixture(coroutines.testDispatchers)
    private val rateLimiter = fixture.rateLimiter()
    private val access = WooPosAccessSession(flags, site, rateLimiter, fixture.recovery)

    @Test
    fun `given Admin is active, when Exit is requested, then the direct action runs without a prompt`() = runTest {
        // GIVEN
        val viewModel = signIn(ADMIN_PIN)
        var directlyAllowed = false

        // WHEN
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { directlyAllowed = true }, onApproved = {})

        // THEN
        assertThat(directlyAllowed).isTrue()
        assertThat(viewModel.state.value).isNull()
    }

    @Test
    fun `given Cashier is active, when Admin approves coupon creation, then only that action runs once`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        var created = 0
        var exited = 0
        viewModel.run(WooPosApprovalAction.CreateCoupon, onAllowed = { created++ })
        runCurrent()
        fixture.pinReadGate = CompletableDeferred()

        // WHEN
        viewModel.enter(ADMIN_PIN)
        viewModel.enter(ADMIN_PIN)
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { exited++ })
        assertThat(viewModel.state.value?.status).isEqualTo(Status.Checking)
        assertThat(viewModel.state.value?.enteredDigits).isEqualTo(4)
        fixture.pinReadGate?.complete(Unit)
        runCurrent()

        // THEN
        assertThat(created).isEqualTo(1)
        assertThat(exited).isZero()
        assertThat(viewModel.state.value).isNull()
        assertThat(access.state.value.activeRole).isEqualTo(WooPosLocalRole.CASHIER)
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { exited++ })
        assertThat(viewModel.state.value?.action).isEqualTo(WooPosApprovalAction.Exit)
        assertThat(exited).isZero()
        viewModel.cancel()
    }

    @Test
    fun `given unlock charged four failures, when Cashier tries to approve, then approval and unlock share the wait`() =
        runTest {
            // GIVEN
            access.initialize()
            repeat(4) { access.unlock(WRONG_PIN) }
            val viewModel = signIn(CASHIER_PIN)
            var performed = false
            viewModel.run(WooPosApprovalAction.Exit, onAllowed = { performed = true })
            runCurrent()

            // WHEN
            viewModel.enter(CASHIER_PIN)
            runCurrent()
            assertThat(viewModel.state.value?.status).isEqualTo(Status.Wait(30))
            viewModel.enter(ADMIN_PIN)
            viewModel.cancel()
            access.lock()
            access.unlock(ADMIN_PIN)

            // THEN
            assertThat(performed).isFalse()
            assertThat(access.state.value.status).isEqualTo(Status.Wait(30))
            assertThat(fixture.records[1]?.failures).isEqualTo(5L)
        }

    @Test
    fun `given an approval is open, when a wrong PIN is entered, then it can be retried without executing`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        var performed = false
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { performed = true })
        runCurrent()

        // WHEN
        viewModel.enter(WRONG_PIN)
        runCurrent()

        // THEN
        assertThat(viewModel.state.value?.status).isEqualTo(Status.Incorrect)
        assertThat(performed).isFalse()
        viewModel.enter(ADMIN_PIN)
        runCurrent()
        assertThat(performed).isTrue()
    }

    @Test
    fun `given an existing cooldown, when the clock reaches its end, then Admin can approve`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = {})
        runCurrent()
        repeat(5) {
            viewModel.enter(WRONG_PIN)
            runCurrent()
        }
        viewModel.cancel()
        var performed = false
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { performed = true })
        runCurrent()
        assertThat(viewModel.state.value?.status).isEqualTo(Status.Wait(30))

        // WHEN
        fixture.elapsed += 30_000
        advanceTimeBy(1_000)
        runCurrent()
        viewModel.enter(ADMIN_PIN)
        runCurrent()

        // THEN
        assertThat(performed).isTrue()
        assertThat(fixture.records[1]?.failures).isZero()
    }

    @Test
    fun `given a PIN check in progress, when canceled, then its result cannot run a newer action`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        var created = false
        var exited = false
        viewModel.run(WooPosApprovalAction.CreateCoupon, onAllowed = { created = true })
        runCurrent()
        val pendingRead = CompletableDeferred<Unit>()
        fixture.pinReadGate = pendingRead
        viewModel.enter(ADMIN_PIN)
        runCurrent()

        // WHEN
        viewModel.cancel()
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { exited = true })
        pendingRead.complete(Unit)
        runCurrent()

        // THEN
        assertThat(created).isFalse()
        assertThat(exited).isFalse()
        assertThat(viewModel.state.value?.action).isEqualTo(WooPosApprovalAction.Exit)
        viewModel.cancel()
    }

    @Test
    fun `given an approval is open, when the session refreshes without a role change, then the prompt closes`() =
        runTest {
            // GIVEN
            val viewModel = signIn(CASHIER_PIN)
            var performed = false
            viewModel.run(WooPosApprovalAction.Exit, onAllowed = { performed = true })
            runCurrent()
            assertThat(viewModel.state.value?.status).isEqualTo(Status.Idle)
            viewModel.onDigit(ADMIN_PIN.first().digitToInt())

            // WHEN
            access.initialize()
            runCurrent()

            // THEN
            assertThat(access.state.value.activeRole).isEqualTo(WooPosLocalRole.CASHIER)
            assertThat(viewModel.state.value).isNull()
            assertThat(performed).isFalse()
        }

    @Test
    fun `given a PIN check in progress, when locking starts, then late approval cannot execute`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        val pendingRead = CompletableDeferred<Unit>()
        var performed = false
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { performed = true })
        runCurrent()
        fixture.pinReadGate = pendingRead
        viewModel.enter(ADMIN_PIN)
        runCurrent()

        // WHEN
        launch { access.lock() }
        runCurrent()
        pendingRead.complete(Unit)
        runCurrent()

        // THEN
        assertThat(performed).isFalse()
        assertThat(viewModel.state.value).isNull()
        assertThat(access.state.value.isLocked).isTrue()
    }

    @Test
    fun `given a PIN check in progress, when logout clears PINs, then late approval cannot execute`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        val pendingRead = CompletableDeferred<Unit>()
        var performed = false
        viewModel.run(WooPosApprovalAction.Exit, onAllowed = { performed = true })
        runCurrent()
        fixture.pinReadGate = pendingRead
        viewModel.enter(ADMIN_PIN)
        runCurrent()
        whenever(fixture.store.clearAll()).thenReturn(Result.success(Unit))

        // WHEN
        launch { WooPosClearLocalPINData(fixture.store, rateLimiter)() }
        runCurrent()
        pendingRead.complete(Unit)
        runCurrent()

        // THEN
        assertThat(performed).isFalse()
        assertThat(viewModel.state.value).isNull()
    }

    @Test
    fun `given a PIN check in progress, when the selected store changes, then late approval cannot execute`() = runTest {
        // GIVEN
        val viewModel = signIn(CASHIER_PIN)
        val pendingRead = CompletableDeferred<Unit>()
        var performed = false
        viewModel.run(WooPosApprovalAction.IssueRefund("1043"), onAllowed = { performed = true })
        runCurrent()
        fixture.pinReadGate = pendingRead
        viewModel.enter(ADMIN_PIN)
        runCurrent()

        // WHEN
        whenever(site.getOrNull()).thenReturn(SiteModel().apply { id = 2 })
        pendingRead.complete(Unit)
        runCurrent()

        // THEN
        assertThat(performed).isFalse()
        assertThat(viewModel.state.value).isNull()
    }

    private suspend fun signIn(pin: String): WooPosManagerApprovalViewModel {
        access.initialize()
        access.unlock(pin)
        return WooPosManagerApprovalViewModel(access)
    }

    private fun WooPosManagerApprovalViewModel.enter(pin: String) {
        pin.forEach { onDigit(it.digitToInt()) }
    }

    private companion object {
        const val ADMIN_PIN = "1234"
        const val CASHIER_PIN = "5678"
        const val WRONG_PIN = "0000"
    }
}
