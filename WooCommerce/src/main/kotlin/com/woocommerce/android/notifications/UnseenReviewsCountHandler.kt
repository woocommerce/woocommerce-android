package com.woocommerce.android.notifications

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.domain.SupportsReviewsReadStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind.STORE_REVIEW
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UnseenReviewsCountHandler @Inject constructor(
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
    private val wpComPushNotificationStore: WpComPushNotificationStore,
    private val supportsReviewsReadStatus: SupportsReviewsReadStatus,
    selectedSite: SelectedSite
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val unseenReviewsCount: SharedFlow<Int> =
        selectedSite.observe()
            .filterNotNull()
            .flatMapLatest { site ->
                if (supportsReviewsReadStatus()) {
                    wpComPushNotificationStore.observeNotificationsForSite(
                        site = site,
                        filterBySubtype = listOf(STORE_REVIEW.toString())
                    ).map { notifications -> notifications.count { !it.read } }
                } else {
                    flowOf(0)
                }
            }
            .distinctUntilChanged()
            .shareIn(
                scope = appCoroutineScope,
                started = SharingStarted.WhileSubscribed(),
                replay = 1
            )

    fun observeUnseenCount(): Flow<Int> = unseenReviewsCount
}
