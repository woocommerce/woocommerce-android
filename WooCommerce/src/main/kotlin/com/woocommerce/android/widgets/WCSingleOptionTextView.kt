package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.AttrRes
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R

/**
 * Custom [MaterialTextView] for displaying single choice items. By default will display a "right arrow" as the
 * [drawableEnd]. This view has the appropriate style already assigned at the theme level.
 */
class WCSingleOptionTextView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = R.attr.wcSingleOptionTextViewStyle
) : MaterialTextView(ctx, attrs, defStyleAttr)
