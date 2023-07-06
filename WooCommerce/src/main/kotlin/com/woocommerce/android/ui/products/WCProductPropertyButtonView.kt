package com.woocommerce.android.ui.products

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import com.woocommerce.android.databinding.HighlightsTooltipLayoutBinding
import com.woocommerce.android.databinding.ProductPropertyButtonViewLayoutBinding
import com.woocommerce.android.ui.products.models.ProductProperty
import org.wordpress.android.util.DisplayUtils

class WCProductPropertyButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    companion object {
        const val TOOLTIP_VERTICAL_OFFSET = -8
    }
    private val binding = ProductPropertyButtonViewLayoutBinding.inflate(LayoutInflater.from(context), this)

    fun show(
        text: String,
        icon: Drawable?,
        onClick: () -> Unit,
        link: ProductProperty.Button.Link? = null,
        tooltip: ProductProperty.Button.Tooltip? = null,
    ) {
        with(binding.productButton) {
            this.text = text
            this.icon = icon
            setOnClickListener { onClick() }
        }

        link?.let { linkData ->
            with(binding.link) {
                this.text = HtmlCompat.fromHtml(context.getString(linkData.text), HtmlCompat.FROM_HTML_MODE_LEGACY)
                setOnClickListener {
                    linkData.onClick()
                }
            }
        }

        tooltip?.let {
            showPopupWindow(it)
        }
    }

    private fun showPopupWindow(tooltip: ProductProperty.Button.Tooltip) {
        val popupBinding = HighlightsTooltipLayoutBinding.inflate(LayoutInflater.from(context), null, false)

        val popupWindow = PopupWindow(popupBinding.root, LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

        popupBinding.tooltipTitle.text = context.getString(tooltip.title)
        popupBinding.tooltipMessage.text = context.getString(tooltip.text)
        popupBinding.tooltipDismissButton.text = context.getString(tooltip.primaryButtonText)

        tooltip.onPrimaryButtonClick?.let {
            popupBinding.tooltipDismissButton.setOnClickListener { it() }
        } ?: run {
            popupBinding.tooltipDismissButton.setOnClickListener { popupWindow.dismiss() }
        }

        // Make the popupWindow dismissible by clicking outside of it.
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        // Show the PopupWindow below the button
        popupWindow.showAsDropDown(
            binding.productButton,
            0,
            DisplayUtils.dpToPx(context, TOOLTIP_VERTICAL_OFFSET)
        )
    }
}
