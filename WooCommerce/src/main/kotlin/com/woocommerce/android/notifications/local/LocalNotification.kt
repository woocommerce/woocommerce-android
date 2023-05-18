package com.woocommerce.android.notifications.local

import java.util.concurrent.TimeUnit

@Suppress("ForbiddenComment")
sealed class LocalNotification(
    open val title: String,
    open val description: String,
    val type: String,
    val delay: Long,
    val delayUnit: TimeUnit
) {
    companion object {
        const val NOTIFICATION_TYPE_STORE_CREATION_COMPLETE = "store_creation_complete"
        const val NOTIFICATION_TYPE_WITHOUT_FREE_TRIAL = "one_day_after_store_creation_name_without_free_trial"
        const val NOTIFICATION_TYPE_BEFORE_FREE_TRIAL = "one_day_before_free_trial_expires"
        const val NOTIFICATION_TYPE_AFTER_FREE_TRIAL = "one_day_after_free_trial_expires"
    }
    val id = type.hashCode()

    // TODO: Remove this test notification
    data class Test(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        type = "test",
        delay = 10,
        delayUnit = TimeUnit.SECONDS
    )

    data class NewStoreReadyNotice(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        type = NOTIFICATION_TYPE_STORE_CREATION_COMPLETE,
        delay = 5,
        delayUnit = TimeUnit.MINUTES
    )

    data class FreeTrialReminder(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        type = NOTIFICATION_TYPE_WITHOUT_FREE_TRIAL,
        delay = 24,
        delayUnit = TimeUnit.HOURS
    )

    data class FreeTrialExpiringNotice(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        type = NOTIFICATION_TYPE_BEFORE_FREE_TRIAL,
        delay = 13,
        delayUnit = TimeUnit.DAYS
    )

    data class FreeTrialExpiredNotice(
        override val title: String,
        override val description: String
    ) : LocalNotification(
        title = title,
        description = description,
        type = NOTIFICATION_TYPE_AFTER_FREE_TRIAL,
        delay = 15,
        delayUnit = TimeUnit.DAYS
    )
}
