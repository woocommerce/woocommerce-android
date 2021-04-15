package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewBannerWarningBinding

class WCWarningBanner @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    private val binding = ViewBannerWarningBinding.inflate(LayoutInflater.from(context), this)

    var title: CharSequence?
        get() = binding.warningTitle.text
        set(value) {
            binding.warningTitle.text = value
            binding.warningTitle.isVisible = !value.isNullOrEmpty()
        }

    var message: CharSequence?
        get() = binding.warningMessage.text
        set(value) {
            binding.warningMessage.text = value
            binding.warningMessage.isVisible = !value.isNullOrEmpty()
        }

    init {
        setBackgroundResource(R.color.warning_banner_background_color)
        if (attrs != null) {
            context.obtainStyledAttributes(attrs, R.styleable.WCWarningBanner).use {
                title = it.getString(R.styleable.WCWarningBanner_title)
                message = it.getString(R.styleable.WCWarningBanner_message)
            }
        }
    }
}
