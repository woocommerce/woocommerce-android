package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.woocommerce.android.databinding.WcActionableEmptyLabelBinding

class WCActionableEmptyLabel @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FrameLayout(ctx, attrs) {
    private val binding = WcActionableEmptyLabelBinding.inflate(LayoutInflater.from(context), this, true)

    fun setText(text: String, @StringRes emptyTextId: Int) {
        if (text.isNotEmpty()) {
            binding.notEmptyLabel.visibility = View.VISIBLE
            binding.emptyLabel.visibility = View.GONE
            binding.notEmptyLabel.text = text
        } else {
            binding.emptyLabel.visibility = View.VISIBLE
            binding.notEmptyLabel.visibility = View.GONE
            binding.emptyLabel.setText(emptyTextId)
        }
    }

    fun setTextIsSelectable(value: Boolean) {
        binding.notEmptyLabel.setTextIsSelectable(value)
    }
}
