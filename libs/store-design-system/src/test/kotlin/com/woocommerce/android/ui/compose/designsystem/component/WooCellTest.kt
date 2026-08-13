package com.woocommerce.android.ui.compose.designsystem.component

import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.ui.compose.designsystem.foundation.loadWooColors
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooCellTest {
    private val colors = loadWooColors(
        context = ApplicationProvider.getApplicationContext(),
        useDarkTheme = false,
    )

    @Test
    fun `given enabled cell, when style resolves, then shell and slots match live bindings`() {
        val style = wooCellStyle(enabled = true, colors = colors)

        assertThat(style.containerColor).isEqualTo(colors.surface.bright)
        assertThat(style.slotContentColor).isEqualTo(colors.surface.onVariant)
    }

    @Test
    fun `given disabled cell, when style resolves, then slots use lowest variant`() {
        val style = wooCellStyle(enabled = false, colors = colors)

        assertThat(style.containerColor).isEqualTo(colors.surface.bright)
        assertThat(style.slotContentColor).isEqualTo(colors.surface.onVariantLowest)
    }
}
