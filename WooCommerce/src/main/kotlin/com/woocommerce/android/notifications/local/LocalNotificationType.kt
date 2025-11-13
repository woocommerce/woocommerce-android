package com.woocommerce.android.notifications.local

enum class LocalNotificationType(val value: String) {
    BLAZE_NO_CAMPAIGN_REMINDER("blaze_no_campaign_reminder"),
    BLAZE_ABANDONED_CAMPAIGN_REMINDER("blaze_abandoned_campaign_reminder"),
    WOO_POS_SURVEY_POTENTIAL_USER_REMINDER("woo_pos_survey_potential_user_survey"),
    WOO_POS_SURVEY_CURRENT_USER_REMINDER("woo_pos_survey_current_user_survey");
    override fun toString() = value

    companion object {
        fun fromString(source: String?): LocalNotificationType? =
            entries.firstOrNull { it.value.equals(source, ignoreCase = true) }
    }
}
