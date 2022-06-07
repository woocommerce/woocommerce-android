package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ViewStepperBinding

class StepperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {
    private val binding = ViewStepperBinding.inflate(LayoutInflater.from(context), this)
    private var plusMinusContentDescription: Int? = null

    var value: Int
        get() = binding.valueText.text.toString().toIntOrNull() ?: 0
        set(value) {
            val text = value.toString()
            if (text != binding.valueText.text) {
                binding.valueText.text = text
                plusMinusContentDescription?.let {
                    binding.minusButton.contentDescription = context.getString(
                        it,
                        value,
                        value - 1
                    )
                    binding.plusButton.contentDescription = context.getString(
                        it,
                        value,
                        value + 1
                    )
                }
            }
        }

    var isPlusButtonEnabled: Boolean
        get() = binding.plusButton.isEnabled
        set(value) {
            binding.plusButton.isEnabled = value
        }

    var isMinusButtonEnabled: Boolean
        get() = binding.minusButton.isEnabled
        set(value) {
            binding.minusButton.isEnabled = value
        }

    init {
        background = ContextCompat.getDrawable(context, R.drawable.bg_stepper_view)
        orientation = HORIZONTAL
    }

    fun init(
        currentValue: Int = 0,
        onPlusButtonClick: () -> Unit,
        onMinusButtonClick: () -> Unit,
        @StringRes plusMinusContentDescription: Int? = null
    ) {
        value = currentValue
        this.plusMinusContentDescription = plusMinusContentDescription
        binding.plusButton.setOnClickListener { onPlusButtonClick() }
        binding.minusButton.setOnClickListener { onMinusButtonClick() }
    }
}
