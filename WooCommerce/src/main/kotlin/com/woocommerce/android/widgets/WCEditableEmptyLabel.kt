package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.woocommerce.android.databinding.WcEditableEmptyLabelBinding

class WCEditableEmptyLabel @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FrameLayout(ctx, attrs) {
    private val binding = WcEditableEmptyLabelBinding.inflate(LayoutInflater.from(context), this, true)

    fun setText(text: String, @StringRes emptyTextId: Int) {
        if (text.isNotEmpty()) {
            binding.editTextView.visibility = View.VISIBLE
            binding.emptyLabel.visibility = View.GONE
            binding.editTextView.setText(text)
        } else {
            binding.emptyLabel.visibility = View.VISIBLE
            binding.editTextView.visibility = View.GONE
            binding.emptyLabel.setText(emptyTextId)
        }
    }
}
