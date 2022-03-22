package com.woocommerce.android.ui.coupons

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentCouponListBinding
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CouponListFragment : BaseFragment(R.layout.fragment_coupon_list) {
    companion object {
        const val TAG: String = "CouponListFragment"
    }

    private var _binding: FragmentCouponListBinding? = null
    private val binding get() = _binding!!
    private val feedbackState
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)?.state ?: FeatureFeedbackSettings.FeedbackState.UNANSWERED

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
                    CouponListContainer(viewModel)
                }
            }
        }
        return view
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        displayCouponsWIPCard(true)
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
            onGiveFeedbackClick = { },
            onDismissClick = { },
            showFeedbackButton = true
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
