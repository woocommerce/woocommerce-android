package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosLockScreenStateTest {
    @Test
    fun `given locked screen, when idle or incorrect, then PIN entry is enabled`() {
        listOf(WooPosPINEntryStatus.Idle, WooPosPINEntryStatus.Incorrect).forEach { status ->
            assertThat(WooPosLockScreenState(isLocked = true, status = status).canEnterPIN).isTrue()
        }
    }

    @Test
    fun `given locked screen, when initializing checking waiting or storage unavailable, then PIN entry is disabled`() {
        listOf(
            WooPosPINEntryStatus.Initializing,
            WooPosPINEntryStatus.Checking,
            WooPosPINEntryStatus.Wait(30),
            WooPosPINEntryStatus.StorageUnavailable,
        ).forEach { status ->
            assertThat(WooPosLockScreenState(isLocked = true, status = status).canEnterPIN).isFalse()
        }
    }

    @Test
    fun `given unlocked screen, when checking PIN entry availability, then entry is disabled`() {
        assertThat(WooPosLockScreenState().canEnterPIN).isFalse()
    }
}
