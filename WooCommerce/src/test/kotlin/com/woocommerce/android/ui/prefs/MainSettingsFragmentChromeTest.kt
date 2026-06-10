package com.woocommerce.android.ui.prefs

import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.main.AppBarStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MainSettingsFragmentChromeTest {
    @Test
    fun `when MainSettings chrome is queried, then Activity app bar is hidden`() {
        val fragment = MainSettingsFragment()

        assertThat(fragment).isInstanceOf(BaseFragment::class.java)
        assertThat((fragment as BaseFragment).activityAppBarStatus).isSameAs(AppBarStatus.Hidden)
    }
}
