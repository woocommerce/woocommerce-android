package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.shipping_label_creation_step.view.*

class ShippingLabelCreationStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    init {
        View.inflate(context, R.layout.shipping_label_creation_step, this)

        context.obtainStyledAttributes(attrs, R.styleable.ShippingLabelCreationStepView, 0, 0).use { typedArray ->
            val captionText = typedArray.getString(R.styleable.ShippingLabelCreationStepView_caption)
            val detailsText = typedArray.getString(R.styleable.ShippingLabelCreationStepView_details)
            val iconRes = typedArray.getResourceId(R.styleable.ShippingLabelCreationStepView_icon, 0)
            val isEnabled = typedArray.getBoolean(R.styleable.ShippingLabelCreationStepView_android_enabled, true)
            val isButtonVisible = typedArray.getBoolean(R.styleable.ShippingLabelCreationStepView_button_visible, false)
            if (captionText.isNullOrEmpty() || detailsText.isNullOrEmpty() || iconRes == 0) {
                throw IllegalArgumentException("ShippingLabelCreationStepView must have caption, details and icon")
            }
            caption?.text = captionText
            details?.text = detailsText
            this.icon?.setImageDrawable(ContextCompat.getDrawable(context, iconRes))
            if (!isEnabled) {
                setForegroundColor(ContextCompat.getColor(context, R.color.color_on_surface_disabled))
            } else {
                resetColors()
            }
            continueButton.isVisible = isButtonVisible
        }
    }

    fun update(caption: String? = null, details: String? = null, @DrawableRes icon: Int? = null) {
        caption?.let { this.caption?.text = it }
        details?.let { this.details?.text = it }
        icon?.let { this.icon?.setImageDrawable(ContextCompat.getDrawable(context, it)) }
    }

    private fun setForegroundColor(@ColorInt color: Int) {
        details?.tag = details?.currentTextColor
        details?.setTextColor(color)

        caption?.tag = caption?.currentTextColor
        caption?.setTextColor(color)

        icon?.setColorFilter(color, SRC_IN)
    }

    private fun resetColors() {
        (details?.tag as? Int)?.let { details?.setTextColor(it) }
        (caption?.tag as? Int)?.let { caption?.setTextColor(it) }
        icon?.clearColorFilter()
    }
}

