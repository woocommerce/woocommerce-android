package com.woocommerce.android.ui.products

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.databinding.HighlightsTooltipLayoutBinding
import com.woocommerce.android.databinding.ProductPropertyButtonViewLayoutBinding

class WCProductPropertyButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private val binding = ProductPropertyButtonViewLayoutBinding.inflate(LayoutInflater.from(context), this)

    fun show(
        text: String,
        icon: Drawable?,
        onClick: () -> Unit,
    ) {
        with(binding.productButton) {
            this.text = text
            this.icon = icon
            setOnClickListener { onClick() }
        }

        val popupBinding = HighlightsTooltipLayoutBinding.inflate(LayoutInflater.from(context), null, false)
        showPopupWindow(popupBinding)
    }

    private fun showPopupWindow(popupBinding: HighlightsTooltipLayoutBinding) {
        val popupWindow = PopupWindow(popupBinding.root, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        popupBinding.tooltipDismissButton.setOnClickListener { popupWindow.dismiss() }

        // Make the popupWindow dismissible by clicking outside of it.
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Show the PopupWindow below the button
        popupWindow.showAsDropDown(binding.productButton)
    }

    fun show(
        text: String,
        icon: Drawable?,
        onClick: () -> Unit,
        linkText: Spanned,
        onLinkClick: () -> Unit,
    ) {
        with(binding.productButton) {
            this.text = text
            this.icon = icon
            setOnClickListener { onClick() }
        }

        with(binding.link) {
            this.text = linkText
            setOnClickListener { onLinkClick() }
        }

        val popupBinding = HighlightsTooltipLayoutBinding.inflate(LayoutInflater.from(context), null, false)
        showPopupWindow(popupBinding)
    }
}
