package com.woocommerce.android.ui.woopos.common.composeui.component

import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosBreakpoint

sealed interface WooPosPhoneModalStyle {
    data object Dialog : WooPosPhoneModalStyle

    data object BottomSheet : WooPosPhoneModalStyle

    class FullScreen(
        val title: String,
        val onBackClick: (() -> Unit)? = null,
    ) : WooPosPhoneModalStyle
}

internal fun WooPosPhoneModalStyle.resolveFor(breakpoint: WooPosBreakpoint): WooPosPhoneModalStyle =
    when (breakpoint) {
        WooPosBreakpoint.Phone -> this
        WooPosBreakpoint.SmallTablet,
        WooPosBreakpoint.Tablet -> WooPosPhoneModalStyle.Dialog
    }
