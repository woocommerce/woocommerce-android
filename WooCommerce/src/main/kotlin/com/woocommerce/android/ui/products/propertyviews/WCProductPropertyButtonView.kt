package com.woocommerce.android.ui.products.propertyviews

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewParent
import android.widget.PopupWindow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.core.widget.NestedScrollView
import com.woocommerce.android.databinding.HighlightsTooltipLayoutBinding
import com.woocommerce.android.databinding.ProductPropertyButtonViewLayoutBinding
import com.woocommerce.android.ui.products.models.ProductProperty
import org.wordpress.android.util.DisplayUtils

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
            addOnAttachStateChangeListener(
                object : OnAttachStateChangeListener {
                    override fun onViewDetachedFromWindow(v: View) {
                        removeOnAttachStateChangeListener(this)
                    }

                    override fun onViewAttachedToWindow(v: View) {
                        showPopupWindowWhenEnoughSpace(
                            scrollableView = parent?.findParentNestedScrollView()
                                ?: error("No NestedScrollView found in parent hierarchy needed to show AI tooltip"),
                            tooltip = it,
                        )
                        removeOnAttachStateChangeListener(this)
                    }

                    fun ViewParent.findParentNestedScrollView(): NestedScrollView? =
                        if (this is NestedScrollView) {
                            this
                        } else {
                            parent?.findParentNestedScrollView()
                        }
                }
            )
        }
    }

    private fun showPopupWindowWhenEnoughSpace(
        scrollableView: NestedScrollView,
        tooltip: ProductProperty.Button.Tooltip
    ) {
        if (isEnoughSpaceToShowTooltip()) {
            showTooltip(scrollableView.measuredWidth, tooltip)
            return
        }
        scrollableView.setOnScrollChangeListener(
            NestedScrollView.OnScrollChangeListener { _, _, _, _, _ ->
                if (isEnoughSpaceToShowTooltip()) {
                    scrollableView.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
                    showTooltip(
                        parentViewWidth = scrollableView.measuredWidth,
                        tooltip = tooltip,
                    )
                }
            }
        )
    }

    private fun isEnoughSpaceToShowTooltip(): Boolean {
        val locationOnScreen = IntArray(2)
        getLocationOnScreen(locationOnScreen)
        val topPadding = context.resources.displayMetrics.heightPixels -
            DisplayUtils.dpToPx(context, TOOLTIP_BOTTOM_OFFSET_BEFORE_SHOWING_DP)
        return locationOnScreen[1] < topPadding
    }

    private fun showTooltip(
        parentViewWidth: Int,
        tooltip: ProductProperty.Button.Tooltip
    ) {
        val popupBinding = HighlightsTooltipLayoutBinding.inflate(LayoutInflater.from(context), null, false)

        val popupWindow = PopupWindow(
            popupBinding.root,
            parentViewWidth,
            LayoutParams.WRAP_CONTENT
        )

        popupBinding.tooltipTitle.text = context.getString(tooltip.title)
        popupBinding.tooltipMessage.text = context.getString(tooltip.text)
        popupBinding.tooltipDismissButton.text = context.getString(tooltip.dismissButtonText)

        // Make the popupWindow dismissible by clicking outside of it.
        popupWindow.isOutsideTouchable = true
        popupWindow.isFocusable = true

        popupBinding.tooltipDismissButton.setOnClickListener {
            tooltip.onDismiss.invoke()
            popupWindow.dismiss()
        }
        popupWindow.showAsDropDown(
            binding.productButton,
            0,
            DisplayUtils.dpToPx(context, TOOLTIP_VERTICAL_OFFSET)
        )
    }

    private companion object {
        const val TOOLTIP_VERTICAL_OFFSET = -8
        const val TOOLTIP_BOTTOM_OFFSET_BEFORE_SHOWING_DP = 230
    }
}
