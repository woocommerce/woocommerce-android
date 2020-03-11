package com.woocommerce.android.ui.base

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.woocommerce.android.R

class MainContainerChild @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CoordinatorLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.container_main_child, this)
    }
}
