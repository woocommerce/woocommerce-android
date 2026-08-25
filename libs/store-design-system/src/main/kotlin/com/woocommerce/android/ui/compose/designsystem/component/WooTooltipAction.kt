package com.woocommerce.android.ui.compose.designsystem.component

/** A single action rendered inside [WooTooltipBox]. */
data class WooTooltipAction(
    val label: String,
    val onClick: () -> Unit,
) {
    init {
        require(label.isNotBlank()) {
            "WooTooltipAction label must not be blank"
        }
    }
}
