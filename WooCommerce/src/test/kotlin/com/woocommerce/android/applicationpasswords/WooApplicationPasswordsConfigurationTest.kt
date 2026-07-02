package com.woocommerce.android.applicationpasswords

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooApplicationPasswordsConfigurationTest {
    @Test
    fun `given device name contains path separators, when sanitized, then separators are replaced`() {
        // GIVEN
        val deviceName = "FIH Sharp Aquos S2 4/64\\test"

        // WHEN
        val sanitizedDeviceName = deviceName.toApplicationPasswordSafeDeviceName()

        // THEN
        assertThat(sanitizedDeviceName).isEqualTo("FIH-Sharp-Aquos-S2-4-64-test")
    }

    @Test
    fun `given device name contains spaces, when sanitized, then existing format is preserved`() {
        // GIVEN
        val deviceName = "Google Pixel 8"

        // WHEN
        val sanitizedDeviceName = deviceName.toApplicationPasswordSafeDeviceName()

        // THEN
        assertThat(sanitizedDeviceName).isEqualTo("Google-Pixel-8")
    }
}
