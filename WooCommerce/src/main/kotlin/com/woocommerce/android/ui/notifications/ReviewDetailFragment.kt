package com.woocommerce.android.ui.notifications

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.extensions.getReviewDetail
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_review_detail.*
import org.wordpress.android.fluxc.model.CommentModel
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

        review_approve.setOnCheckedChangeListener { _, isChecked ->
            when (isChecked) {
                true -> disapproveReview()
                false -> approveReview()
            }
        }

        review_spam.setOnClickListener { spamReview() }
        review_trash.setOnClickListener { trashReview() }
        review_open_product.setOnClickListener { openProduct() }

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
        note.getReviewDetail()?.let {
            it.productInfo?.let { product ->
                review_product_name.text = product.name
            }

            it.userInfo?.let { user ->
                // Load the user gravatar image if available
                user.iconUrl?.let { icon ->
                    GlideApp.with(review_gravatar.context)
                            .load(icon)
                            .placeholder(R.drawable.ic_user_circle_grey_24dp)
                            .circleCrop()
                            .into(review_gravatar)
                }
                review_user_name.text = user.name
            }

            review_time.text = DateTimeUtils.timeSpanFromTimestamp(it.timestamp, activity as Context)

            it.rating?.let { rating ->
                review_rating_bar.rating = rating
                review_rating_bar.visibility = View.VISIBLE
            }

            review_description.text = it.msg
        }

        // TODO parse actions and set button status
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

    /**
     * Open the product detail page in an external browser
     */
    private fun openProduct() {
        // TODO - open product
//        review?.let {
//            it.productInfo?.url?.let { url ->
//                ActivityUtils.openUrlExternal(activity as Context, url)
//            }
//        }
    }
}
