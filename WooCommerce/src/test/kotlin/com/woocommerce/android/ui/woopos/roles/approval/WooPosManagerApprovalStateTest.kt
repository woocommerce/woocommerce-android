package com.woocommerce.android.ui.woopos.roles.approval

import com.woocommerce.android.ui.woopos.roles.pin.WooPosPINEntryStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosManagerApprovalStateTest {
    @Test
    fun `given protection removal, when storage is checking, then its modal cannot dismiss`() {
        val state = WooPosManagerApprovalState(
            WooPosManagerApprovalAction.DisablePINProtection,
            WooPosPINEntryStatus.Checking,
        )
        assertThat(state.canDismiss).isFalse()
    }

    @Test
    fun `given protection removal, when storage fails, then the Admin can dismiss the error`() {
        val state = WooPosManagerApprovalState(
            WooPosManagerApprovalAction.DisablePINProtection,
            WooPosPINEntryStatus.StorageUnavailable,
        )
        assertThat(state.canDismiss).isTrue()
    }
}
