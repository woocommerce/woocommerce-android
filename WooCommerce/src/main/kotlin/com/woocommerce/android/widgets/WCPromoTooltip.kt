package com.woocommerce.android.widgets

import android.content.Context
import android.os.Handler
import android.view.Gravity
import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.tooltip.Tooltip
import com.woocommerce.android.R
import java.lang.ref.WeakReference

/**
 * Used to advertise a new feature with a single-shot tooltip
 */
object WCPromoTooltip {
    private const val TOOLTIP_DELAY_BEFORE_SHOWING = 1000L
    private const val TOOLTIP_DELAY_BEFORE_HIDING = 3000L
    private const val PREF_NAME = "promo_tooltip"

    /*
     * we include the version the tooltip was added both as reference to us and to provide a future means
     * to programmatically disable tooltips after a certain number of versions
     */
    enum class Feature(val prefKeyName: String, @StringRes val messageId: Int, val addedInversionCode: Int) {
        SITE_SWITCHER("key_site_switcher", R.string.tooltip_site_switcher, 34)
    }

    /**
     * Shows the tooltip for the passed feature if it hasn't already been shown
     */
    fun showIfNeeded(feature: Feature, anchorView: View) {
        if (isTooltipShown(anchorView.context, feature)) {
            return
        }

        // use a weak reference in case the context is no longer valid after the delay
        val weakAnchorView = WeakReference(anchorView)

        // show it after a brief delay so it doesn't appear immediately after user enters the activity
        Handler().postDelayed({
            weakAnchorView.get()?.let {
                show(feature, it)
                setTooltipShown(it.context, feature, true)
            }
        }, TOOLTIP_DELAY_BEFORE_SHOWING)
    }

    private fun getPrefs(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun isTooltipShown(context: Context, feature: Feature) =
            getPrefs(context).getBoolean(feature.prefKeyName, false)

    fun setTooltipShown(context: Context, feature: Feature, shown: Boolean) {
        getPrefs(context).edit().putBoolean(feature.prefKeyName, shown).apply()
    }

    private fun show(feature: Feature, anchorView: View) {
        val context = anchorView.context
        val bgColor = ContextCompat.getColor(context, R.color.grey_dark)
        val textColor = ContextCompat.getColor(context, R.color.white)
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_large)

        val tooltip = Tooltip.Builder(anchorView)
                .setBackgroundColor(bgColor)
                .setTextColor(textColor)
                .setPadding(padding)
                .setGravity(Gravity.BOTTOM)
                .setText(feature.messageId)
                .show()

        // This assumes the anchor view will show a background ripple when pressed
        anchorView.isPressed = true

        val weakAnchorView = WeakReference(anchorView)
        val weakTooltip = WeakReference(tooltip)

        Handler().postDelayed({
            weakAnchorView.get()?.isPressed = false
            weakTooltip.get()?.dismiss()
        }, TOOLTIP_DELAY_BEFORE_HIDING)
    }
}
