package com.woocommerce.android.notifications.local

enum class LocalNotificationType(val value: String) {
    STORE_CREATION_FINISHED("store_creation_complete"),
    STORE_CREATION_INCOMPLETE("one_day_after_store_creation_name_without_free_trial"),
    FREE_TRIAL_EXPIRING("one_day_before_free_trial_expires"),
    FREE_TRIAL_EXPIRED("one_day_after_free_trial_expires"),
    SIX_HOURS_AFTER_FREE_TRIAL_SUBSCRIBED("six_hours_after_free_trial_subscribed"),
    FREE_TRIAL_SURVEY_24H_AFTER_FREE_TRIAL_SUBSCRIBED("free_trial_survey_24h_after_free_trial_subscribed");

    override fun toString() = value

    companion object {
        fun fromString(source: String?): LocalNotificationType? =
            values().firstOrNull { it.value.equals(source, ignoreCase = true) }
    }
}
