package com.woocommerce.android

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppPrefsHttpsWarningTest {
    @Before
    fun setUp() {
        AppPrefs.init(RuntimeEnvironment.getApplication())
        AppPrefs.getPreferences().edit().clear().apply()
    }

    @Test
    fun `given per-site HTTPS warning dismissals, when logging out, then remove all dynamic keys`() {
        AppPrefs.setHttpsConfigurationWarningDismissedAt(localSiteId = 1, dismissedAt = 100L)
        AppPrefs.setHttpsConfigurationWarningDismissedAt(localSiteId = 2, dismissedAt = 200L)

        AppPrefs.resetUserPreferences()

        assertThat(AppPrefs.getHttpsConfigurationWarningDismissedAt(1)).isZero()
        assertThat(AppPrefs.getHttpsConfigurationWarningDismissedAt(2)).isZero()
    }
}
