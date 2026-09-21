package com.woocommerce.android.ui.woopos.util.analytics

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.EntryPoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosAnalyticsCommonPropertiesProviderTest {
    private val context: Context = mock()
    private val displayManager: DisplayManager = mock()
    private val display: Display = mock()
    private val resources: Resources = mock()
    private val displayMetrics = DisplayMetrics().apply { density = 1f }
    private val entryPointKeeper = WooPosAnalyticsEntryPointKeeper()

    private lateinit var sut: WooPosAnalyticsCommonPropertiesProvider

    @Before
    fun setup() {
        whenever(context.getSystemService(DisplayManager::class.java)).thenReturn(displayManager)
        whenever(displayManager.getDisplay(Display.DEFAULT_DISPLAY)).thenReturn(display)
        whenever(context.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(displayMetrics)

        sut = WooPosAnalyticsCommonPropertiesProvider(
            context = context,
            entryPointKeeper = entryPointKeeper,
        )
    }

    @Test
    fun `given tablet sized display, when common properties requested, then device type is tablet`() {
        // GIVEN
        givenDisplaySizeDp(shortSize = 800, longSize = 1280)

        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties["device_type"]).isEqualTo("tablet")
    }

    @Test
    fun `given phone sized display, when common properties requested, then device type is phone`() {
        // GIVEN
        givenDisplaySizeDp(shortSize = 411, longSize = 891)

        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties["device_type"]).isEqualTo("phone")
    }

    @Test
    fun `given display below the POS tablet threshold, when common properties requested, then device type is phone`() {
        // GIVEN
        givenDisplaySizeDp(shortSize = 600, longSize = 960)

        // WHEN
        val properties = sut.commonProperties

        // THEN
        assertThat(properties["device_type"]).isEqualTo("phone")
    }

    @Suppress("DEPRECATION")
    private fun givenDisplaySizeDp(shortSize: Int, longSize: Int) {
        doAnswer { invocation ->
            invocation.getArgument<Point>(0).apply {
                x = shortSize
                y = longSize
            }
            Unit
        }.whenever(display).getRealSize(any())
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
