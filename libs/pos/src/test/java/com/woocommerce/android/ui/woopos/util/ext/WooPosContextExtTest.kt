package com.woocommerce.android.ui.woopos.util.ext

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosContextExtTest {
    private val context: Context = mock()
    private val resources: Resources = mock()
    private val displayManager: DisplayManager = mock()
    private val display: Display = mock()
    private val displayMetrics = DisplayMetrics().apply { density = 1.75f }

    @Before
    fun setup() {
        whenever(context.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(displayMetrics)
        whenever(context.getSystemService(DisplayManager::class.java)).thenReturn(displayManager)
        whenever(displayManager.getDisplay(Display.DEFAULT_DISPLAY)).thenReturn(display)
    }

    @Test
    fun `given tablet display in portrait, when phone layout checked, then false`() {
        // GIVEN
        givenRealDisplaySizePx(width = 1200, height = 2000)

        // WHEN
        val result = context.isWooPosPhoneLayout()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given same tablet display in landscape, when phone layout checked, then false`() {
        // GIVEN
        givenRealDisplaySizePx(width = 2000, height = 1200)

        // WHEN
        val result = context.isWooPosPhoneLayout()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given phone display, when phone layout checked, then true`() {
        // GIVEN
        givenRealDisplaySizePx(width = 720, height = 1560)

        // WHEN
        val result = context.isWooPosPhoneLayout()

        // THEN
        assertThat(result).isTrue()
    }

    @Suppress("DEPRECATION")
    private fun givenRealDisplaySizePx(width: Int, height: Int) {
        doAnswer { invocation ->
            invocation.getArgument<Point>(0).apply {
                x = width
                y = height
            }
            Unit
        }.whenever(display).getRealSize(any())
    }
}
