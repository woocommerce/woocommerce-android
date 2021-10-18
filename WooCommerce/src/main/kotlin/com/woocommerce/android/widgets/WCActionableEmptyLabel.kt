package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StringRes
import com.woocommerce.android.databinding.WcActionableEmptyLabelBinding

/**
 * Simple ViewGroup which contains two views:
 * 1. emptyLabel - button-styled TextView which appears when the text is empty, used as a call to action
 * 2. notEmptyLabel - standard TextView which appears when the text is NOT empty
 */
class WCActionableEmptyLabel @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FrameLayout(ctx, attrs) {
    private val binding = WcActionableEmptyLabelBinding.inflate(LayoutInflater.from(context), this, true)

    /**
     * @param text - the text to appear if it's not empty
     * @param emptyTextId - string id of the text to appear if text is empty
     */
    fun setText(text: String, @StringRes emptyTextId: Int) {
        if (text.isEmpty()) {
            binding.emptyLabel.setText(emptyTextId)
            binding.emptyLabel.visibility = View.VISIBLE
            binding.notEmptyLabel.visibility = View.GONE
        } else {
            binding.notEmptyLabel.text = text
            binding.notEmptyLabel.visibility = View.VISIBLE
            binding.emptyLabel.visibility = View.GONE
        }
    }

    /**
     * When the view is read-only, we make the text selectable and hide the pencil icon
     */
    fun setIsReadOnly(readOnly: Boolean) {
        binding.notEmptyLabel.setTextIsSelectable(readOnly)
        if (readOnly) {
            binding.notEmptyLabel.setCompoundDrawables(null, null, null, null)
        }
    }
}
