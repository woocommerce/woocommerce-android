package com.woocommerce.android.ui.products.reviews

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentProductReviewsListBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.reviews.ReviewListAdapter
import com.woocommerce.android.ui.reviews.ReviewModerationUi
import com.woocommerce.android.ui.reviews.observeModerationStatus
import com.woocommerce.android.ui.reviews.reviewList
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.setupTabletSecondPaneToolbar
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.UnreadItemDecoration
import com.woocommerce.android.widgets.UnreadItemDecoration.ItemDecorationListener
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProductReviewsFragment :
    BaseFragment(R.layout.fragment_product_reviews_list),
    ReviewListAdapter.OnReviewClickListener,
    ReviewModerationUi,
    BackPressListener,
    ItemDecorationListener {
    companion object {
        const val PRODUCT_REVIEWS_MODIFIED = "product-reviews-modified"
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    val viewModel: ProductReviewsViewModel by viewModels()

    private var _reviewsAdapter: ReviewListAdapter? = null
    private val reviewsAdapter: ReviewListAdapter
        get() = _reviewsAdapter!!

    private val skeletonView = SkeletonView()

    private var _binding: FragmentProductReviewsListBinding? = null
    private val binding get() = _binding!!

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentProductReviewsListBinding.bind(view)
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
        val unreadReviewItemDecoration = UnreadItemDecoration(requireContext(), this)

        binding.reviewsList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(false)
            addItemDecoration(unreadReviewItemDecoration)

            adapter = reviewsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1)) {
                        viewModel.loadMoreReviews()
                    }
                }
            })
        }

        binding.apply {
            // Set the scrolling view in the custom SwipeRefreshLayout
            notifsRefreshLayout.scrollUpChild = reviewsList
            notifsRefreshLayout.setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.PRODUCT_REVIEWS_PULLED_TO_REFRESH)
                viewModel.refreshProductReviews()
            }
        }
        setUnreadFilterChangedListener()

        setupTabletSecondPaneToolbar(
            title = getString(R.string.product_reviews),
            onMenuItemSelected = { _ -> false },
            onCreateMenu = { toolbar ->
                toolbar.setNavigationOnClickListener {
                    viewModel.onBackButtonClicked()
                }
            }
        )
    }

    private fun setupObservers() {
        viewModel.productReviewsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.notifsRefreshLayout.isRefreshing = it
            }
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

        viewModel.reviewList.observe(viewLifecycleOwner) {
            showReviewList(it)
        }

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
            if (binding.unreadFilterSwitch.isChecked) {
                binding.unreadReviewsFilterLayout.show()
                binding.emptyView.show(EmptyViewType.UNREAD_FILTERED_REVIEW_LIST)
            } else {
                binding.emptyView.show(EmptyViewType.REVIEW_LIST) {
                    ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_REVIEWS)
                }
                binding.unreadReviewsFilterLayout.hide()
            }
        } else {
            binding.emptyView.hide()
        }
    }

    private fun setUnreadFilterChangedListener() {
        binding.unreadFilterSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onUnreadReviewsFilterChanged(isChecked)
        }
    }

    override fun onReviewClick(review: ProductReview, sharedView: View?) {
        AnalyticsTracker.track(AnalyticsEvent.REVIEW_OPEN)
        (activity as? MainNavigationRouter)?.showReviewDetail(
            review.remoteId,
            launchedFromNotification = false
        )
    }

    override fun onRequestAllowBackPress(): Boolean {
        viewModel.onBackButtonClicked()
        return false
    }

    override fun getItemTypeAtPosition(position: Int) = reviewsAdapter.getItemTypeAtRecyclerPosition(position)
}
