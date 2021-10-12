package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.woocommerce.android.databinding.WcEditableEmptyLabelBinding

class WCEditableEmptyLabel @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : FrameLayout(ctx, attrs) {
    private val binding = WcEditableEmptyLabelBinding.inflate(LayoutInflater.from(context), this, true)

}
