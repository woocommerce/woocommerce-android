package com.woocommerce.android.ui.designsystem.xml

import android.view.LayoutInflater
import android.view.ContextThemeWrapper
import androidx.annotation.StyleRes
import androidx.fragment.app.Fragment
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.DesignSystemMode
import com.woocommerce.android.ui.designsystem.defaultDesignSystemMode

fun Fragment.designSystemXmlLayoutInflater(
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
