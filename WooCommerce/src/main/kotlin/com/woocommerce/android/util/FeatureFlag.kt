package com.woocommerce.android.util

/**
 * Feature flags control feature availability.
 *
 * - If overridden (debug builds) → use override value
 * - If [localValue] is false → the feature stays disabled
 * - If [localValue] is true and remote has a value for [remoteFlagKey] → use remote value
 * - Otherwise → use [localValue]
 *
 * Access via [FeatureFlagRepository.isEnabled].
 */
enum class FeatureFlag(
    val remoteFlagKey: String,
    val localValue: Boolean = true
) {
    BOOKINGS_MVP("bookings_mvp"),
    LOCAL_NOTIFICATION_STORE_CREATION_READY("woo_notification_store_creation_ready"),
    LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES("woo_notification_1d_before_free_trial_expires"),
    LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES("woo_notification_1d_after_free_trial_expires"),
    WOO_POS("woo_pos"),
    WOO_POS_PHONE("woo_pos_phone", localValue = PackageUtils.isDebugBuild()),
    WOO_POS_TAP_TO_PAY("woo_pos_tap_to_pay", localValue = PackageUtils.isDebugBuild()),
    WOO_POS_SCAN_TO_PAY("woo_pos_scan_to_pay", localValue = PackageUtils.isDebugBuild()),
    WOO_POS_MARK_ORDER_AS_PAID("woo_pos_mark_order_as_complete", localValue = PackageUtils.isDebugBuild()),
    APP_PASSWORDS_FOR_JETPACK_SITES("woo_app_passwords_for_jetpack_sites"),
    WOO_POS_LOCAL_CATALOG_M1("woo_pos_local_catalog_m1"),
    WOO_POS_TABLET_PROMO_BANNER("woo_pos_tablet_promo_banner"),
    WC_SHIPPING_BANNER("wc_shipping_banner", localValue = PackageUtils.isDebugBuild()),
    BETTER_CUSTOMER_SEARCH_M2("better_customer_search_m2", localValue = PackageUtils.isDebugBuild()),
    ORDER_CREATION_AUTO_TAX_RATE("order_creation_auto_tax_rate", localValue = PackageUtils.isDebugBuild()),
    POS_PRODUCTS_FTS("pos_products_fts", localValue = PackageUtils.isDebugBuild()),
    POS_BOOKINGS("pos_bookings", localValue = PackageUtils.isDebugBuild()),
    AGE_ELIGIBILITY_CHECKS("age_eligibility_checks", localValue = PackageUtils.isDebugBuild()),
    BOOKINGS_RESCHEDULE("bookings_reschedule", localValue = PackageUtils.isDebugBuild()),
    WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1("woo_self_driven_push_notifications_m1", localValue = false),
    LOGGED_OUT_FF_PANEL("logged_out_ff_panel", localValue = PackageUtils.isDebugBuild()),
    AI_ASSISTANT("ai_assistant", localValue = PackageUtils.isDebugBuild()),
    AI_SUPPORT_CHAT("ai_support_chat", localValue = PackageUtils.isDebugBuild()),
    SMARTER_NOTIFICATIONS("smarter_notifications", localValue = PackageUtils.isDebugBuild()),
    IPP_COUNTRY_EXPANSION("woo_ipp_country_expansion"),
    IPP_COUNTRY_EXPANSION_EU_EXTENDED("woo_ipp_country_expansion_eu_extended"),
    IPP_AUSTRALIA_WOOPAYMENTS("woo_ipp_australia_woopayments"),
}
