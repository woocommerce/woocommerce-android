package com.woocommerce.android.ui.coupons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentCouponListBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.Feature.COUPONS
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.coupons.CouponListViewModel.NavigateToCouponDetailsEvent
import com.woocommerce.android.ui.feedback.SurveyType
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CouponListFragment : BaseFragment(R.layout.fragment_coupon_list) {
    companion object {
        const val TAG: String = "CouponListFragment"
    }

    private var _binding: FragmentCouponListBinding? = null
    private val binding get() = _binding!!
    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(COUPONS)?.feedbackState
            ?: FeatureFeedbackSettings.FeedbackState.UNANSWERED

    private val viewModel: CouponListViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCouponListBinding.inflate(inflater, container, false)

        val view = binding.root
        binding.couponsComposeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CouponListScreen(viewModel)
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToCouponDetailsEvent -> navigateToCouponDetails(event.couponId)
            }
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        displayCouponsWIPCard(true)
    }

    private fun navigateToCouponDetails(couponId: Long) {
        findNavController().navigateSafely(
            CouponListFragmentDirections.actionCouponListFragmentToCouponDetailsFragment(couponId)
        )
    }

    private fun displayCouponsWIPCard(show: Boolean) {
        if (!show ||
            feedbackState == FeatureFeedbackSettings.FeedbackState.DISMISSED
        ) {
            binding.couponsWIPcard.isVisible = false
            return
        }

        binding.couponsWIPcard.isVisible = true
        binding.couponsWIPcard.initView(
            getString(R.string.coupon_list_wip_title),
            getString(R.string.coupon_list_wip_message_enabled),
            onGiveFeedbackClick = { onGiveFeedbackClicked() },
            onDismissClick = { onDismissWIPCardClicked() },
            showFeedbackButton = true
        )
    }

    private fun onGiveFeedbackClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_COUPONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_GIVEN
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.GIVEN)

        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.COUPONS)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissWIPCardClicked() {
        AnalyticsTracker.track(
            AnalyticsEvent.FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to AnalyticsTracker.VALUE_COUPONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to AnalyticsTracker.VALUE_FEEDBACK_DISMISSED
            )
        )
        registerFeedbackSetting(FeatureFeedbackSettings.FeedbackState.DISMISSED)
        displayCouponsWIPCard(false)
    }

    private fun registerFeedbackSetting(state: FeatureFeedbackSettings.FeedbackState) {
        FeatureFeedbackSettings(
            COUPONS,
            state
        ).registerItself()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
