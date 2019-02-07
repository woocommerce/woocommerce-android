package com.woocommerce.android.widgets

import android.content.Context
import android.os.Handler
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.view.Gravity
import android.view.View
import com.tooltip.Tooltip
import com.woocommerce.android.R
import com.woocommerce.android.widgets.WCFeatureTooltip.Feature.SITE_SWITCHER
import java.lang.ref.WeakReference

/**
 * Used to "advertise" a new feature with a tooltip
 */
object WCFeatureTooltip {
    private const val TOOLTIP_DELAY = 2500L
    private const val PREF_NAME = "feature_tooltip"

    enum class Feature {
        SITE_SWITCHER
    }

    fun showIfNeeded(feature: Feature, anchorView: View) {
        val prefs = anchorView.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(feature.name, false)) {
            return
        }

        val weakAnchorView = WeakReference(anchorView)
        Handler().postDelayed({
            weakAnchorView.get()?.let {
                show(feature, it)
                prefs.edit().putBoolean(feature.name, true).apply()
            }
        }, TOOLTIP_DELAY)
    }

    private fun show(feature: Feature, anchorView: View) {
        val context = anchorView.context
        val bgColor = ContextCompat.getColor(context, R.color.wc_purple)
        val textColor = ContextCompat.getColor(context, R.color.white)
        val padding = context.resources.getDimensionPixelSize(R.dimen.margin_large)

        @StringRes val messageId = when (feature) {
            SITE_SWITCHER -> R.string.tooltip_site_switcher
        }

        val tooltip = Tooltip.Builder(anchorView)
                .setBackgroundColor(bgColor)
                .setTextColor(textColor)
                .setPadding(padding)
                .setGravity(Gravity.BOTTOM)
                .setText(messageId)
                .show()

        // This assumes the anchor view has a background ripple
        anchorView.isPressed = true

        val weakAnchorView = WeakReference(anchorView)
        val weakTooltip = WeakReference(tooltip)
        Handler().postDelayed({
            weakAnchorView.get()?.isPressed = false
            weakTooltip.get()?.dismiss()
        }, TOOLTIP_DELAY)
    }
}
