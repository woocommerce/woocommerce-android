package com.woocommerce.android.ui.woopos.util.analytics

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.hardware.display.DisplayManager
import android.view.Display
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.EntryPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosAnalyticsCommonPropertiesProviderTest {
    private val context: Context = mock()
    private val displayContext: Context = mock()
    private val displayManager: DisplayManager = mock()
    private val display: Display = mock()
    private val resources: Resources = mock()
    private val configuration = Configuration()
    private val entryPointKeeper = WooPosAnalyticsEntryPointKeeper()

    private lateinit var sut: WooPosAnalyticsCommonPropertiesProvider

    @Before
    fun setup() {
        whenever(context.getSystemService(DisplayManager::class.java)).thenReturn(displayManager)
        whenever(displayManager.getDisplay(Display.DEFAULT_DISPLAY)).thenReturn(display)
        whenever(context.createDisplayContext(display)).thenReturn(displayContext)
        whenever(displayContext.resources).thenReturn(resources)
        whenever(resources.configuration).thenReturn(configuration)

        sut = WooPosAnalyticsCommonPropertiesProvider(
            context = context,
            entryPointKeeper = entryPointKeeper,
        )
    }

    @Test
    fun `given tablet sized display, when common properties requested, then device type is tablet`() {
        // GIVEN
        configuration.smallestScreenWidthDp = 800

        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties["device_type"]).isEqualTo("tablet")
    }

    @Test
    fun `given phone sized display, when common properties requested, then device type is phone`() {
        // GIVEN
        configuration.smallestScreenWidthDp = 411

        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties["device_type"]).isEqualTo("phone")
    }

    @Test
    fun `given pos entered from tab, when common properties requested, then entry point is pos tab`() {
        // GIVEN
        entryPointKeeper.onPosEntered(EntryPoint.POS_TAB)

        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties["entry_point"]).isEqualTo("pos_tab")
    }

    @Test
    fun `given pos not entered, when common properties requested, then entry point is absent`() {
        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties).doesNotContainKey("entry_point")
    }

    @Test
    fun `given pos entered and then exited, when common properties requested, then entry point is absent`() {
        // GIVEN
        entryPointKeeper.onPosEntered(EntryPoint.POS_TAB)

        // WHEN
        entryPointKeeper.onPosSessionEnded()
        val properties = sut.commonProperties

        // THEN
        assertThat(properties).doesNotContainKey("entry_point")
    }
}
