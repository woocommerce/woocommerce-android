package com.woocommerce.android.ui.base

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R

class MainContainerTop @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.container_main_top, this)
    }
}
