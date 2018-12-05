package com.woocommerce.android.ui.notifications

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.NotificationReviewDetail
import com.woocommerce.android.extensions.getReviewDetail
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_review_detail.*
import org.wordpress.android.fluxc.model.NotificationModel
import org.wordpress.android.util.DateTimeUtils
import javax.inject.Inject

class ReviewDetailFragment : Fragment(), ReviewDetailContract.View {
    companion object {
        const val TAG = "ReviewDetailFragment"
        const val FIELD_REVIEW_DETAIL = "notif-review-detail"
        const val FIELD_REMOTE_NOTIF_ID = "notif-remote-id"

        fun newInstance(notification: NotificationModel): ReviewDetailFragment {
            val args = Bundle()
            args.putLong(FIELD_REMOTE_NOTIF_ID, notification.remoteNoteId)
            args.putParcelable(FIELD_REVIEW_DETAIL, notification.getReviewDetail())

            val fragment = ReviewDetailFragment()
            fragment.arguments = args
            return fragment
        }
    }

    @Inject lateinit var presenter: ReviewDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val skeletonView = SkeletonView()
    private var review: NotificationReviewDetail? = null
    private var remoteNoteId: Long? = null

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
            review = it.getParcelable(FIELD_REVIEW_DETAIL)
            remoteNoteId = it.getLong(FIELD_REMOTE_NOTIF_ID)
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

        populateView()
    }

    private fun populateView() {
        review?.let {
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
        review?.let {
            it.productInfo?.url?.let { url ->
                ActivityUtils.openUrlExternal(activity as Context, url)
            }
        }
    }
}
