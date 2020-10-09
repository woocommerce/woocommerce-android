package com.woocommerce.android.ui.products.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.reviews.ReviewListAdapter
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import kotlinx.android.synthetic.main.fragment_reviews_list.*
import kotlinx.android.synthetic.main.fragment_reviews_list.view.*
import javax.inject.Inject

class ProductReviewsFragment : BaseFragment() {
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    @Inject lateinit var viewModelFactory: ViewModelFactory
    val viewModel: ProductReviewsViewModel by viewModels { viewModelFactory }

    private lateinit var reviewsAdapter: ReviewListAdapter

    private val skeletonView = SkeletonView()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reviews_list, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_reviews)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()
        reviewsAdapter = ReviewListAdapter(activity, null)

        reviewsList.apply {
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

        notifsRefreshLayout?.apply {
            // Set the scrolling view in the custom SwipeRefreshLayout
            scrollUpChild = reviewsList
            setOnRefreshListener {
                AnalyticsTracker.track(Stat.PRODUCT_REVIEWS_PULLED_TO_REFRESH)
                viewModel.refreshProductReviews()
            }
        }
    }

    private fun setupObservers() {
        viewModel.productReviewsViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { showSkeleton(it) }
            new.isRefreshing?.takeIfNotEqualTo(old?.isRefreshing) { notifsRefreshLayout.isRefreshing = it }
            new.isLoadingMore?.takeIfNotEqualTo(old?.isLoadingMore) { showLoadMoreProgress(it) }
            new.isEmptyViewVisible?.takeIfNotEqualTo(old?.isEmptyViewVisible) { showEmptyView(it) }
        }

        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                else -> event.isHandled = false
            }
        })

        viewModel.reviewList.observe(viewLifecycleOwner, Observer {
            showReviewList(it)
        })
    }

    private fun showReviewList(reviews: List<ProductReview>) {
        reviewsAdapter.setReviews(reviews)
    }

    private fun showLoadMoreProgress(show: Boolean) {
        notifsLoadMoreProgress.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showSkeleton(show: Boolean) {
        when (show) {
            true -> {
                skeletonView.show(notifsView, R.layout.skeleton_notif_list, delayed = true)
                showEmptyView(false)
            }
            false -> skeletonView.hide()
        }
    }

    private fun showEmptyView(show: Boolean) {
        if (show) {
            empty_view.show(EmptyViewType.REVIEW_LIST) {
                ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_REVIEWS)
            }
        } else {
            empty_view.hide()
        }
    }

    override fun onDestroyView() {
        skeletonView.hide()
        super.onDestroyView()
    }
}
