package com.woocommerce.android.notifications.local

enum class LocalNotificationType(val value: String) {
    BLAZE_NO_CAMPAIGN_REMINDER("blaze_no_campaign_reminder");
    override fun toString() = value

    companion object {
        fun fromString(source: String?): LocalNotificationType? =
            entries.firstOrNull { it.value.equals(source, ignoreCase = true) }
    }
}
