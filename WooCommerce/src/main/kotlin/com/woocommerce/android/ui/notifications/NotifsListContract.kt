package com.woocommerce.android.ui.notifications

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.notification.NotificationModel

interface NotifsListContract {
    interface Presenter : BasePresenter<View> {
        var isLoading: Boolean

        fun loadNotifs(forceRefresh: Boolean)
        fun fetchAndLoadNotifsFromDb(isForceRefresh: Boolean)
    }

    interface View : BaseView<Presenter> {
        var isActive: Boolean
        var isRefreshPending: Boolean

        fun showNotifications(notifsList: List<NotificationModel>, isFreshData: Boolean)
        fun showLoadNotificationsError()
        fun refreshFragmentState()
        fun showSkeleton(show: Boolean)
        fun openReviewDetail(notification: NotificationModel)
    }
}
