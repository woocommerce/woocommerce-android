package com.woocommerce.android.util

enum class RemoteFeatureFlag(val remoteKey: String) {
    LOCAL_NOTIFICATION_STORE_CREATION_READY("woo_notification_store_creation_ready"),
    LOCAL_NOTIFICATION_NUDGE_FREE_TRIAL_AFTER_1D("woo_notification_nudge_free_trial_after_1d"),
    LOCAL_NOTIFICATION_1D_BEFORE_FREE_TRIAL_EXPIRES("woo_notification_1d_before_free_trial_expires"),
    LOCAL_NOTIFICATION_1D_AFTER_FREE_TRIAL_EXPIRES("woo_notification_1d_after_free_trial_expires")
}
