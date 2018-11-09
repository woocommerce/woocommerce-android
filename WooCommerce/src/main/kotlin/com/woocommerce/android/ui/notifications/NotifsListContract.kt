package com.woocommerce.android.ui.notifications

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView

interface NotifsListContract {
    interface Presenter : BasePresenter<View> {
        fun loadNotifs(forceRefresh: Boolean)
        fun loadMoreNotifs()
        fun canLoadMore(): Boolean
        fun isLoading(): Boolean
        fun fetchAndLoadNotifsFromDb(isForceRefresh: Boolean)
        fun setNotifsSeen()
        fun setAllNotifsRead()
        fun setNotificationRead()
    }

    interface View : BaseView<Presenter> {
        var isActive: Boolean
        var isRefreshPending: Boolean

        fun setLoadingMoreIndicator(active: Boolean)
        fun showNotifications(notifs: List<WCNotificationModel>, isFreshData: Boolean)
        fun refreshFragmentState()
        fun showSkeleton(show: Boolean)
    }
}
