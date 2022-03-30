package com.woocommerce.android.ui.products.reviews

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentReviewsListBinding
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.reviews.ReviewListAdapter
import com.woocommerce.android.ui.reviews.ReviewModerationUi
import com.woocommerce.android.ui.reviews.observeModerationStatus
import com.woocommerce.android.ui.reviews.reviewList
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductReviewsFragment :
    BaseFragment(R.layout.fragment_reviews_list),
    ReviewListAdapter.OnReviewClickListener,
    ReviewModerationUi,
    BackPressListener {
    companion object {
        const val PRODUCT_REVIEWS_MODIFIED = "product-reviews-modified"
    }
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    val viewModel: ProductReviewsViewModel by viewModels()

    private var _reviewsAdapter: ReviewListAdapter? = null
    private val reviewsAdapter: ReviewListAdapter
        get() = _reviewsAdapter!!

    private val skeletonView = SkeletonView()

    private var _binding: FragmentReviewsListBinding? = null
    private val binding get() = _binding!!

    override fun getFragmentTitle() = getString(R.string.product_reviews)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReviewsListBinding.bind(view)
        setupViews()
        setupObservers()
    }

    override fun onDestroyView() {
        skeletonView.hide()
        _reviewsAdapter = null
        super.onDestroyView()
        _binding = null
    }

    private fun setupViews() {
        _reviewsAdapter = ReviewListAdapter(this)

        binding.reviewsList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(false)

            adapter = reviewsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) { }
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1)) {
                        viewModel.loadMoreReviews()
                    }
                }
            })
        }

        binding.notifsRefreshLayout.apply {
            // Set the scrolling view in the custom SwipeRefreshLayout
            scrollUpChild = binding.reviewsList
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_REVIEWS_PULLED_TO_REFRESH)
                viewModel.refreshProductReviews()
            }
        }
    }

    private fun setupObservers() {
        viewModel.productReviewsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { binding.notifsRefreshLayout.isRefreshing = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { showEmptyView(it) }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithNotice(PRODUCT_REVIEWS_MODIFIED)
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }

        viewModel.reviewList.observe(
            viewLifecycleOwner,
            Observer {
                showReviewList(it)
            }
        )

        observeModerationStatus(
            reviewModerationConsumer = viewModel,
            uiMessageResolver = uiMessageResolver
        )
    }

    private fun showReviewList(reviews: List<ProductReview>) {
        reviewsAdapter.setReviews(reviews)
    }

    private fun showLoadMoreProgress(show: Boolean) {
        binding.notifsLoadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> {
                skeletonView.show(binding.notifsView, R.layout.skeleton_notif_list, delayed = true)
                showEmptyView(false)
            }
            false -> skeletonView.hide()
        }
    }

    private fun showEmptyView(show: Boolean) {
        if (show) {
            binding.emptyView.show(EmptyViewType.REVIEW_LIST) {
                ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_REVIEWS)
            }
        } else {
            binding.emptyView.hide()
        }
    }

    override fun onReviewClick(review: ProductReview, sharedView: View?) {
        (activity as? MainNavigationRouter)?.showReviewDetail(
            review.remoteId,
            launchedFromNotification = false
        )
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }
}
