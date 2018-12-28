package com.woocommerce.android.ui.notifications

import android.content.Context
import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.notification.NotificationModel

interface ReviewDetailContract {
    interface Presenter : BasePresenter<View> {
        var notification: NotificationModel?
        var comment: CommentModel?

        fun loadNotificationDetail(noteId: Long, commentId: Long)
        fun getOrBuildCommentForNotification(notif: NotificationModel): CommentModel
        fun reloadComment()
        fun fetchComment()
        fun markNotificationRead(context: Context, notification: NotificationModel)
    }

    interface View : BaseView<Presenter> {
        fun showSkeleton(show: Boolean)
        fun setNotification(note: NotificationModel, comment: CommentModel)
        fun updateStatus(status: CommentStatus)
    }
}
