package com.woocommerce.android.ui.woopos.localcatalog

import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.time.Duration.Companion.days

class WooPosIsWooCommerceVersionSunsetWarningRequiredTest {
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()
    private val preferencesRepository: WooPosPreferencesRepository = mock()
    private val dateTimeProvider: DateTimeProvider = mock()

    private lateinit var sut: WooPosIsWooCommerceVersionSunsetWarningRequired

    @Before
    fun setup() {
        sut = WooPosIsWooCommerceVersionSunsetWarningRequired(
            getWooCoreVersion = getWooCoreVersion,
            preferencesRepository = preferencesRepository,
            dateTimeProvider = dateTimeProvider,
        )
    }

    @Test
    fun `given WC version below 10_5_0 and no dismissal, when invoked, then returns true`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.4.0")
        whenever(preferencesRepository.getWooVersionSunsetBannerDismissalTimestamp()).thenReturn(null)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given WC version equal to 10_5_0, when invoked, then returns false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.5.0")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given WC version above 10_5_0, when invoked, then returns false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.6.0")

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given WC version is null, when invoked, then returns false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given WC version below 10_5_0 and within 14 day cooldown, when invoked, then returns false`() = runTest {
        // GIVEN
        val currentTime = 100_000_000L
        val dismissalTime = currentTime - 13.days.inWholeMilliseconds

        whenever(getWooCoreVersion()).thenReturn("10.4.0")
        whenever(preferencesRepository.getWooVersionSunsetBannerDismissalTimestamp()).thenReturn(dismissalTime)
        whenever(dateTimeProvider.now()).thenReturn(currentTime)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given WC version below 10_5_0 and exactly at 14 day cooldown, when invoked, then returns true`() = runTest {
        // GIVEN
        val currentTime = 100_000_000L
        val dismissalTime = currentTime - 14.days.inWholeMilliseconds

        whenever(getWooCoreVersion()).thenReturn("10.4.0")
        whenever(preferencesRepository.getWooVersionSunsetBannerDismissalTimestamp()).thenReturn(dismissalTime)
        whenever(dateTimeProvider.now()).thenReturn(currentTime)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given WC version below 10_5_0 and past 14 day cooldown, when invoked, then returns true`() = runTest {
        // GIVEN
        val currentTime = 100_000_000L
        val dismissalTime = currentTime - 15.days.inWholeMilliseconds

        whenever(getWooCoreVersion()).thenReturn("10.4.0")
        whenever(preferencesRepository.getWooVersionSunsetBannerDismissalTimestamp()).thenReturn(dismissalTime)
        whenever(dateTimeProvider.now()).thenReturn(currentTime)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given WC version 9_x below 10_5_0, when invoked, then returns true`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.0")
        whenever(preferencesRepository.getWooVersionSunsetBannerDismissalTimestamp()).thenReturn(null)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }
}
