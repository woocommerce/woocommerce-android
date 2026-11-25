package com.woocommerce.android.ui.woopos.common.composeui.component

import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosPhoneModalStyleTest {

    @Test
    fun `given phone breakpoint, when resolving bottom sheet, then bottom sheet is used`() {
        // When
        val result = WooPosPhoneModalStyle.BottomSheet.resolveFor(WooPosBreakpoint.Phone)

        // Then
        assertThat(result).isEqualTo(WooPosPhoneModalStyle.BottomSheet)
    }

    @Test
    fun `given phone breakpoint, when resolving full screen, then the same full screen style is used`() {
        // Given
        val style = WooPosPhoneModalStyle.FullScreen(title = "Title")

        // When
        val result = style.resolveFor(WooPosBreakpoint.Phone)

        // Then
        assertThat(result).isSameAs(style)
    }

    @Test
    fun `given small tablet breakpoint, when resolving any phone style, then dialog is used`() {
        // When
        val results = listOf(
            WooPosPhoneModalStyle.BottomSheet,
            WooPosPhoneModalStyle.FullScreen(title = "Title"),
        ).map { it.resolveFor(WooPosBreakpoint.SmallTablet) }

        // Then
        assertThat(results).containsOnly(WooPosPhoneModalStyle.Dialog)
    }

    @Test
    fun `given tablet breakpoint, when resolving any phone style, then dialog is used`() {
        // When
        val results = listOf(
            WooPosPhoneModalStyle.BottomSheet,
            WooPosPhoneModalStyle.FullScreen(title = "Title"),
        ).map { it.resolveFor(WooPosBreakpoint.Tablet) }

        // Then
        assertThat(results).containsOnly(WooPosPhoneModalStyle.Dialog)
    }
}
