package com.woocommerce.android.notifications.local

enum class LocalNotificationType(val value: String) {
    // A sample notification type to keep detekt happy, this should be removed when adding real notifications
    UNUSED("unused");
    override fun toString() = value

    companion object {
        fun fromString(source: String?): LocalNotificationType? =
            entries.firstOrNull { it.value.equals(source, ignoreCase = true) }
    }
}
