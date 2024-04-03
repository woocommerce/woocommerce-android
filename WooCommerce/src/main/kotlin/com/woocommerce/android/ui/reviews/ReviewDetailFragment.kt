package com.woocommerce.android.ui.reviews

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.transition.MaterialContainerTransform
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentReviewDetailBinding
import com.woocommerce.android.extensions.fastStripHtml
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.ProductImageMap
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorFragment
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorStrategy.SEND_RESULT_ON_CONFIRMATION
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.reviews.ProductReviewStatus.APPROVED
import com.woocommerce.android.ui.reviews.ProductReviewStatus.HOLD
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ProductReviewStatus.TRASH
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.NavigateBackFromNotification
import com.woocommerce.android.ui.reviews.ReviewDetailViewModel.ReviewDetailEvent.Reply
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.REVIEWS
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DateTimeUtils
import org.wordpress.android.util.DisplayUtils
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

@AndroidEntryPoint
class ReviewDetailFragment :
    BaseFragment(R.layout.fragment_review_detail),
    BackPressListener {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var productImageMap: ProductImageMap

    private val viewModel: ReviewDetailViewModel by viewModels()

    private var runOnStartFunc: (() -> Unit)? = null
    private var productIconSize: Int = 0
    private val skeletonView = SkeletonView()

    private var _binding: FragmentReviewDetailBinding? = null
    private val binding get() = _binding!!

    private val navArgs: ReviewDetailFragmentArgs by navArgs()

    private val moderateListener = OnCheckedChangeListener { _, isChecked ->
        AnalyticsTracker.track(AnalyticsEvent.REVIEW_DETAIL_APPROVE_BUTTON_TAPPED)
        when (isChecked) {
            true -> processReviewModeration(APPROVED)
            false -> processReviewModeration(HOLD)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_review_detail, container, false)
    }

    @Suppress("DEPRECATION")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val dimen = requireActivity().resources.getDimensionPixelSize(R.dimen.image_minor_50)
        productIconSize = DisplayUtils.dpToPx(activity, dimen)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentReviewDetailBinding.bind(view)

        ViewCompat.setTransitionName(
            binding.scrollView,
            getString(R.string.review_card_detail_transition_name)
        )

        initializeViewModel()

        handleResult<String>(SimpleTextEditorFragment.SIMPLE_TEXT_EDITOR_RESULT) {
            viewModel.onReviewReplied(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val backgroundColor = ContextCompat.getColor(requireContext(), R.color.default_window_background)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.snack_root
            duration = transitionDuration
            scrimColor = Color.TRANSPARENT
            startContainerColor = backgroundColor
            endContainerColor = backgroundColor
        }
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
        _binding = null
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

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is Exit -> findNavController().popBackStack()
                is NavigateBackFromNotification -> exitReviewDetailOpenedFromNotification()
                is Reply -> navigateToReply()
            }
        }
    }

    private fun navigateToReply() {
        NavGraphMainDirections.actionGlobalSimpleTextEditorFragment(
            currentText = "",
            screenTitle = "Answer review",
            hint = "Answer review",
            strategy = SEND_RESULT_ON_CONFIRMATION
        ).let {
            findNavController().navigateSafely(it)
        }
    }

    private fun exitReviewDetailOpenedFromNotification() {
        findNavController().navigateSafely(
            ReviewDetailFragmentDirections.actionReviewDetailFromNotificationToReviewListFragment()
        )
    }

    private fun setReview(review: ProductReview) {
        // adjust the gravatar url so it's requested at the desired size and a has default image of 404 (this causes the
        // request to return a 404 rather than an actual default image URL, so we can stick with our default avatar)
        val size = activity?.resources?.getDimensionPixelSize(R.dimen.image_major_64) ?: 256
        val avatarUrl = UrlUtils.removeQuery(review.reviewerAvatarUrl) + "?s=" + size + "&d=404"

        // Populate reviewer section
        Glide.with(binding.reviewGravatar.context)
            .load(avatarUrl)
            .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.ic_user_circle_24dp))
            .circleCrop()
            .into(binding.reviewGravatar)

        binding.reviewUserName.text = review.reviewerName
        binding.reviewTime.text = DateTimeUtils.javaDateToTimeSpan(review.dateCreated, requireActivity())

        // Populate reviewed product info
        review.product?.let { product ->
            binding.reviewProductName.text = product.name.fastStripHtml()
            binding.reviewOpenProduct.setOnClickListener {
                AnalyticsTracker.track(AnalyticsEvent.REVIEW_DETAIL_OPEN_EXTERNAL_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(activity as Context, product.externalUrl)
            }
            refreshProductImage(product.remoteProductId)
        }

        if (review.rating > 0) {
            binding.reviewRatingBar.rating = review.rating.toFloat()
            binding.reviewRatingBar.visibility = View.VISIBLE
        } else {
            binding.reviewRatingBar.visibility = View.GONE
        }

        // Set the review text
        binding.reviewDescription.text = HtmlUtils.fromHtml(review.review)

        // Initialize the moderation buttons and set review status
        configureModerationButtons(ProductReviewStatus.fromString(review.status))

        binding.reviewReply.setOnClickListener {
            viewModel.onReplyClicked()
        }
    }

    private fun refreshProductImage(remoteProductId: Long) {
        // Note that if productImageMap doesn't already have the image for this product then it will request
        // it from the backend. When the request completes it will be captured by the presenter, which will
        // call this method to show the image for the just-downloaded product model
        productImageMap.get(remoteProductId)?.let { productImage ->
            val imageUrl = PhotonUtils.getPhotonImageUrl(productImage, productIconSize, productIconSize)
            Glide.with(activity as Context)
                .load(imageUrl)
                .placeholder(ContextCompat.getDrawable(requireContext(), R.drawable.ic_product))
                .into(binding.reviewProductIcon)
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(binding.container, R.layout.skeleton_notif_detail, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun configureModerationButtons(status: ProductReviewStatus) {
        binding.reviewApprove.setOnCheckedChangeListener(null)

        // Use the status override if present,else new status
        when (val newStatus = navArgs.tempStatus?.let { ProductReviewStatus.fromString(it) } ?: status) {
            APPROVED -> binding.reviewApprove.isChecked = true
            HOLD -> binding.reviewApprove.isChecked = false
            else -> WooLog.w(REVIEWS, "Unable to process Review with a status of $newStatus")
        }

        // Configure the moderate button
        binding.reviewApprove.setOnCheckedChangeListener(moderateListener)

        // Configure the spam button
        binding.reviewSpam.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_DETAIL_SPAM_BUTTON_TAPPED)

            processReviewModeration(SPAM)
        }

        // Configure the trash button
        binding.reviewTrash.setOnClickListener {
            AnalyticsTracker.track(AnalyticsEvent.REVIEW_DETAIL_TRASH_BUTTON_TAPPED)

            processReviewModeration(TRASH)
        }
    }

    private fun processReviewModeration(newStatus: ProductReviewStatus) {
        viewModel.moderateReview(newStatus)
    }

    override fun onRequestAllowBackPress(): Boolean = viewModel.onBackPressed()
}
