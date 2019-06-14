package com.woocommerce.android.ui.notifications

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.canMarkAsSpam
import com.woocommerce.android.extensions.canModerate
import com.woocommerce.android.extensions.canTrash
import com.woocommerce.android.extensions.getConvertedTimestamp
import com.woocommerce.android.extensions.getProductInfo
import com.woocommerce.android.extensions.getRating
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_review_detail.*
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class ReviewDetailFragment : androidx.fragment.app.Fragment(), ReviewDetailContract.View {
    @Inject lateinit var presenter: ReviewDetailContract.Presenter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var networkStatus: NetworkStatus
    @Inject lateinit var productImageMap: ProductImageMap

    private val skeletonView = SkeletonView()
    private var remoteNoteId: Long = 0L
    private var remoteCommentId: Long = 0L
    private var commentStatusOverride: CommentStatus? = null
    private var productUrl: String? = null
    private var remoteProductId: Long = 0L
    private var runOnStartFunc: (() -> Unit)? = null
    private var productIconSize: Int = 0

    private val navArgs: ReviewDetailFragmentArgs by navArgs()

    private val moderateListener = OnCheckedChangeListener { _, isChecked ->
        AnalyticsTracker.track(Stat.REVIEW_DETAIL_APPROVE_BUTTON_TAPPED)
        when (isChecked) {
            true -> processCommentModeration(CommentStatus.APPROVED)
            false -> processCommentModeration(CommentStatus.UNAPPROVED)
        }
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dimen = activity!!.resources.getDimensionPixelSize(R.dimen.product_icon_sz)
        productIconSize = DisplayUtils.dpToPx(activity, dimen)
        presenter.takeView(this)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        remoteNoteId = navArgs.remoteNoteId
        remoteCommentId = navArgs.remoteCommentId
        commentStatusOverride = navArgs.tempStatus?.let {
            CommentStatus.fromString(it)
        }

        presenter.loadNotificationDetail(remoteNoteId, remoteCommentId)
    }

    override fun onStart() {
        super.onStart()

        runOnStartFunc?.let {
            it.invoke()
            runOnStartFunc = null
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

    override fun setNotification(note: NotificationModel, comment: CommentModel) {
        // adjust the gravatar url so it's requested at the desired size and a has default image of 404 (this causes the
        // request to return a 404 rather than an actual default image URL, so we can stick with our default avatar)
        val size = activity?.resources?.getDimensionPixelSize(R.dimen.avatar_sz_large) ?: 256
        val avatarUrl = UrlUtils.removeQuery(comment.authorProfileImageUrl) + "?s=" + size + "&d=404"

        // Populate reviewer section
        GlideApp.with(review_gravatar.context)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_user_circle_grey_24dp)
                .circleCrop()
                .into(review_gravatar)
        review_user_name.text = comment.authorName
        review_time.text = DateTimeUtils.timeSpanFromTimestamp(note.getConvertedTimestamp(), activity as Context)

        // Populate reviewed product info
        review_product_name.text = comment.postTitle
        note.getProductInfo()?.url?.let { url ->
            review_open_product.setOnClickListener {
                AnalyticsTracker.track(Stat.REVIEW_DETAIL_OPEN_EXTERNAL_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(activity as Context, url)
            }
        }
        productUrl = note.getProductInfo()?.url

        // Set the rating if available, or hide
        note.getRating()?.let { rating ->
            review_rating_bar.rating = rating
            review_rating_bar.visibility = View.VISIBLE

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val stars = review_rating_bar.progressDrawable as? LayerDrawable
                stars?.getDrawable(2)?.setColorFilter(
                        ContextCompat.getColor(requireContext(), R.color.alert_yellow),
                        PorterDuff.Mode.SRC_ATOP
                )
            }
        }

        // Set the review text
        review_description.text = HtmlUtils.fromHtml(comment.content)

        // Initialize moderation buttons and set comment status
        configureModerationButtons(note)
        updateStatus(CommentStatus.fromString(comment.status))

        note.getProductInfo()?.let { info ->
            remoteProductId = info.remoteProductId
            refreshProductImage()
        }

        activity?.let { presenter.markNotificationRead(it, note) }
    }

    override fun refreshProductImage() {
        // Note that if productImageMap doesn't already have the image for this product then it will request
        // it from the backend. When the request completes it will be captured by the presenter, which will
        // call this method to show the image for the just-downloaded product model
        productImageMap.get(remoteProductId)?.let { productImage ->
            val imageUrl = PhotonUtils.getPhotonImageUrl(productImage, productIconSize, productIconSize)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(review_product_icon)
        }
    }

    private fun configureModerationButtons(note: NotificationModel) {
        // Configure the moderate button
        with(review_approve) {
            if (note.canModerate()) {
                visibility = View.VISIBLE
                setOnCheckedChangeListener(moderateListener)
            } else {
                visibility = View.GONE
            }
        }

        // Configure the spam button
        with(review_spam) {
            if (note.canMarkAsSpam()) {
                visibility = View.VISIBLE
                setOnClickListener {
                    AnalyticsTracker.track(Stat.REVIEW_DETAIL_SPAM_BUTTON_TAPPED)

                    processCommentModeration(CommentStatus.SPAM)
                }
            } else {
                visibility = View.GONE
            }
        }

        // Configure the trash button
        with(review_trash) {
            if (note.canTrash()) {
                visibility = View.VISIBLE
                setOnClickListener {
                    AnalyticsTracker.track(Stat.REVIEW_DETAIL_TRASH_BUTTON_TAPPED)

                    processCommentModeration(CommentStatus.TRASH)
                }
            } else {
                visibility = View.GONE
            }
        }
    }

    override fun updateStatus(status: CommentStatus) {
        review_approve.setOnCheckedChangeListener(null)

        // Use the status override if present, else new status
        val newStatus = commentStatusOverride ?: status
        when (newStatus) {
            CommentStatus.APPROVED -> review_approve.isChecked = true
            CommentStatus.UNAPPROVED -> review_approve.isChecked = false
            else -> WooLog.w(NOTIFICATIONS, "Unable to process Notification with a status of $status")
        }
        review_approve.setOnCheckedChangeListener(moderateListener)
    }

    private fun processCommentModeration(newStatus: CommentStatus) {
        parentFragment?.let { listener ->
            if (listener is ReviewActionListener) {
                presenter.comment?.let {
                    listener.moderateComment(remoteNoteId, it, newStatus)
                }

                // Close this fragment
                findNavController().popBackStack()
            } else {
                WooLog.e(NOTIFICATIONS, "ParentFragment must implement ReviewActionListener to " +
                        "moderate product review notifications!")

                uiMessageResolver.showSnack(R.string.wc_moderate_review_error)
            }
        }
    }

    override fun showLoadReviewError() {
        uiMessageResolver.showSnack(R.string.wc_load_review_error)
        if (isStateSaved) {
            runOnStartFunc = { activity?.onBackPressed() }
        } else {
            activity?.onBackPressed()
        }
    }
}
