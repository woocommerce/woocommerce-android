package com.woocommerce.android.ui.reviews

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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.reviews.ProductReviewStatus.APPROVED
import com.woocommerce.android.ui.reviews.ProductReviewStatus.HOLD
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ProductReviewStatus.TRASH
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.MarkNotificationAsRead
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.fragment_review_detail.*
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class ReviewDetailFragment : BaseFragment() {
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var productImageMap: ProductImageMap

    private val viewModel: ReviewDetailViewModel by viewModels { viewModelFactory }

    private var runOnStartFunc: (() -> Unit)? = null
    private var productIconSize: Int = 0
    private val skeletonView = SkeletonView()

    private val navArgs: ReviewDetailFragmentArgs by navArgs()

    private val moderateListener = OnCheckedChangeListener { _, isChecked ->
        AnalyticsTracker.track(Stat.REVIEW_DETAIL_APPROVE_BUTTON_TAPPED)
        when (isChecked) {
            true -> processReviewModeration(APPROVED)
            false -> processReviewModeration(HOLD)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dimen = requireActivity().resources.getDimensionPixelSize(R.dimen.image_minor_50)
        productIconSize = DisplayUtils.dpToPx(activity, dimen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    override fun onStart() {
        super.onStart()

        runOnStartFunc?.let {
            it.invoke()
            runOnStartFunc = null
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    override fun getFragmentTitle() = getString(R.string.wc_review_title)

    private fun initializeViewModel() {
        setupObservers(viewModel)
        viewModel.start(navArgs.remoteReviewId, navArgs.launchedFromNotification)
    }

    private fun setupObservers(viewModel: ReviewDetailViewModel) {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.productReview?.takeIfNotEqualTo(old?.productReview) { setReview(it) }
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MarkNotificationAsRead -> {
                    NotificationHandler.removeNotificationWithNoteIdFromSystemBar(
                            requireContext(),
                            event.remoteNoteId.toString()
                    )
                }
                is Exit -> exitDetailView()
            }
        })
    }

    private fun setReview(review: ProductReview) {
        // adjust the gravatar url so it's requested at the desired size and a has default image of 404 (this causes the
        // request to return a 404 rather than an actual default image URL, so we can stick with our default avatar)
        val size = activity?.resources?.getDimensionPixelSize(R.dimen.image_major_64) ?: 256
        val avatarUrl = UrlUtils.removeQuery(review.reviewerAvatarUrl) + "?s=" + size + "&d=404"

        // Populate reviewer section
        GlideApp.with(review_gravatar.context)
                .load(avatarUrl)
                .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_circle_24dp))
                .circleCrop()
                .into(review_gravatar)

        review_user_name.text = review.reviewerName
        review_time.text = DateTimeUtils.javaDateToTimeSpan(review.dateCreated, requireActivity())

        // Populate reviewed product info
        review.product?.let { product ->
            review_product_name.text = product.name.fastStripHtml()
            review_open_product.setOnClickListener {
                AnalyticsTracker.track(Stat.REVIEW_DETAIL_OPEN_EXTERNAL_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(activity as Context, product.externalUrl)
            }
            refreshProductImage(product.remoteProductId)
        }

        if (review.rating > 0) {
            review_rating_bar.rating = review.rating.toFloat()
            review_rating_bar.visibility = View.VISIBLE
        } else {
            review_rating_bar.visibility = View.GONE
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val stars = review_rating_bar.progressDrawable as? LayerDrawable
            stars?.getDrawable(2)?.setColorFilter(
                    ContextCompat.getColor(requireContext(), R.color.alert_yellow),
                    PorterDuff.Mode.SRC_ATOP
            )
        }

        // Set the review text
        review_description.text = HtmlUtils.fromHtml(review.review)

        // Initialize the moderation buttons and set review status
        configureModerationButtons(ProductReviewStatus.fromString(review.status))
    }

    private fun refreshProductImage(remoteProductId: Long) {
        // Note that if productImageMap doesn't already have the image for this product then it will request
        // it from the backend. When the request completes it will be captured by the presenter, which will
        // call this method to show the image for the just-downloaded product model
        productImageMap.get(remoteProductId)?.let { productImage ->
            val imageUrl = PhotonUtils.getPhotonImageUrl(productImage, productIconSize, productIconSize)
            GlideApp.with(activity as Context)
                    .load(imageUrl)
                    .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.ic_product))
                    .into(review_product_icon)
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(container, R.layout.skeleton_notif_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun exitDetailView() {
        if (isStateSaved) {
            runOnStartFunc = { findNavController().popBackStack() }
        } else {
            findNavController().popBackStack()
        }
    }

    private fun configureModerationButtons(status: ProductReviewStatus) {
        review_approve.setOnCheckedChangeListener(null)

        // Use the status override if present,else new status
        when (val newStatus = navArgs.tempStatus?.let { ProductReviewStatus.fromString(it) } ?: status) {
            APPROVED -> review_approve.isChecked = true
            HOLD -> review_approve.isChecked = false
            else -> WooLog.w(REVIEWS, "Unable to process Review with a status of $newStatus")
        }

        // Configure the moderate button
        review_approve.setOnCheckedChangeListener(moderateListener)

        // Configure the spam button
        review_spam.setOnClickListener {
            AnalyticsTracker.track(Stat.REVIEW_DETAIL_SPAM_BUTTON_TAPPED)

            processReviewModeration(SPAM)
        }

        // Configure the trash button
        review_trash.setOnClickListener {
            AnalyticsTracker.track(Stat.REVIEW_DETAIL_TRASH_BUTTON_TAPPED)

            processReviewModeration(TRASH)
        }
    }

    private fun processReviewModeration(newStatus: ProductReviewStatus) {
        viewModel.moderateReview(newStatus)
    }
}
