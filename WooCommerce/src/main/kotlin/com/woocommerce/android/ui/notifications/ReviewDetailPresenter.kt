package com.woocommerce.android.ui.notifications

import com.woocommerce.android.extensions.buildComment
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.notifications.ReviewDetailContract.View
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.CommentAction.FETCH_COMMENT
import org.wordpress.android.fluxc.action.CommentAction.PUSH_COMMENT
import org.wordpress.android.fluxc.generated.CommentActionBuilder
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NoteIdSet
import org.wordpress.android.fluxc.store.CommentStore
import org.wordpress.android.fluxc.store.CommentStore.OnCommentChanged
import org.wordpress.android.fluxc.store.CommentStore.RemoteCommentPayload
import org.wordpress.android.fluxc.store.NotificationStore
import javax.inject.Inject

class ReviewDetailPresenter @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val commentStore: CommentStore,
    private val notificationStore: NotificationStore,
    private val networkStatus: NetworkStatus
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
                getOrBuildCommentForNotification(note).also {
                    view?.setNotification(note, it)
                    comment = it
                }
            } ?: fetchNotification(noteIdSet) // fetch from api
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
                view?.let { it.updateStatus(CommentStatus.fromString(comment.status)) }
            }
        }
    }

    override fun fetchNotification(idSet: NoteIdSet) {
        if (networkStatus.isConnected()) {
            // TODO fetch notifications from api or just single?
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

    override fun moderateComment(comment: CommentModel) {
        if (networkStatus.isConnected()) {
            val payload = RemoteCommentPayload(selectedSite.get(), comment)
            dispatcher.dispatch(CommentActionBuilder.newPushCommentAction(payload))
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onCommentChanged(event: OnCommentChanged) {
        view?.showSkeleton(false)

        when (event.causeOfChange) {
            FETCH_COMMENT -> onCommentFetched(event)
            PUSH_COMMENT -> onCommentModerated(event)
        }
    }

    private fun onCommentModerated(event: OnCommentChanged) {
        if (event.isError) {
            WooLog.e(NOTIFICATIONS, "$TAG - Error pushing comment changes to server! ${event.error.message}")

            // todo - set comment status back to previous status
            // todo - update the view labels
            view?.showModerateReviewError()
        } else {
            // Comment has been saved to the server.
            reloadComment()
        }
    }

    private fun onCommentFetched(event: OnCommentChanged) {
        if (event.isError) {
            WooLog.e(NOTIFICATIONS, "$TAG - Error fetching comment for note_id: " +
                    "${notification?.noteId} and comment_id: ${comment?.id}, ${event.error.message}")
            view?.showLoadReviewError()
        } else {
            // Comment has been fetched from the api successfully.
            // TODO add tracks for COMMENTS

            reloadComment()
        }
    }
}
