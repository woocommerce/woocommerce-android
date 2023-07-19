package com.woocommerce.android.notifications.local

enum class LocalNotificationType(val value: String) {
    STORE_CREATION_FINISHED("store_creation_complete"),
    STORE_CREATION_INCOMPLETE("one_day_after_store_creation_name_without_free_trial"),
    FREE_TRIAL_EXPIRING("one_day_before_free_trial_expires"),
    FREE_TRIAL_EXPIRED("one_day_after_free_trial_expires"),
    UPGRADE_TO_PAID_PLAN("upgrade_to_paid_plan"),
}
