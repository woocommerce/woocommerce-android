package com.woocommerce.android.ui.designsystem.xml

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.appcompat.widget.Toolbar
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.DesignSystemMode
import com.woocommerce.android.ui.designsystem.defaultDesignSystemMode

fun Fragment.designSystemXmlLayoutInflater(
    inflater: LayoutInflater,
    mode: DesignSystemMode = defaultDesignSystemMode(),
    @StyleRes themeOverlay: Int = R.style.ThemeOverlay_Woo_DesignSystem_Xml,
): LayoutInflater =
    requireContext().designSystemXmlLayoutInflater(inflater, mode, themeOverlay)

fun Context.designSystemXmlLayoutInflater(
    inflater: LayoutInflater,
    mode: DesignSystemMode = defaultDesignSystemMode(),
    @StyleRes themeOverlay: Int = R.style.ThemeOverlay_Woo_DesignSystem_Xml,
): LayoutInflater =
    when (mode) {
        DesignSystemMode.LEGACY -> inflater
        DesignSystemMode.DESIGN_SYSTEM -> inflater.cloneInContext(
            ContextThemeWrapper(inflater.context, themeOverlay)
        )
    }

inline fun <T> Fragment.withDesignSystemXmlLayoutInflater(
    inflater: LayoutInflater,
    mode: DesignSystemMode = defaultDesignSystemMode(),
    @StyleRes themeOverlay: Int = R.style.ThemeOverlay_Woo_DesignSystem_Xml,
    block: (LayoutInflater) -> T,
): T = block(designSystemXmlLayoutInflater(inflater, mode, themeOverlay))

fun Context.designSystemToolbarLayoutInflater(
    inflater: LayoutInflater,
    mode: DesignSystemMode = defaultDesignSystemMode(),
): LayoutInflater = designSystemXmlLayoutInflater(
    inflater = inflater,
    mode = mode,
    themeOverlay = R.style.ThemeOverlay_Woo_DesignSystem_Toolbar,
)

fun Toolbar.applyDesignSystemToolbarLayout(mode: DesignSystemMode) {
    if (mode == DesignSystemMode.LEGACY) return

    updateLayoutParams<ViewGroup.LayoutParams> {
        height = resources.getDimensionPixelSize(R.dimen.design_system_toolbar_height)
    }
}
