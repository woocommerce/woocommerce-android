package com.woocommerce.android.push

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
import org.wordpress.android.fluxc.store.NotificationStore
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class UnseenReviewsCountHandler @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val notificationStore: NotificationStore,
    private val selectedSite: SelectedSite
) {

    private val unseenReviewsCount: StateFlow<Int> =
        merge(unseenNotificationUpdates(), selectedSite.observe())
            .mapLatest { getUnseenReviewsNotificationCount() }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = appCoroutineScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = getUnseenReviewsNotificationCount()
            )

    fun observeUnseenCount(): Flow<Int> = unseenReviewsCount

    private fun unseenNotificationUpdates() = notificationStore
        .observeNotificationChanges()
        .filter { onlyNotificationUpdates(it) }

    private fun onlyNotificationUpdates(it: NotificationStore.OnNotificationChanged) =
        when (it.causeOfChange) {
            NotificationAction.MARK_NOTIFICATIONS_SEEN,
            NotificationAction.FETCH_NOTIFICATION -> true
            else -> false
        }

    private fun getUnseenReviewsNotificationCount() =
        notificationStore.getNotificationsForSite(
            site = selectedSite.get(),
            filterBySubtype = listOf(STORE_REVIEW.toString())
        ).filter { !it.read }.count()

}
