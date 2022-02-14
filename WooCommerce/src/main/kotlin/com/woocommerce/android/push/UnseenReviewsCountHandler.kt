package com.woocommerce.android.push

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.NotificationStore
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class UnseenReviewsCountHandler @Inject constructor(
    private val notificationStore: NotificationStore,
    private val selectedSite: SelectedSite
) {

    private val unseenReviewsCount: Flow<Int> =
        notificationStore.observeNotificationChanges()
            .filter { it.causeOfChange == NotificationAction.UPDATE_NOTIFICATION }
            .mapLatest { getUnreadReviewsNotificationCount() }
            .flowOn(Dispatchers.IO)

    fun observeUnseenCount(): Flow<Int> = unseenReviewsCount

    private fun getUnreadReviewsNotificationCount() =
        notificationStore.getNotificationsForSite(
            site = selectedSite.get(),
            filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())
        ).filter { !it.read }.count()
}
