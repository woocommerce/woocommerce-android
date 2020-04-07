package com.woocommerce.android.ui.prefs

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.StyleRes
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

class WCSettingsCategoryHeaderView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.settingsCategoryHeaderStyle,
    @StyleRes defStyleRes: Int = R.style.Woo_TextView_Subtitle1
) : MaterialTextView(ctx, attrs, defStyleAttr, defStyleRes)
