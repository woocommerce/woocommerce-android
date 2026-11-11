package com.woocommerce.android.ui.woopos.lock

import android.provider.Settings
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class WooPosAndroidPINClockUnavailableTest {
    @Test
    fun `given missing boot identity, when sampling platform time, then no fallback identity is returned`() {
        // GIVEN
        val context = RuntimeEnvironment.getApplication()
        Settings.Global.putString(context.contentResolver, Settings.Global.BOOT_COUNT, null)
        assertThat(Settings.Global.getString(context.contentResolver, Settings.Global.BOOT_COUNT)).isNull()

        // THEN
        assertThatThrownBy { WooPosAndroidPINClock(context).now() }
            .isInstanceOf(Settings.SettingNotFoundException::class.java)
    }
}
