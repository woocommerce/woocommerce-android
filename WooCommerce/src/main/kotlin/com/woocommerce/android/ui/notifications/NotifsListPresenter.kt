package com.woocommerce.android.ui.notifications

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.di.ActivityScope
import com.woocommerce.android.network.ConnectionChangeReceiver
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.notifications.NotifsListContract.View
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.CommentAction.DELETE_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.PUSH_COMMENT
import org.wordpress.android.fluxc.action.NotificationAction
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import javax.inject.Inject

@OpenClassOnDebug
@ActivityScope
class NotifsListPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val notificationStore: NotificationStore,
    private val networkStatus: NetworkStatus
) : NotifsListContract.Presenter {
    companion object {
        private val TAG: String = NotifsListPresenter::class.java.simpleName
    }

    private var view: NotifsListContract.View? = null
    private var isRefreshing = false

    override fun takeView(view: View) {
        this.view = view
        ConnectionChangeReceiver.getEventBus().register(this)
        dispatcher.register(this)
    }

    override fun dropView() {
        view = null
        ConnectionChangeReceiver.getEventBus().unregister(this)
        dispatcher.unregister(this)
    }

    override fun loadNotifs(forceRefresh: Boolean) {
        view?.let {
            if (networkStatus.isConnected() && forceRefresh) {
                it.showSkeleton(true)
                isRefreshing = forceRefresh

                val payload = FetchNotificationsPayload()
                dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(payload))
            } else {
                // Load cached notifications from the db
                fetchAndLoadNotifsFromDb(forceRefresh)
            }
        }
    }

    override fun reloadNotifs() {
        if (networkStatus.isConnected()) {
            val payload = FetchNotificationsPayload()
            isRefreshing = true
            dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(payload))
        }
    }

    override fun fetchAndLoadNotifsFromDb(isForceRefresh: Boolean) {
        val notifs = notificationStore.getNotificationsForSite(
                site = selectedSite.get(),
                filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
                filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString()))
        if (notifs.size > 0) {
            view?.showEmptyView(false)
            view?.showNotifications(notifs, isFreshData = isForceRefresh)
        } else {
            view?.showEmptyView(true)
        }
    }

    override fun pushUpdatedComment(comment: CommentModel) {
        if (networkStatus.isConnected()) {
            val payload = RemoteCommentPayload(selectedSite.get(), comment)
            dispatcher.dispatch(CommentActionBuilder.newPushCommentAction(payload))
        }
    }

    override fun markAllNotifsRead() {
        if (networkStatus.isConnected()) {
            val unreadNotifs = notificationStore.getNotificationsForSite(
                    site = selectedSite.get(),
                    filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
                    filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())).filter { !it.read }
            if (unreadNotifs.isNotEmpty()) {
                val payload = MarkNotificationsReadPayload(unreadNotifs)
                dispatcher.dispatch(NotificationActionBuilder.newMarkNotificationsReadAction(payload))

                // Optimistic design - update the UI immediately to show all notifs are now
                // marked as read. If this fails, the list will be reloaded by the database.
                view?.visuallyMarkNotificationsAsRead()

                // we only show this snackbar twice to avoid annoying the user
                val numTimesShown = AppPrefs.getNumTimesMarkAllReadSnackShown()
                if (numTimesShown < 2) {
                    AppPrefs.incNumTimesMarkAllReadSnackShown()
                    view?.showMarkAllNotificationsReadSuccess()
                }
            } else {
                WooLog.d(NOTIFICATIONS, "Mark all as read: No unread notifications found. Exiting.")
            }

            view?.updateMarkAllReadMenuItem()
        }
    }

    override fun hasUnreadNotifs(): Boolean {
        return notificationStore.hasUnreadNotificationsForSite(
                site = selectedSite.get(),
                filterByType = listOf(NotificationModel.Kind.STORE_ORDER.toString()),
                filterBySubtype = listOf(NotificationModel.Subkind.STORE_REVIEW.toString())
        )
    }

    private fun onCommentModerated(event: OnCommentChanged) {
        if (event.isError) {
            AnalyticsTracker.track(Stat.REVIEW_ACTION_FAILED, mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to event.error.message))

            WooLog.e(NOTIFICATIONS, "${NotifsListFragment.TAG} - Error pushing comment changes to server! " +
                    "${event.error.message}")

            view?.notificationModerationError()
            fetchAndLoadNotifsFromDb(false)
        } else {
            AnalyticsTracker.track(Stat.REVIEW_ACTION_SUCCESS)
            view?.notificationModerationSuccess()

            // Request fresh notification data be fetched from the api to reflect deleted
            // notifications.
            reloadNotifs()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ConnectionChangeEvent) {
        if (event.isConnected) {
            // Refresh data now that a connection is active if needed
            view?.let { notifView ->
                if (notifView.isRefreshPending) {
                    notifView.refreshFragmentState()
                }
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        view?.showSkeleton(false)
        when (event.causeOfChange) {
            NotificationAction.FETCH_NOTIFICATIONS -> {
                if (event.isError) {
                    WooLog.e(NOTIFICATIONS, "$TAG - Error fetching notifications: ${event.error.message}")
                    view?.showLoadNotificationsError()
                    fetchAndLoadNotifsFromDb(false)
                } else {
                    AnalyticsTracker.track(Stat.NOTIFICATIONS_LOADED)

                    val forceRefresh = isRefreshing
                    fetchAndLoadNotifsFromDb(forceRefresh)
                }
                isRefreshing = false
            }
            NotificationAction.UPDATE_NOTIFICATION, NotificationAction.FETCH_NOTIFICATION -> {
                // TODO eventually we'll want to implement inserting or updating notifications
                // in the notifications list individually.

                // Refresh the list of notifications from the database
                fetchAndLoadNotifsFromDb(false)
            }
            NotificationAction.MARK_NOTIFICATIONS_READ -> {
                if (event.isError) {
                    WooLog.e(NOTIFICATIONS, "$TAG - Error marking all notifications as read: ${event.error.message}")
                    view?.showMarkAllNotificationsReadError()
                }
                // TODO eventually we'll want to implement updating individual notifications in
                // the notifications list.

                // Refresh the list of notifications from the database
                fetchAndLoadNotifsFromDb(false)
            }
            else -> {}
        }

        view?.updateMarkAllReadMenuItem()
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onCommentChanged(event: OnCommentChanged) {
        if (event.causeOfChange == DELETE_COMMENT || event.causeOfChange == PUSH_COMMENT) onCommentModerated(event)
    }
}
