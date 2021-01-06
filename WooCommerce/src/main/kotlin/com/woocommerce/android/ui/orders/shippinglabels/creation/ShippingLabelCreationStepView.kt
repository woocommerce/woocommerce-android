package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.content.Context
import android.graphics.PorterDuff.Mode.SRC_IN
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ShippingLabelCreationStepBinding

class ShippingLabelCreationStepView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val binding = ShippingLabelCreationStepBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        context.obtainStyledAttributes(attrs, R.styleable.ShippingLabelCreationStepView, 0, 0).use { typedArray ->
            val captionText = typedArray.getString(R.styleable.ShippingLabelCreationStepView_caption)
            val detailsText = typedArray.getString(R.styleable.ShippingLabelCreationStepView_details)
            val iconRes = typedArray.getResourceId(R.styleable.ShippingLabelCreationStepView_icon, 0)
            val isEnabled = typedArray.getBoolean(R.styleable.ShippingLabelCreationStepView_android_enabled, true)
            val highlighted = typedArray.getBoolean(R.styleable.ShippingLabelCreationStepView_highlighted, false)
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
            caption = captionText ?: ""
            details = detailsText ?: ""
            icon = iconRes
            isViewEnabled = isEnabled
            isContinueButtonVisible = continueButtonVisible
            isEditButtonVisible = editButtonVisible
            isDividerVisible = dividerVisible
            isHighlighted = highlighted
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

    var isHighlighted: Boolean = false
        set(value) {
            field = value
            if (!value) {
                setIconColor(ContextCompat.getColor(context, R.color.color_on_surface_high))
            } else {
                resetColors()
            }
        }

    var isDividerVisible: Boolean = false
        set(value) {
            field = value
            binding.divider.isVisible = value
        }

    var isContinueButtonVisible: Boolean = false
        set(value) {
            field = value
            binding.continueButton.isVisible = value
        }

    var isEditButtonVisible: Boolean = false
        set(value) {
            field = value
            binding.editButton.isVisible = value
        }

    var caption: String = ""
        set(value) {
            field = value
            binding.captionTextView.text = value
        }

    var details: String = ""
        set(value) {
            field = value
            binding.detailsTextView.text = value
        }

    @DrawableRes var icon: Int = 0
        set(value) {
            field = value
            if (icon != 0) {
                binding.iconImageView.setImageDrawable(ContextCompat.getDrawable(context, value))
            }
        }

    var continueButtonClickListener: (() -> Unit) = {}
        set(value) {
            field = value
            binding.continueButton.setOnClickListener { value() }
        }

    var editButtonClickListener: (() -> Unit) = {}
        set(value) {
            field = value
            binding.editButton.setOnClickListener { value() }
        }

    private fun setForegroundColor(@ColorInt color: Int) {
        setTextColor(color)
        setIconColor(color)
    }

    private fun setIconColor(color: Int) {
        binding.iconImageView.setColorFilter(color, SRC_IN)
    }

    private fun setTextColor(color: Int) {
        binding.detailsTextView.tag = binding.detailsTextView.currentTextColor
        binding.detailsTextView.setTextColor(color)

        binding.captionTextView.tag = binding.captionTextView.currentTextColor
        binding.captionTextView.setTextColor(color)
    }

    private fun resetColors() {
        (binding.detailsTextView.tag as? Int)?.let { binding.detailsTextView.setTextColor(it) }
        (binding.captionTextView.tag as? Int)?.let { binding.captionTextView.setTextColor(it) }
        binding.iconImageView.clearColorFilter()
    }
}
