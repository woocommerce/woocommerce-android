package com.woocommerce.android.ui.reviews

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentReviewsListBinding
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.ViewBindingHolder
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.reviews.ProductReviewStatus.SPAM
import com.woocommerce.android.ui.reviews.ProductReviewStatus.TRASH
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.UnreadItemDecoration
import com.woocommerce.android.widgets.UnreadItemDecoration.ItemDecorationListener
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import dagger.android.support.AndroidSupportInjection
import java.util.Locale
import javax.inject.Inject

class ReviewListFragment : TopLevelFragment(R.layout.fragment_reviews_list),
    ItemDecorationListener,
    ReviewListAdapter.OnReviewClickListener,
    ViewBindingHolder<FragmentReviewsListBinding> {
    companion object {
        const val TAG = "ReviewListFragment"
        const val KEY_NEW_DATA_AVAILABLE = "new-data-available"

        fun newInstance() = ReviewListFragment()
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var reviewsAdapter: ReviewListAdapter

    private val viewModel: ReviewListViewModel by viewModels { viewModelFactory }

    private val skeletonView = SkeletonView()
    private var menuMarkAllRead: MenuItem? = null

    private var pendingModerationRequest: ProductReviewModerationRequest? = null
    private var pendingModerationRemoteReviewId: Long? = null
    private var pendingModerationNewStatus: String? = null
    private var changeReviewStatusSnackbar: Snackbar? = null

    override var binding: FragmentReviewsListBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        binding = FragmentReviewsListBinding.bind(view)
        registerBinding(requireBinding(), this.viewLifecycleOwner)

        val activity = requireActivity()

        reviewsAdapter = ReviewListAdapter(activity, this)
        val unreadDecoration = UnreadItemDecoration(activity as Context, this)
        requireBinding().reviewsList.apply {
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

        requireBinding().notifsRefreshLayout.apply {
            // Set the scrolling view in the custom SwipeRefreshLayout
            scrollUpChild = requireBinding().reviewsList
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.REVIEWS_LIST_PULLED_TO_REFRESH)
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
                AnalyticsTracker.track(Stat.REVIEWS_LIST_MENU_MARK_READ_BUTTON_TAPPED)
                viewModel.markAllReviewsAsRead()
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
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
        super.onDestroyView()
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { showMarkAllReadMenuItem(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) {
                requireBinding().notifsRefreshLayout.isRefreshing = it
            }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { requireBinding().notifsLoadMoreProgress.isVisible = it }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MarkAllAsRead -> handleMarkAllAsReadEvent(event.status)
            }
        })

        viewModel.moderateProductReview.observe(viewLifecycleOwner, Observer {
            if (reviewsAdapter.isEmpty()) {
                pendingModerationRequest = it
            } else {
                it?.let { request -> handleReviewModerationRequest(request) }
            }
        })

        viewModel.reviewList.observe(viewLifecycleOwner, Observer {
            showReviewList(it)
            pendingModerationRequest?.let {
                handleReviewModerationRequest(it)
                pendingModerationRequest = null
            }
        })
    }

    private fun handleMarkAllAsReadEvent(status: ActionStatus) {
        when (status) {
            ActionStatus.SUBMITTED -> {
                menuMarkAllRead?.actionView = layoutInflater.inflate(R.layout.action_menu_progress, null)
            }
            ActionStatus.SUCCESS -> {
                menuMarkAllRead?.actionView = null
                showMarkAllReadMenuItem(show = false)

                // Remove all active notifications from the system bar
                context?.let { NotificationHandler.removeAllReviewNotifsFromSystemBar(it) }
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
                skeletonView.show(requireBinding().notifsView, R.layout.skeleton_notif_list, delayed = true)
                showEmptyView(false)
            }
            false -> skeletonView.hide()
        }
    }

    private fun showEmptyView(show: Boolean) {
        if (show) {
            requireBinding().emptyView.show(EmptyViewType.REVIEW_LIST) {
                ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_REVIEWS)
            }
        } else {
            requireBinding().emptyView.hide()
        }
    }

    private fun showMarkAllReadMenuItem(show: Boolean) {
        menuMarkAllRead?.let { if (it.isVisible != show) it.isVisible = show }
    }

    private fun openReviewDetail(review: ProductReview) {
        (activity as? MainNavigationRouter)?.showReviewDetail(
            review.remoteId,
            launchedFromNotification = false,
            enableModeration = true,
            tempStatus = pendingModerationNewStatus
        )
    }

    private fun handleReviewModerationRequest(request: ProductReviewModerationRequest) {
        when (request.actionStatus) {
            ActionStatus.PENDING -> processNewModerationRequest(request)
            ActionStatus.SUCCESS -> {
                reviewsAdapter.removeHiddenReviewFromList()
                resetPendingModerationVariables()
            }
            ActionStatus.ERROR -> revertPendingModerationState()
            else -> { /* do nothing */
            }
        }
    }

    private fun processNewModerationRequest(request: ProductReviewModerationRequest) {
        with(request) {
            pendingModerationRemoteReviewId = productReview.remoteId
            pendingModerationNewStatus = newStatus.toString()

            var changeReviewStatusCanceled = false

            // Listener for the UNDO button in the snackbar
            val actionListener = OnClickListener {
                AnalyticsTracker.track(Stat.SNACK_REVIEW_ACTION_APPLIED_UNDO_BUTTON_TAPPED)

                // User canceled the action to change the status
                changeReviewStatusCanceled = true

                // Add the notification back to the list
                revertPendingModerationState()
            }

            val callback = object : Snackbar.Callback() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    super.onDismissed(transientBottomBar, event)
                    if (!changeReviewStatusCanceled) {
                        viewModel.submitReviewStatusChange(productReview, newStatus)
                    }
                }
            }

            changeReviewStatusSnackbar = uiMessageResolver
                .getUndoSnack(
                    R.string.review_moderation_undo,
                    ProductReviewStatus.getLocalizedLabel(context, newStatus)
                        .toLowerCase(Locale.getDefault()),
                    actionListener = actionListener
                ).also {
                    it.addCallback(callback)
                    it.show()
                }

            // Manually remove the product review from the list if it's new
            // status will be spam or trash
            if (newStatus == SPAM || newStatus == TRASH) {
                removeProductReviewFromList(productReview.remoteId)
            }

            AppRatingDialog.incrementInteractions()
        }
    }

    private fun removeProductReviewFromList(remoteReviewId: Long) {
        reviewsAdapter.hideReviewWithId(remoteReviewId)
    }

    private fun resetPendingModerationVariables() {
        pendingModerationNewStatus = null
        pendingModerationRemoteReviewId = null
        reviewsAdapter.resetPendingModerationState()
    }

    private fun revertPendingModerationState() {
        AnalyticsTracker.track(Stat.REVIEW_ACTION_UNDO)

        pendingModerationNewStatus?.let {
            val status = ProductReviewStatus.fromString(it)
            if (status == SPAM || status == TRASH) {
                val itemPos = reviewsAdapter.revertHiddenReviewAndReturnPos()
                if (itemPos != SectionedRecyclerViewAdapter.INVALID_POSITION && !reviewsAdapter.isEmpty()) {
                    requireBinding().reviewsList.smoothScrollToPosition(itemPos)
                }
            }
        }

        resetPendingModerationVariables()
    }

    override fun getFragmentTitle() = getString(R.string.review_notifications)

    override fun scrollToTop() {
        requireBinding().reviewsList.smoothScrollToPosition(0)
    }

    override fun getItemTypeAtPosition(position: Int) = reviewsAdapter.getItemTypeAtRecyclerPosition(position)

    override fun onReviewClick(review: ProductReview) {
        openReviewDetail(review)
    }

    override fun shouldExpandToolbar() = requireBinding().reviewsList.computeVerticalScrollOffset() == 0
}
