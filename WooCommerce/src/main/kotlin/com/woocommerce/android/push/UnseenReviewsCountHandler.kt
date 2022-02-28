package com.woocommerce.android.push

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
import org.wordpress.android.fluxc.store.NotificationStore
import javax.inject.Inject
import javax.inject.Singleton

@ExperimentalCoroutinesApi
@Singleton
class UnseenReviewsCountHandler @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val notificationStore: NotificationStore,
    selectedSite: SelectedSite
) {
    private val unseenReviewsCount: SharedFlow<Int> =
        selectedSite.observe()
            .flatMapLatest { site ->
                if (site != null) {
                    notificationStore.observeNotificationsForSite(
                        site = site,
                        filterBySubtype = listOf(STORE_REVIEW.toString())
                    )
                } else {
                    flowOf(emptyList())
                }
            }
            .map { it.count { notification -> !notification.read } }
            .distinctUntilChanged()
            .shareIn(
                scope = appCoroutineScope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )

    fun observeUnseenCount(): Flow<Int> = unseenReviewsCount
}
