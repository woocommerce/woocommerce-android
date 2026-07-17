package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WooColorResourcesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `given light mode, when colors load, then secondary container uses the 16 percent primary container tint`() {
        val colors = loadWooColors(context = context, useDarkTheme = false)

        assertThat(colors.container.secondaryContainer)
            .isEqualTo(colors.tintLayers.primaryContainer.opacity16)
        assertThat(colors.container.secondaryContainer)
            .isNotEqualTo(colors.tintLayers.primaryContainer.opacity24)
    }

    @Test
    fun `given dark mode, when colors load, then secondary container uses the 16 percent primary tint`() {
        val colors = loadWooColors(context = context, useDarkTheme = true)

        assertThat(colors.container.secondaryContainer).isEqualTo(colors.tintLayers.primary.opacity16)
        assertThat(colors.container.secondaryContainer).isNotEqualTo(colors.tintLayers.primary.opacity24)
    }

    @Test
    fun `when colors load in both modes, then bright surface differs from default surface`() {
        listOf(false, true).forEach { useDarkTheme ->
            val colors = loadWooColors(context = context, useDarkTheme = useDarkTheme)

            assertThat(colors.surface.bright).isNotEqualTo(colors.surface.default)
        }
    }

    @Test
    fun `when Material colors project in both modes, then surface hierarchy uses distinct source roles`() {
        listOf(false, true).forEach { useDarkTheme ->
            val colors = loadWooColors(context = context, useDarkTheme = useDarkTheme)
            val materialColors = colors.toMaterialColorScheme(useDarkTheme = useDarkTheme)

            assertThat(materialColors.surface).isEqualTo(colors.surface.default)
            assertThat(materialColors.surfaceBright).isEqualTo(colors.surface.bright)
            assertThat(materialColors.surfaceDim).isEqualTo(colors.surface.surfaceDim)
            assertThat(materialColors.surfaceContainer).isEqualTo(colors.surface.default)
            assertThat(materialColors.surfaceContainerLow).isEqualTo(colors.surface.default)
            assertThat(materialColors.surfaceContainerHigh).isEqualTo(colors.surface.default)
            assertThat(materialColors.surfaceContainerHighest).isEqualTo(colors.surface.surfaceContainerHighest)
            assertThat(materialColors.surfaceContainerLowest).isEqualTo(colors.surface.bright)
        }
    }
}
