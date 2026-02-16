package com.woocommerce.android.util

/**
 * Feature flags control feature availability.
 *
 * - If overridden (debug builds) → use override value
 * - If remote has value for [remoteFlagKey] → use remote value
 * - Otherwise → use [default] value
 *
 * Access via [FeatureFlagRepository.isEnabled].
 */
enum class FeatureFlag(
    val remoteFlagKey: String,
    val default: Boolean = PackageUtils.isDebugBuild()
) {
    WC_SHIPPING_BANNER("wc_shipping_banner"),
    BETTER_CUSTOMER_SEARCH_M2("better_customer_search_m2"),
    ORDER_CREATION_AUTO_TAX_RATE("order_creation_auto_tax_rate"),
    BOOKINGS_MVP("bookings_mvp"),
    POS_REFUNDS("pos_refunds"),
    POS_BOOKINGS("pos_bookings"),
    POS_PRODUCTS_FTS("pos_products_fts"),
    WOO_POS_LOCAL_CATALOG_FILE_APPROACH("woo_pos_local_catalog_file_approach", default = false),
    WOO_PUSH_NOTIFICATIONS_SYSTEM("woo_push_notifications_system", default = false),
    WOO_PUSH_NOTIFICATIONS_SYSTEM_M2("woo_push_notifications_system_m2", default = false),
    WOO_POS_CLIENT_SIDE_BANNER("woo_pos_client_side_banner"),
    AGE_ELIGIBILITY_CHECKS("age_eligibility_checks"),
    LOCAL_NOTIFICATION_STORE_CREATION_READY("woo_notification_store_creation_ready"),
    LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES("woo_notification_1d_before_free_trial_expires"),
    LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES("woo_notification_1d_after_free_trial_expires"),
    WOO_POS("woo_pos"),
    APP_PASSWORDS_FOR_JETPACK_SITES("woo_app_passwords_for_jetpack_sites"),
    WOO_POS_LOCAL_CATALOG_M1("woo_pos_local_catalog_m1"),
    WOO_POS_TABLET_PROMO_BANNER("woo_pos_tablet_promo_banner"),
}
