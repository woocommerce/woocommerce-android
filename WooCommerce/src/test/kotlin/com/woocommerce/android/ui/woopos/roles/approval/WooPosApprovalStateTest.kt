package com.woocommerce.android.ui.woopos.roles.approval

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosApprovalStateTest {
    @Test
    fun `given protection removal, when storage is checking, then its modal cannot dismiss`() {
        val state = WooPosApprovalState(
            WooPosApprovalAction.DisablePINProtection,
            WooPosPINEntryStatus.Checking,
        )
        assertThat(state.canDismiss).isFalse()
    }

    @Test
    fun `given protection removal, when storage fails, then the Admin can dismiss the error`() {
        val state = WooPosApprovalState(
            WooPosApprovalAction.DisablePINProtection,
            WooPosPINEntryStatus.StorageUnavailable,
        )
        assertThat(state.canDismiss).isTrue()
    }
}
