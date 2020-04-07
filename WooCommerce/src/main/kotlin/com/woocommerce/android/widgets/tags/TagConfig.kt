package com.woocommerce.android.widgets.tags

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.woocommerce.android.R

class TagConfig(context: Context) {
    var tagText = ""
    @ColorInt var fgColor = ContextCompat.getColor(context, R.color.tagView_text)
    @ColorInt var bgColor = ContextCompat.getColor(context, R.color.tagView_bg)
    @ColorInt var borderColor = ContextCompat.getColor(context, R.color.tagView_border_bg)
}
