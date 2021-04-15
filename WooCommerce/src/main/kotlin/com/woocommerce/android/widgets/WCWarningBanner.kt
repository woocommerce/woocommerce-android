package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewBannerWarningBinding

class WCWarningBanner(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = ViewBannerWarningBinding.inflate(LayoutInflater.from(context), this)

    var text: CharSequence
        get() = binding.warningMessage.text
        set(value) {
            binding.warningMessage.text = value
        }

    init {
        setBackgroundResource(R.color.warning_banner_background_color)
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.WCWarningBanner).use {
                binding.warningMessage.text = it.getString(R.styleable.WCWarningBanner_text).orEmpty()
            }
        }
    }
}
