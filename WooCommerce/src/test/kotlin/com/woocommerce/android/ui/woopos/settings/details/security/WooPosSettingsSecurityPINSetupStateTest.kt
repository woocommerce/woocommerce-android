package com.woocommerce.android.ui.woopos.settings.details.security

import com.woocommerce.android.ui.woopos.roles.WooPosLocalRole.ADMIN
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Choose
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Confirm
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saved
import com.woocommerce.android.ui.woopos.settings.details.security.WooPosSettingsSecurityPINSetupState.Saving
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosSettingsSecurityPINSetupStateTest {
    @Test
    fun `when a full confirmation is held, then Back close and input are unavailable`() {
        val held = Confirm(ADMIN, enteredDigits = 4)

        assertThat(held.canGoBack).isFalse()
        assertThat(held.canClose).isFalse()
        assertThat(held.canEnterDigits).isFalse()
    }

    @Test
    fun `when confirmation is partial, then Back close and input are available`() {
        val partial = Confirm(ADMIN, enteredDigits = 3)

        assertThat(partial.canGoBack).isTrue()
        assertThat(partial.canClose).isTrue()
        assertThat(partial.canEnterDigits).isTrue()
    }

    @Test
    fun `when choosing, then close is available and Back is not`() {
        assertThat(Choose(ADMIN, enteredDigits = 4).canClose).isTrue()
        assertThat(Choose(ADMIN).canGoBack).isFalse()
    }

    @Test
    fun `when saving, then close is unavailable until the PIN is saved`() {
        assertThat(Saving(ADMIN).canClose).isFalse()
        assertThat(Saved(ADMIN).canClose).isTrue()
    }
}
