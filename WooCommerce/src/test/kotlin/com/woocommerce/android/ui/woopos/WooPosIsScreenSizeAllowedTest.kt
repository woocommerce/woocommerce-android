package com.woocommerce.android.ui.woopos

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosIsScreenSizeAllowedTest {

    private val context: Context = mock()
    private val resources: Resources = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val displayMetrics = DisplayMetrics().apply { density = 1f }

    private lateinit var sut: WooPosIsScreenSizeAllowed

    @Before
    fun setup() {
        whenever(context.resources).thenReturn(resources)
        whenever(resources.displayMetrics).thenReturn(displayMetrics)

        sut = WooPosIsScreenSizeAllowed(
            context = context,
            featureFlagRepository = featureFlagRepository,
            wooPosLog = mock()
        )
    }

    @Test
    fun `given tablet screen size, when invoked, then return true`() {
        // GIVEN
        displayMetrics.widthPixels = 800
        displayMetrics.heightPixels = 1280

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given phone screen size and phone flag enabled, when invoked, then return true`() {
        // GIVEN
        displayMetrics.widthPixels = 411
        displayMetrics.heightPixels = 891
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(true)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given phone screen size and phone flag disabled, when invoked, then return false`() {
        // GIVEN
        displayMetrics.widthPixels = 411
        displayMetrics.heightPixels = 891
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given tablet boundary size 674x800 and flag disabled, when invoked, then return true`() {
        // GIVEN
        displayMetrics.widthPixels = 674
        displayMetrics.heightPixels = 800
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given below tablet boundary 673x800 and flag disabled, when invoked, then return false`() {
        // GIVEN
        displayMetrics.widthPixels = 673
        displayMetrics.heightPixels = 800
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given high density tablet pixels but phone-sized in dp and flag disabled, when invoked, then return false`() {
        // GIVEN
        displayMetrics.density = 2f
        displayMetrics.widthPixels = 800
        displayMetrics.heightPixels = 1280
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_PHONE)).thenReturn(false)

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).isFalse()
    }
}
