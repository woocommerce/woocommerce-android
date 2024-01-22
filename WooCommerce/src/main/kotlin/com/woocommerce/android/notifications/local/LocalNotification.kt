package com.woocommerce.android.notifications.local

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.ResourceProvider
import java.util.concurrent.TimeUnit

sealed class LocalNotification(
    open val siteId: Long,
    @StringRes val title: Int,
    @StringRes val description: Int,
    val type: LocalNotificationType,
    val delay: Long,
    val delayUnit: TimeUnit
) {
    open val data: String? = null
    val id: Int by lazy {
        // Combine current time with hash codes of properties for uniqueness
        val timeComponent = (System.currentTimeMillis() / 1000L % Integer.MAX_VALUE).toInt()
        val hashComponent = (siteId.hashCode() xor title.hashCode() xor description.hashCode() xor type.hashCode() xor delay.hashCode() xor delayUnit.hashCode())

        // Combine components to form a unique ID
        timeComponent xor hashComponent
    }

    abstract fun getDescriptionString(resourceProvider: ResourceProvider): String

    open fun getTitleString(resourceProvider: ResourceProvider): String {
        return resourceProvider.getString(title)
    }

    data class StoreCreationCompletedNotification(
        override val siteId: Long,
        val name: String
    ) : LocalNotification(
        siteId = siteId,
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



    data class FreeTrialExpiringNotification(
        val expiryDate: String,
        override val siteId: Long
    ) : LocalNotification(
        siteId = siteId,
        title = R.string.local_notification_one_day_before_free_trial_expires_title,
        description = R.string.local_notification_one_day_before_free_trial_expires_description,
        type = LocalNotificationType.FREE_TRIAL_EXPIRING,
        delay = 13,
        delayUnit = TimeUnit.DAYS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, expiryDate)
        }
    }

    data class FreeTrialExpiredNotification(
        val name: String,
        override val siteId: Long
    ) : LocalNotification(
        siteId = siteId,
        title = R.string.local_notification_one_day_after_free_trial_expires_title,
        description = R.string.local_notification_one_day_after_free_trial_expires_description,
        type = LocalNotificationType.FREE_TRIAL_EXPIRED,
        delay = 15,
        delayUnit = TimeUnit.DAYS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, name)
        }
    }

    data class UpgradeToPaidPlanNotification(override val siteId: Long) : LocalNotification(
        siteId = siteId,
        title = R.string.local_notification_upgrade_to_paid_plan_after_6_hours_title,
        description = R.string.local_notification_upgrade_to_paid_plan_after_6_hours_description,
        type = LocalNotificationType.SIX_HOURS_AFTER_FREE_TRIAL_SUBSCRIBED,
        delay = 6,
        delayUnit = TimeUnit.HOURS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description)
        }
    }

    data class FreeTrialSurveyNotification(override val siteId: Long) : LocalNotification(
        siteId = siteId,
        title = R.string.local_notification_survey_after_24_hours_title,
        description = R.string.local_notification_survey_after_24_hours_description,
        type = LocalNotificationType.FREE_TRIAL_SURVEY_24H_AFTER_FREE_TRIAL_SUBSCRIBED,
        delay = 24,
        delayUnit = TimeUnit.HOURS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description)
        }
    }

    data class StillExploringNotification(override val siteId: Long) : LocalNotification(
        siteId = siteId,
        title = R.string.local_notification_still_exploring_title,
        description = R.string.local_notification_still_exploring_description,
        type = LocalNotificationType.THREE_DAYS_AFTER_STILL_EXPLORING,
        delay = 3,
        delayUnit = TimeUnit.DAYS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description)
        }
    }

    data class TestNotification(
        override val siteId: Long,
        @StringRes val testTitle: Int,
        @StringRes val testDescription: Int
    ) : LocalNotification(
        siteId = siteId,
        title = testTitle,
        description = testDescription,
        type = LocalNotificationType.TEST, // Assuming you have a TEST type defined
        delay = 10,
        delayUnit = TimeUnit.SECONDS
    ) {
        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description)
        }
    }
}
