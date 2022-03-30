package com.woocommerce.android.ui.reviews

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.ViewGroupCompat
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentReviewsListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.UnreadItemDecoration
import com.woocommerce.android.widgets.UnreadItemDecoration.ItemDecorationListener
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewListFragment :
    BaseFragment(R.layout.fragment_reviews_list),
    ItemDecorationListener,
    ReviewListAdapter.OnReviewClickListener,
    ReviewModerationUi {
    companion object {
        const val TAG = "ReviewListFragment"
        const val KEY_NEW_DATA_AVAILABLE = "new-data-available"

        fun newInstance() = ReviewListFragment()
    }

    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var selectedSite: SelectedSite

    private var _reviewsAdapter: ReviewListAdapter? = null
    private val reviewsAdapter: ReviewListAdapter
        get() = _reviewsAdapter!!

    private val viewModel: ReviewListViewModel by viewModels()

    private val skeletonView = SkeletonView()
    private var menuMarkAllRead: MenuItem? = null

    private var changeReviewStatusSnackbar: Snackbar? = null

    private var _binding: FragmentReviewsListBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val transitionDuration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        val fadeThroughTransition = MaterialFadeThrough().apply { duration = transitionDuration }
        enterTransition = fadeThroughTransition
        exitTransition = fadeThroughTransition
        reenterTransition = fadeThroughTransition
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        setHasOptionsMenu(true)

        _binding = FragmentReviewsListBinding.bind(view)
        view.doOnPreDraw { startPostponedEnterTransition() }

        val activity = requireActivity()
        ViewGroupCompat.setTransitionGroup(binding.notifsRefreshLayout, true)
        _reviewsAdapter = ReviewListAdapter(this)
        val unreadDecoration = UnreadItemDecoration(activity as Context, this)
        binding.reviewsList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(false)

            // unread item decoration
            addItemDecoration(unreadDecoration)

            adapter = reviewsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    // noop
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1)) {
                        viewModel.loadMoreReviews()
                    }
                }
            })

            // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
            // and only processes the first click event. More details on this issue can be found here:
            // https://github.com/woocommerce/woocommerce-android/issues/2074
            isMotionEventSplittingEnabled = false
        }

        binding.notifsRefreshLayout.apply {
            // Set the scrolling view in the custom SwipeRefreshLayout
            scrollUpChild = binding.reviewsList
            setOnRefreshListener {
                AnalyticsTracker.track(AnalyticsEvent.REVIEWS_LIST_PULLED_TO_REFRESH)
                viewModel.forceRefreshReviews()
            }
        }

        setupObservers()
        viewModel.start()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reviews_list_fragment, menu)
        menuMarkAllRead = menu.findItem(R.id.menu_mark_all_read)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        viewModel.checkForUnreadReviews()
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_mark_all_read -> {
                AnalyticsTracker.track(AnalyticsEvent.REVIEWS_LIST_MENU_MARK_READ_BUTTON_TAPPED)
                viewModel.markAllReviewsAsRead()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()

        changeReviewStatusSnackbar?.dismiss()
    }

    override fun onDestroyView() {
        skeletonView.hide()
        _reviewsAdapter = null
        super.onDestroyView()
        _binding = null
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { showMarkAllReadMenuItem(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                binding.notifsRefreshLayout.isRefreshing = it
            }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { binding.notifsLoadMoreProgress.isVisible = it }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MarkAllAsRead -> handleMarkAllAsReadEvent(event.status)
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

    private fun handleMarkAllAsReadEvent(status: ActionStatus) {
        when (status) {
            ActionStatus.SUBMITTED -> {
                menuMarkAllRead?.actionView = layoutInflater.inflate(R.layout.action_menu_progress, null)
            }
            ActionStatus.SUCCESS -> {
                menuMarkAllRead?.actionView = null
                showMarkAllReadMenuItem(show = false)
            }
            ActionStatus.ERROR -> menuMarkAllRead?.actionView = null
            else -> {
            }
        }
    }

    private fun showReviewList(reviews: List<ProductReview>) {
        reviewsAdapter.setReviews(reviews)
        showEmptyView(reviews.isEmpty())
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

    private fun showMarkAllReadMenuItem(show: Boolean) {
        menuMarkAllRead?.let { if (it.isVisible != show) it.isVisible = show }
    }

    override fun getFragmentTitle() = getString(R.string.review_notifications)

    override fun getItemTypeAtPosition(position: Int) = reviewsAdapter.getItemTypeAtRecyclerPosition(position)

    override fun onReviewClick(review: ProductReview, sharedView: View?) {
        (activity as? MainNavigationRouter)?.let { router ->
            if (sharedView == null) {
                router.showReviewDetail(
                    review.remoteId,
                    launchedFromNotification = false,
                    tempStatus = review.status
                )
            } else {
                router.showReviewDetailWithSharedTransition(
                    review.remoteId,
                    launchedFromNotification = false,
                    tempStatus = review.status,
                    sharedView = sharedView
                )
            }
        }
    }
}
