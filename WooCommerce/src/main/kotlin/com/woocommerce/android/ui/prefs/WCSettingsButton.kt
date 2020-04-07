package com.woocommerce.android.ui.prefs

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.woocommerce.android.R

class WCSettingsButton @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.settingsButtonStyle
) : MaterialButton(ctx, attrs, defStyleAttr)
