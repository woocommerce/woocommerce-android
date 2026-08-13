package com.woocommerce.android.background

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.background.GetBackgroundRestrictions.BackgroundRestrictions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class BackgroundUpdatesDisabledTest {
    private val getBackgroundRestrictions: GetBackgroundRestrictions = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val sut = BackgroundUpdatesDisabled(
        getBackgroundRestrictions = getBackgroundRestrictions,
        analyticsTrackerWrapper = analyticsTrackerWrapper
    )

    @Test
    fun `given no restriction is active, when invoked, then nothing is tracked`() {
        stubRestrictions()

        sut()

        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given data saver is enabled, when invoked, then background updates disabled is tracked`() {
        stubRestrictions(isDataSaverEnabled = true)

        sut()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.BACKGROUND_UPDATES_DISABLED)
    }

    @Test
    fun `given power save mode is enabled, when invoked, then background updates disabled is tracked`() {
        stubRestrictions(isPowerSaveModeEnabled = true)

        sut()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.BACKGROUND_UPDATES_DISABLED)
    }

    @Test
    fun `given background is restricted, when invoked, then background updates disabled is tracked`() {
        stubRestrictions(isBackgroundRestricted = true)

        sut()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.BACKGROUND_UPDATES_DISABLED)
    }

    @Test
    fun `given every restriction is active, when invoked, then the event is tracked once`() {
        stubRestrictions(
            isDataSaverEnabled = true,
            isPowerSaveModeEnabled = true,
            isBackgroundRestricted = true
        )

        sut()

        verify(analyticsTrackerWrapper).track(AnalyticsEvent.BACKGROUND_UPDATES_DISABLED)
    }

    private fun stubRestrictions(
        isDataSaverEnabled: Boolean = false,
        isPowerSaveModeEnabled: Boolean = false,
        isBackgroundRestricted: Boolean = false
    ) = getBackgroundRestrictions.stub {
        on { invoke() } doReturn BackgroundRestrictions(
            isDataSaverEnabled = isDataSaverEnabled,
            isPowerSaveModeEnabled = isPowerSaveModeEnabled,
            isBackgroundRestricted = isBackgroundRestricted
        )
    }
}
