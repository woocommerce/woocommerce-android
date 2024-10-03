package com.woocommerce.android.ui.whatsnew

import android.content.Context
import android.content.res.Resources
import android.util.DisplayMetrics
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FeatureAnnouncementDisplayAsDialogTest {
    private val mockContext: Context = mock()
    private val mockResources: Resources = mock()

    private val displayAsDialog = FeatureAnnouncementDisplayAsDialog(mockContext)

    @Test
    fun `given screen short size is greater than or equal to 674, when invoke, then returns true`() {
        // GIVEN
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1348
            heightPixels = 1600
            density = 2.0f
        }
        setupMockContext(displayMetrics)

        // WHEN
        val result = displayAsDialog()

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given screen short size is less than 674, when invoke, then returns false`() {
        // GIVEN
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 600
            heightPixels = 500
            density = 2.0f
        }
        setupMockContext(displayMetrics)

        // WHEN
        val result = displayAsDialog()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given landscape and small screen, when invoke, then invoke returns false`() {
        // GIVEN
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 1348
            heightPixels = 800
            density = 2.0f
        }
        setupMockContext(displayMetrics)

        // WHEN
        val result = displayAsDialog()

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given portrait and small screen, when invoke, then returns false`() {
        // GIVEN
        val displayMetrics = DisplayMetrics().apply {
            widthPixels = 800
            heightPixels = 1348
            density = 2.0f
        }
        setupMockContext(displayMetrics)

        // WHEN
        val result = displayAsDialog()

        // THEN
        assertThat(result).isFalse()
    }

    private fun setupMockContext(displayMetrics: DisplayMetrics) {
        whenever(mockContext.resources).thenReturn(mockResources)
        whenever(mockResources.displayMetrics).thenReturn(displayMetrics)
    }
}
