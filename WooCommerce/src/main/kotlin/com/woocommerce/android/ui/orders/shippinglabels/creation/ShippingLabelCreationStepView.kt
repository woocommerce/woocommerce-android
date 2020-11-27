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
            val dividerVisible = typedArray.getBoolean(
                R.styleable.ShippingLabelCreationStepView_divider_visible,
                true
            )
            val continueButtonVisible = typedArray.getBoolean(
                R.styleable.ShippingLabelCreationStepView_continue_button_visible,
                false
            )
            val editButtonVisible = typedArray.getBoolean(
                R.styleable.ShippingLabelCreationStepView_edit_button_visible,
                false
            )
            if (captionText.isNullOrEmpty() || detailsText.isNullOrEmpty() || iconRes == 0) {
                throw IllegalArgumentException("ShippingLabelCreationStepView must have caption, details and icon")
            }
            caption = captionText
            details = detailsText
            icon = iconRes
            isViewEnabled = isEnabled
            isContinueButtonVisible = continueButtonVisible
            isEditButtonVisible = editButtonVisible
            isDividerVisible = dividerVisible
        }
    }

    var isViewEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                setForegroundColor(ContextCompat.getColor(context, R.color.color_on_surface_disabled))
            } else {
                resetColors()
            }
        }

    var isDividerVisible: Boolean = false
        set(value) {
            field = value
            divider.isVisible = value
        }

    var isContinueButtonVisible: Boolean = false
        set(value) {
            field = value
            continueButton.isVisible = value
        }

    var isEditButtonVisible: Boolean = false
        set(value) {
            field = value
            editButton.isVisible = value
        }

    var caption: String = ""
        set(value) {
            field = value
            captionTextView?.text = value
        }

    var details: String = ""
        set(value) {
            field = value
            detailsTextView?.text = value
        }

    @DrawableRes var icon: Int = 0
        set(value) {
            field = value
            if (icon != 0) {
                this.iconImageView?.setImageDrawable(ContextCompat.getDrawable(context, value))
            }
        }

    var continueButtonClickListener: (() -> Unit) = {}
        set(value) {
            field = value
            continueButton.setOnClickListener { value() }
        }

    var editButtonClickListener: (() -> Unit) = {}
        set(value) {
            field = value
            editButton.setOnClickListener { value() }
        }

    private fun setForegroundColor(@ColorInt color: Int) {
        detailsTextView?.tag = detailsTextView?.currentTextColor
        detailsTextView?.setTextColor(color)

        captionTextView?.tag = captionTextView?.currentTextColor
        captionTextView?.setTextColor(color)

        iconImageView?.setColorFilter(color, SRC_IN)
    }

    private fun resetColors() {
        (detailsTextView?.tag as? Int)?.let { detailsTextView?.setTextColor(it) }
        (captionTextView?.tag as? Int)?.let { captionTextView?.setTextColor(it) }
        iconImageView?.clearColorFilter()
    }
}

