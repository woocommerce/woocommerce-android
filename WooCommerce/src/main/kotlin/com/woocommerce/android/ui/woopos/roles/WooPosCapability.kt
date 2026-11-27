package com.woocommerce.android.ui.woopos.roles

enum class WooPosCapability(val identifier: String) {
    PROCESS_SALES("woocommerce_pos_process_sales"),
    VIEW_ORDERS("woocommerce_pos_view_orders"),
    APPLY_COUPONS("woocommerce_pos_apply_coupons"),
    CREATE_COUPONS("woocommerce_pos_create_coupons"),
    ISSUE_REFUNDS("woocommerce_pos_issue_refunds"),
    EXIT_POS("woocommerce_pos_exit"),
    MANAGE_STORE_SETTINGS("woocommerce_pos_manage_store_settings"),
}
