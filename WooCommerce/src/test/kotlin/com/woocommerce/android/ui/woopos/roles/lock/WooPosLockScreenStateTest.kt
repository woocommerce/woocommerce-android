package com.woocommerce.android.ui.woopos.roles.lock

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosLockScreenStateTest {
    @Test
    fun `given locked screen, when idle or incorrect, then PIN entry is enabled`() {
        listOf(WooPosLockScreenState.Status.Idle, WooPosLockScreenState.Status.Incorrect).forEach { status ->
            assertThat(WooPosLockScreenState(isLocked = true, status = status).canEnterPIN).isTrue()
        }
    }

    @Test
    fun `given locked screen, when checking waiting or storage unavailable, then PIN entry is disabled`() {
        listOf(
            WooPosLockScreenState.Status.Checking,
            WooPosLockScreenState.Status.Wait(30),
            WooPosLockScreenState.Status.StorageUnavailable,
        ).forEach { status ->
            assertThat(WooPosLockScreenState(isLocked = true, status = status).canEnterPIN).isFalse()
        }
    }

    @Test
    fun `given unlocked screen, when checking PIN entry availability, then entry is disabled`() {
        assertThat(WooPosLockScreenState().canEnterPIN).isFalse()
    }
}
