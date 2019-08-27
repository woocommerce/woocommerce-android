package com.woocommerce.android.ui.reviews

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.onScrollDown
import com.woocommerce.android.extensions.onScrollUp
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.UnreadItemDecoration
import com.woocommerce.android.widgets.UnreadItemDecoration.ItemDecorationListener
import com.woocommerce.android.widgets.UnreadItemDecoration.ItemType
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_notifs_list.*
import kotlinx.android.synthetic.main.fragment_notifs_list.notifsList
import kotlinx.android.synthetic.main.fragment_notifs_list.view.*
import javax.inject.Inject

class ReviewListFragment : TopLevelFragment(), ItemDecorationListener, ReviewListAdapter.OnReviewClickListener {
    companion object {
        const val TAG = "ReviewListFragment"
        const val KEY_LIST_STATE = "list-state"
        const val KEY_IS_REFRESH_PENDING = "is-refresh-pending"

        fun newInstance() = ReviewListFragment()
    }

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private lateinit var viewModel: ReviewListViewModel
    private lateinit var reviewsAdapter: ReviewListAdapter

    private val skeletonView = SkeletonView()

    var isRefreshPending = true
    private var listState: Parcelable? = null // Save the state of the recycler view

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        savedInstanceState?.let { bundle ->
            listState = bundle.getParcelable(KEY_LIST_STATE)
            isRefreshPending = bundle.getBoolean(KEY_IS_REFRESH_PENDING, false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // TODO AMANDA - rename this layout
        return inflater.inflate(R.layout.fragment_notifs_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()

        reviewsAdapter = ReviewListAdapter(activity, this)
        val unreadDecoration = UnreadItemDecoration(activity as Context, this)
        notifsList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(false)
            // divider decoration between items
            addItemDecoration(
                    androidx.recyclerview.widget.DividerItemDecoration(
                            context,
                            androidx.recyclerview.widget.DividerItemDecoration.VERTICAL
                    )
            )
            // unread item decoration
            addItemDecoration(unreadDecoration)
            adapter = reviewsAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy > 0) {
                        onScrollDown()
                    } else if (dy < 0) {
                        onScrollUp()
                    }
                }

                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)

                    if (!recyclerView.canScrollVertically(1)) {
                        viewModel.loadReviews(true)
                    }
                }
            })
        }

        notifsRefreshLayout?.apply {
            activity.let { activity ->
                setColorSchemeColors(
                        ContextCompat.getColor(activity, R.color.colorPrimary),
                        ContextCompat.getColor(activity, R.color.colorAccent),
                        ContextCompat.getColor(activity, R.color.colorPrimaryDark)
                )
            }
            // Set the scrolling view in the custom SwipeRefreshLayout
            scrollUpChild = notifsList
            setOnRefreshListener {
                // TODO AMANDA : new track notification for refreshing all product reviews
                viewModel.refreshReviewList()
            }
        }

        // TODO AMANDA setup refresh layout
        listState?.let {
            notifsList.layoutManager?.onRestoreInstanceState(listState)
            listState = null
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViewModel()
    }

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // TODO AMANDA : save list state

        outState.putBoolean(KEY_IS_REFRESH_PENDING, isRefreshPending)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }

    private fun initializeViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(ReviewListViewModel::class.java)
        setupObservers()
        viewModel.start()
    }

    private fun setupObservers() {
        viewModel.reviewList.observe(this, Observer {
            showReviewList(it)
        })

        viewModel.isSkeletonShown.observe(this, Observer {
            showSkeleton(it)
        })

        viewModel.isRefreshing.observe(this, Observer {
            notifsRefreshLayout.isRefreshing = it
        })

        viewModel.isLoadingMore.observe(this, Observer {
            showLoadMoreProgress(it)
        })

        viewModel.showSnackbarMessage.observe(this, Observer {
            uiMessageResolver.showSnack(it)
        })
    }

    private fun showReviewList(reviews: List<ProductReview>) {
        reviewsAdapter.setReviews(reviews)

        showEmptyView(reviews.isEmpty())
    }

    private fun showLoadMoreProgress(show: Boolean) {
        notifsLoadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun showSkeleton(show: Boolean) {
        when (show) {
            true -> skeletonView.show(notifsView, R.layout.skeleton_notif_list, delayed = true)
            false -> skeletonView.hide()
        }
    }

    private fun showEmptyView(show: Boolean) {
        if (show) empty_view.show(R.string.reviews_empty_message) else empty_view.hide()
    }

    override fun getFragmentTitle() = getString(R.string.review_notifications)

    override fun refreshFragmentState() {
        if (isActive) {
            viewModel.refreshReviewList()
        }
    }

    override fun scrollToTop() {
        notifsList?.smoothScrollToPosition(0)
    }

    override fun onReturnedFromChildFragment() {
        // TODO AMANDA - Refresh list of reviews if needed
    }

    override fun getItemTypeAtPosition(position: Int): ItemType {
        // TODO AMANDA : implement actual
        return ItemType.UNREAD
    }

    override fun onReviewClick(remoteReviewId: Long) {
        // TODO AMANDA : open review detail
    }
}
