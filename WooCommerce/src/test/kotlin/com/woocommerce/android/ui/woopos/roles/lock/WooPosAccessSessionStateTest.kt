package com.woocommerce.android.ui.woopos.roles.lock

import com.woocommerce.android.ui.woopos.roles.WooPosCapability
import com.woocommerce.android.ui.woopos.roles.WooPosCapability.MANAGE_STORE_SETTINGS
import com.woocommerce.android.ui.woopos.roles.WooPosCapability.PROCESS_SALES
import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenState.Status
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosAccessSessionStateTest {
    @Test
    fun `when POS is unprotected, then every capability is allowed`() {
        WooPosCapability.entries.forEach {
            assertThat(WooPosAccessSessionState.Unprotected.allows(it)).isTrue()
        }
    }

    @Test
    fun `when unlocked, then only the active role capabilities are allowed`() {
        val cashier = WooPosAccessSessionState.Unlocked(WooPosLocalRole.CASHIER)

        assertThat(cashier.allows(PROCESS_SALES)).isTrue()
        assertThat(cashier.allows(MANAGE_STORE_SETTINGS)).isFalse()
        assertThat(WooPosAccessSessionState.Unlocked(WooPosLocalRole.ADMIN).allows(MANAGE_STORE_SETTINGS)).isTrue()
    }

    @Test
    fun `when locked, then no capability is allowed`() {
        WooPosCapability.entries.forEach {
            assertThat(WooPosAccessSessionState.Locked(Status.Idle).allows(it)).isFalse()
        }
    }
}
