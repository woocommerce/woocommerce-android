package com.woocommerce.android.ui.woopos

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.util.DisplayMetrics
import android.view.Display
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosIsScreenSizeAllowedTest {

    private val context: Context = mock()
    private val resources: Resources = mock()
    private val displayManager: DisplayManager = mock()
    private val display: Display = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val displayMetrics = DisplayMetrics().apply { density = 1f }

    private lateinit var sut: WooPosIsScreenSizeAllowed

    @Before
    fun setup() {
        whenever(context.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(displayMetrics)
        whenever(context.getSystemService(DisplayManager::class.java)).thenReturn(displayManager)
        whenever(displayManager.getDisplay(Display.DEFAULT_DISPLAY)).thenReturn(display)

        sut = WooPosIsScreenSizeAllowed(
            context = context,
            featureFlagRepository = featureFlagRepository,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given tablet screen size, when invoked, then return true`() {
        // GIVEN
        givenRealDisplaySizePx(width = 800, height = 1280)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given phone screen size and phone flag enabled, when invoked, then return true`() {
        // GIVEN
        givenRealDisplaySizePx(width = 411, height = 891)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(true)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given phone screen size and phone flag disabled, when invoked, then return false`() {
        // GIVEN
        givenRealDisplaySizePx(width = 411, height = 891)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given tablet boundary size 674x800 and flag disabled, when invoked, then return true`() {
        // GIVEN
        givenRealDisplaySizePx(width = 674, height = 800)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given below tablet boundary 673x800 and flag disabled, when invoked, then return false`() {
        // GIVEN
        givenRealDisplaySizePx(width = 673, height = 800)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given tablet display reported in landscape and flag disabled, when invoked, then return true`() {
        // GIVEN
        givenRealDisplaySizePx(width = 1280, height = 800)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given high density tablet pixels but phone-sized in dp and flag disabled, when invoked, then return false`() {
        // GIVEN
        displayMetrics.density = 2f
        givenRealDisplaySizePx(width = 800, height = 1280)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
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
