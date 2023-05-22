package com.woocommerce.android.notifications.local

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.concurrent.TimeUnit

sealed class LocalNotification(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val type: String,
    val delay: Long,
    val delayUnit: TimeUnit
) {
    companion object {
        const val STORE_CREATION_COMPLETE_NOTICE = "store_creation_complete"
        const val FREE_TRIAL_REMINDER = "one_day_after_store_creation_name_without_free_trial"
        const val FREE_TRIAL_EXPIRING_NOTICE = "one_day_before_free_trial_expires"
        const val FREE_TRIAL_EXPIRED_NOTICE = "one_day_after_free_trial_expires"
    }

    val id = type.hashCode()

    abstract fun getTitleString(resourceProvider: ResourceProvider): String
    abstract fun getDescriptionString(resourceProvider: ResourceProvider): String

    data class StoreCreationCompleteNotification(
        val name: String
    ) : LocalNotification(
        title = R.string.local_notification_store_creation_complete_title,
        description = R.string.local_notification_store_creation_complete_description,
        type = STORE_CREATION_COMPLETE_NOTICE,
        delay = 5,
        delayUnit = TimeUnit.MINUTES
    ) {
        override fun getTitleString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(title)
        }

        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, name)
        }
    }
}
