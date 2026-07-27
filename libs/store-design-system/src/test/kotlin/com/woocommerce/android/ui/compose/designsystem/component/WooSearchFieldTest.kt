package com.woocommerce.android.ui.compose.designsystem.component

import androidx.test.core.app.ApplicationProvider
import com.woocommerce.android.ui.compose.designsystem.foundation.loadWooColors
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooSearchFieldTest {
    private val colors = loadWooColors(
        context = ApplicationProvider.getApplicationContext(),
        useDarkTheme = false,
    )

    @Test
    fun `given enabled search, when style resolves, then live color bindings are used`() {
        val style = wooSearchFieldStyle(enabled = true, colors = colors)

        assertThat(style.shellColor).isEqualTo(colors.surface.bright)
        assertThat(style.clearIconColor).isEqualTo(colors.surface.onDefault)
        assertThat(style.placeholderColor).isEqualTo(colors.stateLayers.onSurface.opacity24)
    }
}
