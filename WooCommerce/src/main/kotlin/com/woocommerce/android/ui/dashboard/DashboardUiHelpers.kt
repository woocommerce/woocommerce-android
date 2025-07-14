package com.woocommerce.android.ui.dashboard

import com.woocommerce.android.R
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetAction

fun DashboardWidget.Type.defaultHideMenuEntry(onHideClicked: () -> Unit): DashboardWidgetAction {
    return DashboardWidgetAction(
        title = UiStringRes(
            R.string.dynamic_dashboard_widget_menu_item_hide,
            params = listOf(UiStringRes(titleResource))
        ),
        action = onHideClicked
    )
}
