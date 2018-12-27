package com.woocommerce.android.ui.notifications

import android.content.Context
import com.woocommerce.android.R
import com.woocommerce.android.extensions.buildComment
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.notifications.ReviewDetailContract.View
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.CommentAction.FETCH_COMMENT
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_READ
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NoteIdSet
import org.wordpress.android.fluxc.store.CommentStore
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import javax.inject.Inject

class ReviewDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val commentStore: CommentStore,
    private val notificationStore: NotificationStore,
    private val networkStatus: NetworkStatus,
    private val uiMessageResolver: UIMessageResolver
) : ReviewDetailContract.Presenter {
    companion object {
        private val TAG: String = ReviewDetailPresenter::class.java.simpleName
    }

    private var view: ReviewDetailContract.View? = null
    override var notification: NotificationModel? = null
    override var comment: CommentModel? = null

    override fun takeView(view: View) {
        this.view = view
        dispatcher.register(this)
    }

    override fun dropView() {
        view = null
        dispatcher.unregister(this)
    }

    override fun loadNotificationDetail(noteId: Long, commentId: Long) {
        view?.let {
            // Attempt to load notification from the database
            val noteIdSet = NoteIdSet(-1, noteId, selectedSite.get().id)

            // Verify there is a valid comment ID for this notification. If not,
            // show an error.
            notificationStore.getNotificationByIdSet(noteIdSet)?.let { note ->
                notification = note

                comment = getOrBuildCommentForNotification(note).also { comment ->
                    view?.setNotification(note, comment)
                }
            }
        }
    }

    override fun getOrBuildCommentForNotification(notif: NotificationModel): CommentModel {
        // Pull comment from db or create a temporary one if none exists.
        return commentStore.getCommentBySiteAndRemoteId(selectedSite.get(), notif.getCommentId())
                ?: notif.buildComment().also { fetchComment() }
    }

    override fun reloadComment() {
        notification?.getCommentId()?.let {
            commentStore.getCommentBySiteAndRemoteId(selectedSite.get(), it)?.let { comment ->
                view?.updateStatus(CommentStatus.fromString(comment.status))
            }
        }
    }

    override fun fetchComment() {
        if (networkStatus.isConnected()) {
            // Request comment from the api
            notification?.getCommentId()?.let {
                val payload = RemoteCommentPayload(selectedSite.get(), it)
                dispatcher.dispatch(CommentActionBuilder.newFetchCommentAction(payload))
            }
        }
    }

    /**
     * Fires the event to mark a notification as read and removes it from the notification bar if needed.
     */
    override fun markNotificationRead(context: Context, notification: NotificationModel) {
        NotificationHandler.removeNotificationWithNoteIdFromSystemBar(context, notification.remoteNoteId.toString())
        if (!notification.read) {
            notification.read = true
            val payload = MarkNotificationsReadPayload(listOf(notification))
            dispatcher.dispatch(NotificationActionBuilder.newMarkNotificationsReadAction(payload))
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onCommentChanged(event: OnCommentChanged) {
        view?.showSkeleton(false)

        when (event.causeOfChange) {
            FETCH_COMMENT -> onCommentFetched(event)
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        when (event.causeOfChange) {
            MARK_NOTIFICATIONS_READ -> onNotificationMarkedRead(event)
        }
    }

    private fun onNotificationMarkedRead(event: OnNotificationChanged) {
        notification?.let {
            // We only care about logging an error
            if (event.changedNotificationLocalIds.contains(it.noteId)) {
                if (event.isError) {
                    WooLog.e(NOTIFICATIONS, "$TAG - Error marking new order notification as read!")
                }
            }
        }
    }

    private fun onCommentFetched(event: OnCommentChanged) {
        if (event.isError) {
            WooLog.e(NOTIFICATIONS, "$TAG - Error fetching comment for note_id: " +
                    "${notification?.noteId} and comment_id: ${comment?.id}, ${event.error.message}")

            // TODO add tracks for fetching comment error
            uiMessageResolver.showSnack(R.string.wc_load_review_error)
        } else {
            // Comment has been fetched from the api successfully.
            // TODO add tracks for COMMENTS

            reloadComment()
        }
    }
}
