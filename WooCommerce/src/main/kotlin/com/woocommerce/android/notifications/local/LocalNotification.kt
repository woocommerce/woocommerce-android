package com.woocommerce.android.notifications.local

import java.util.concurrent.TimeUnit

@Suppress("ForbiddenComment")
sealed class LocalNotification(
    open val title: String,
    open val description: String,
    val tag: String,
    val delay: Long,
    val delayUnit: TimeUnit
) {
    val id = tag.hashCode()

    // TODO: Remove this test notification
    data class Test(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        tag = "test",
        delay = 10,
        delayUnit = TimeUnit.SECONDS
    )

    data class NewStoreReadyNotice(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        tag = "new_store_ready",
        delay = 5,
        delayUnit = TimeUnit.MINUTES
    )

    data class FreeTrialReminder(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        tag = "free_trial_reminder",
        delay = 24,
        delayUnit = TimeUnit.HOURS
    )

    data class FreeTrialExpiringNotice(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        tag = "free_trial_expiring",
        delay = 13,
        delayUnit = TimeUnit.DAYS
    )

    data class FreeTrialExpiredNotice(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        tag = "free_trial_expired",
        delay = 14,
        delayUnit = TimeUnit.DAYS
    )
}
