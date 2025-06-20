package com.woocommerce.android.ui.orders

import android.content.Context
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.widgets.tags.ITag
import com.woocommerce.android.widgets.tags.TagConfig

class SalesChannelTag(private val salesChannelText: String) : ITag(salesChannelText.trim()) {
    override fun getTagConfiguration(context: Context): TagConfig {
        return TagConfig(context).apply {
            tagText = salesChannelText
            bgColor = ContextCompat.getColor(context, R.color.tag_bg_other)
        }
    }
}

