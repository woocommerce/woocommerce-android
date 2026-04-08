package com.woocommerce.android.ui.reviews

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.ActionStatus
import com.woocommerce.android.model.ProductReview
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.reviews.ReviewListViewModel.ReviewListEvent.MarkAllAsRead
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ReviewListFragment :
    BaseFragment(),
    ReviewModerationUi,
    MenuProvider {

    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: ReviewListViewModel by viewModels()

    private var menuMarkAllRead: MenuItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            ReviewListScreen(
                viewModel = viewModel,
                onReviewClick = { review -> onReviewClick(review) },
                onLearnMoreClick = {
                    ChromeCustomTabUtils.launchUrl(requireActivity(), AppUrls.URL_LEARN_MORE_REVIEWS)
                }
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner)
        setupObservers()
        viewModel.start()
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reviews_list_fragment, menu)
        menuMarkAllRead = menu.findItem(R.id.menu_mark_all_read)
    }

    override fun onPrepareMenu(menu: Menu) {
        viewModel.checkForUnreadReviews()
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_mark_all_read -> {
                AnalyticsTracker.track(AnalyticsEvent.REVIEWS_LIST_MENU_MARK_READ_BUTTON_TAPPED)
                viewModel.markAllReviewsAsRead()
                true
            }

            else -> false
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers() {
        viewModel.viewStateData.observe(viewLifecycleOwner) { old, new ->
            new.hasUnreadReviews?.takeIfNotEqualTo(old?.hasUnreadReviews) { showMarkAllReadMenuItem(it) }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is MarkAllAsRead -> handleMarkAllAsReadEvent(event.status)
            }
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
            else -> {}
        }
    }

    private fun showMarkAllReadMenuItem(show: Boolean) {
        menuMarkAllRead?.let { if (it.isVisible != show) it.isVisible = show }
    }

    override fun getFragmentTitle() = getString(R.string.review_notifications)

    private fun onReviewClick(review: ProductReview) {
        AnalyticsTracker.track(AnalyticsEvent.REVIEW_OPEN)
        (activity as? MainNavigationRouter)?.showReviewDetail(
            review.remoteId,
            launchedFromNotification = false,
            tempStatus = review.status
        )
    }

    companion object {
        const val TAG = "ReviewListFragment"

        fun newInstance() = ReviewListFragment()
    }
}
