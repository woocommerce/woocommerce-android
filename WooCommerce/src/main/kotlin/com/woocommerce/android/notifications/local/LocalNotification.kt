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
    val id = type.hashCode()

    abstract fun getTitleString(resourceProvider: ResourceProvider): String
    abstract fun getDescriptionString(resourceProvider: ResourceProvider): String

    open val data: String? = null

    data class StoreCreationFinishedNotification(
        val name: String
    ) : LocalNotification(
        title = R.string.local_notification_store_creation_complete_title,
        description = R.string.local_notification_store_creation_complete_description,
        type = LocalNotificationType.STORE_CREATION_FINISHED,
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

    data class StoreCreationIncompleteNotification(val name: String, val storeName: String) : LocalNotification(
        title = R.string.local_notification_without_free_trial_title,
        description = R.string.local_notification_without_free_trial_description,
        type = LocalNotificationType.STORE_CREATION_INCOMPLETE,
        delay = 24,
        delayUnit = TimeUnit.HOURS
    ) {
        override fun getTitleString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(title)
        }

        override fun getDescriptionString(resourceProvider: ResourceProvider): String {
            return resourceProvider.getString(description, name, storeName)
        }

        override val data: String = storeName
    }
}
