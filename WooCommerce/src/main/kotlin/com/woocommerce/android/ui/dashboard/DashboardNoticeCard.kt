package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.dashboard_notice.view.*

/**
 * Dashboard card that displays a notice with a title and a message, as well as an optional action button.
 */
class DashboardNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null
) : MaterialCardView(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_notice, this)
    }

    /**
     * Sets the display text for the notice, and optionally configures the button.
     *
     * @param title label for the notice title
     * @param message label for the notice message
     * @param buttonLabel label for the button - [buttonAction] must be set for the button to be displayed
     * @param buttonAction callback onClick for the button. Button is hidden if this is null
     */
    fun initView(title: String, message: String, buttonLabel: String? = null, buttonAction: (() -> Unit)? = null) {
        alertAction_title.text = title
        alertAction_msg.text = message
        buttonLabel?.let { alertAction_action.text = buttonLabel }
        buttonAction?.let { callback ->
            alertAction_action.setOnClickListener { callback() }
        } ?: run { alertAction_action.visibility = View.GONE }
    }
}
