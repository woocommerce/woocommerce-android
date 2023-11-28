package com.woocommerce.android.ui.prefs

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

class WCSettingsCategoryHeaderView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.settingsCategoryHeaderStyle
) : MaterialTextView(ctx, attrs, defStyleAttr)
