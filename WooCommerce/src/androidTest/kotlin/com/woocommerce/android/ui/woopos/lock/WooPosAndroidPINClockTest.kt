package com.woocommerce.android.ui.woopos.lock

import android.os.SystemClock
import android.provider.Settings
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class WooPosAndroidPINClockTest {
    @Test
    fun samplesUseCurrentBootAndFreshMonotonicTime() {
        // GIVEN
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
        val clock = WooPosAndroidPINClock(context)
        val before = SystemClock.elapsedRealtime()

        // WHEN
        val first = clock.now()
        val after = SystemClock.elapsedRealtime()

        // THEN
        assertThat(first.bootCount).isEqualTo(boot).isGreaterThanOrEqualTo(0)
        assertThat(first.elapsedMillis).isBetween(before, after)
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1)
        while (SystemClock.elapsedRealtime() <= after && System.nanoTime() < deadline) Thread.yield()
        val laterBefore = SystemClock.elapsedRealtime()
        assertThat(laterBefore).isGreaterThan(after)
        val later = clock.now()
        val laterAfter = SystemClock.elapsedRealtime()
        assertThat(later.bootCount).isEqualTo(boot)
        assertThat(later.elapsedMillis).isBetween(laterBefore, laterAfter).isGreaterThan(first.elapsedMillis)
    }
}
