package com.woocommerce.android.ui.notifications

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.canMarkAsSpam
import com.woocommerce.android.extensions.canModerate
import com.woocommerce.android.extensions.canTrash
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.extensions.getConvertedTimestamp
import com.woocommerce.android.extensions.getProductInfo
import com.woocommerce.android.extensions.getRating
import com.woocommerce.android.extensions.getReviewDetail
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_review_detail.*
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.DELETED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject

class ReviewDetailFragment : Fragment(), ReviewDetailContract.View {
    companion object {
        const val TAG = "ReviewDetailFragment"
        const val FIELD_REMOTE_NOTIF_ID = "notif-remote-id"
        const val FIELD_REMOTE_COMMENT_ID = "remote-comment-id"

        // TODO remove review detail
        const val FIELD_REVIEW_DETAIL = "notif-review-detail"

        fun newInstance(notification: NotificationModel): ReviewDetailFragment {
            val args = Bundle()
            args.putLong(FIELD_REMOTE_NOTIF_ID, notification.remoteNoteId)
            args.putLong(FIELD_REMOTE_COMMENT_ID, notification.getCommentId() ?: 0L)
            args.putParcelable(FIELD_REVIEW_DETAIL, notification.getReviewDetail())

            val fragment = ReviewDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: ReviewDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val skeletonView = SkeletonView()
    private var remoteNoteId: Long = 0L
    private var remoteCommentId: Long = 0L
    private var productUrl: String? = null

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        presenter.takeView(this)

        arguments?.let {
            remoteNoteId = it.getLong(FIELD_REMOTE_NOTIF_ID)
            remoteCommentId = it.getLong(FIELD_REMOTE_COMMENT_ID)
        }

        presenter.loadNotificationDetail(remoteNoteId, remoteCommentId)
    }

    override fun onDestroyView() {
        presenter.dropView()
        super.onDestroyView()
    }

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(container, R.layout.skeleton_notif_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun setNotification(note: NotificationModel, comment: CommentModel) {
        // Populate reviewer section
        GlideApp.with(review_gravatar.context)
                .load(comment.authorProfileImageUrl)
                .placeholder(R.drawable.ic_user_circle_grey_24dp)
                .circleCrop()
                .into(review_gravatar)
        review_user_name.text = comment.authorName
        review_time.text = DateTimeUtils.timeSpanFromTimestamp(note.getConvertedTimestamp(), activity as Context)

        // Populate reviewed product info
        review_product_name.text = comment.postTitle
        note.getProductInfo()?.url?.let { url ->
            review_open_product.setOnClickListener { ActivityUtils.openUrlExternal(activity as Context, url) }
        }
        productUrl = note.getProductInfo()?.url

        // Set the rating if available, or hide
        note.getRating()?.let { rating ->
            review_rating_bar.rating = rating
            review_rating_bar.visibility = View.VISIBLE
        }

        // Set the review text
        review_description.text = comment.content

        // Initialize moderation buttons and set comment status
        configureModerationButtons(note)
        updateStatus(CommentStatus.fromString(comment.status))
    }

    private fun configureModerationButtons(note: NotificationModel) {
        if (note.canModerate()) {
            review_approve.visibility = View.VISIBLE
            review_approve.setOnCheckedChangeListener { _, isChecked ->
                when (isChecked) {
                    true -> disapproveReview()
                    false -> approveReview()
                }
            }
        } else {
            review_approve.visibility = View.GONE
        }

        if (note.canMarkAsSpam()) {
            review_spam.visibility = View.VISIBLE
            review_spam.setOnCheckedChangeListener { _, isChecked ->
                when (isChecked) {
                    // User has marked this review as spam
                    true -> spamReview()
                    // User has marked this review as not spam, set as approved automatically
                    false -> approveReview()
                }
            }
        } else {
            review_spam.visibility = View.GONE
        }

        if (note.canTrash()) {
            review_trash.visibility = View.VISIBLE
            review_trash.setOnCheckedChangeListener { _, isChecked ->
                when (isChecked) {
                    true -> trashReview()
                    // If un-trashed, mark it as approved automatically
                    false -> approveReview()
                }
            }
        } else {
            review_trash.visibility = View.GONE
        }
    }

    override fun updateStatus(status: CommentStatus) {
        when (status) {
            APPROVED -> review_approve.isChecked = true
            UNAPPROVED -> review_approve.isChecked = false
            SPAM -> {}
            TRASH -> {}
            DELETED -> {}
            else -> {}
        }
    }

    override fun showLoadReviewError() {
        // todo
    }

    override fun showModerateReviewError() {
        // todo
    }

    private fun trashReview() {
        uiMessageResolver.showSnack("Trash logic not implemented")
    }

    private fun spamReview() {
        uiMessageResolver.showSnack("Spam logic not implemented")
    }

    private fun approveReview() {
        uiMessageResolver.showSnack("Approve logic not implemented")
    }

    private fun disapproveReview() {
        uiMessageResolver.showSnack("Disapprove logic not implemented")
    }
}
