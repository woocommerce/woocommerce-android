package com.woocommerce.android.notifications.local

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.concurrent.TimeUnit

sealed class LocalNotification(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val type: LocalNotificationType,
    val delay: Long,
    val delayUnit: TimeUnit
) {
    open val data: String? = null
    val id = type.hashCode()

    abstract fun getDescriptionString(resourceProvider: ResourceProvider): String

    open fun getTitleString(resourceProvider: ResourceProvider): String {
        return resourceProvider.getString(title)
    }

    data class StoreCreationFinishedNotification(
        val name: String
    ) : LocalNotification(
        title = R.string.local_notification_store_creation_complete_title,
        description = R.string.local_notification_store_creation_complete_description,
        type = LocalNotificationType.STORE_CREATION_FINISHED,
        delay = 5,
        delayUnit = TimeUnit.MINUTES
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, name)
        }
    }

    data class StoreCreationIncompleteNotification(val name: String, val storeName: String) : LocalNotification(
        title = R.string.local_notification_without_free_trial_title,
        description = R.string.local_notification_without_free_trial_description,
        type = LocalNotificationType.STORE_CREATION_INCOMPLETE,
        delay = 24,
        delayUnit = TimeUnit.HOURS
    ) {
        override val data: String = storeName

        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, name, storeName)
        }
    }

    data class FreeTrialExpiringNotification(val expiryDate: String, val siteId: Long) : LocalNotification(
        title = R.string.local_notification_one_day_before_free_trial_expires_title,
        description = R.string.local_notification_one_day_before_free_trial_expires_description,
        type = LocalNotificationType.FREE_TRIAL_EXPIRING,
        delay = 13,
        delayUnit = TimeUnit.DAYS
    ) {
        override val data: String = siteId.toString()

        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, expiryDate)
        }
    }

    data class FreeTrialExpiredNotification(val name: String, val siteId: Long) : LocalNotification(
        title = R.string.local_notification_one_day_after_free_trial_expires_title,
        description = R.string.local_notification_one_day_after_free_trial_expires_description,
        type = LocalNotificationType.FREE_TRIAL_EXPIRED,
        delay = 15,
        delayUnit = TimeUnit.DAYS
    ) {
        override val data: String = siteId.toString()

        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, name)
        }
    }

    data class UpgradeToPaidPlanNotification(val siteId: Long) : LocalNotification(
        title = R.string.local_notification_upgrade_to_paid_plan_after_6_hours_title,
        description = R.string.local_notification_upgrade_to_paid_plan_after_6_hours_description,
        type = LocalNotificationType.UPGRADE_TO_PAID_PLAN,
        delay = 6,
        delayUnit = TimeUnit.HOURS
    ) {
        override val data: String = siteId.toString()

        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description)
        }
    }

    object FreeTrialSurveyNotification : LocalNotification(
        title = R.string.local_notification_survey_after_24_hours_title,
        description = R.string.local_notification_survey_after_24_hours_description,
        type = LocalNotificationType.UPGRADE_TO_PAID_PLAN,
        delay = 24,
        delayUnit = TimeUnit.HOURS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description)
        }
    }
}
