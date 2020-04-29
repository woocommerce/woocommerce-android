package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.woocommerce.android.R

class WCToggleOutlinedButton @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.wcToggleOutlinedButtonStyle
) : MaterialButton(ctx, attrs, defStyleAttr)
