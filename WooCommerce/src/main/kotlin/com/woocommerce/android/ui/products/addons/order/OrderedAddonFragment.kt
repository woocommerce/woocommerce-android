package com.woocommerce.android.ui.products.addons.order

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_DISMISSED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FEEDBACK_GIVEN
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_PRODUCT_ADDONS_FEEDBACK
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.FEATURE_FEEDBACK_BANNER
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_ADDONS_ORDER_ADDONS_VIEWED
import com.woocommerce.android.databinding.FragmentOrderedAddonBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.products.addons.AddonListAdapter
import com.woocommerce.android.ui.products.addons.order.OrderedAddonViewModel.*
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderedAddonFragment : BaseFragment(R.layout.fragment_ordered_addon) {
    companion object {
        val TAG: String = OrderedAddonFragment::class.java.simpleName
        val CURRENT_WIP_NOTICE_FEATURE = FeatureFeedbackSettings.Feature.PRODUCT_ADDONS
    }

    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderedAddonViewModel by viewModels()

    private val navArgs: OrderedAddonFragmentArgs by navArgs()

    private var _binding: FragmentOrderedAddonBinding? = null
    private val binding get() = _binding!!

    private val skeletonView = SkeletonView()

    private var isLoadingSkeletonVisible: Boolean = false
        set(show) {
            field = show
            if (show) skeletonView.show(
                viewActual = binding.contentContainer,
                layoutId = R.layout.skeleton_ordered_addon_list,
                delayed = true
            )
            else skeletonView.hide()
        }

    private var isLoadingFailure: Boolean = false

    private val supportActionBar
        get() = activity
            ?.let { it as? AppCompatActivity }
            ?.supportActionBar

    private val currentFeedbackSettings
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)
            ?: FeatureFeedbackSettings(CURRENT_WIP_NOTICE_FEATURE.name)
                .apply { registerItselfWith(TAG) }

    private val shouldRequestFeedback
        get() = currentFeedbackSettings.state != DISMISSED

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderedAddonBinding.bind(view)

        setupObservers()
        setupViews()

        viewModel.start(
            navArgs.orderId,
            navArgs.orderItemId,
            navArgs.addonsProductId
        )
    }

    override fun onResume() {
        super.onResume()
        binding.addonsWipCard.isExpanded = false
    }

    override fun getFragmentTitle() = getString(R.string.product_add_ons_title)

    private fun setupObservers() {
        viewModel.orderedAddonsData
            .observe(viewLifecycleOwner, Observer(::onOrderedAddonsReceived))
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, ::handleViewStateChanges)
    }

    private fun handleViewStateChanges(old: ViewState?, new: ViewState?) {
        new?.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isLoadingSkeletonVisible = it }
        new?.isLoadingFailure?.takeIfNotEqualTo(old?.isLoadingFailure) { isLoadingFailure = it }
    }

    private fun setupViews() {
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)
        binding.addonsWipCard.initView(
            title = getString(R.string.ordered_add_ons_wip_title),
            message = getString(R.string.ordered_add_ons_wip_message),
            onGiveFeedbackClick = ::onGiveFeedbackClicked,
            onDismissClick = ::onDismissWIPCardClicked
        )

        binding.addonsList.layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
    }

    private fun onOrderedAddonsReceived(orderedAddons: List<ProductAddon>) {
        showWIPNoticeCard(true)
        setupRecyclerViewWith(orderedAddons)
        track(orderedAddons)
    }

    private fun setupRecyclerViewWith(addonList: List<ProductAddon>) {
        binding.addonsList.adapter = AddonListAdapter(
            addonList,
            currencyFormatter.buildBigDecimalFormatter(viewModel.currencyCode),
            orderMode = true
        )
    }

    private fun showWIPNoticeCard(shouldBeVisible: Boolean) {
        binding.addonsWipCard.visibility =
            if (shouldBeVisible && shouldRequestFeedback) VISIBLE
            else GONE
    }

    private fun onGiveFeedbackClicked() {
        trackFeedback(VALUE_FEEDBACK_GIVEN)

        FeatureFeedbackSettings(
            CURRENT_WIP_NOTICE_FEATURE.name,
            GIVEN
        ).registerItselfWith(TAG)

        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.PRODUCT)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissWIPCardClicked() {
        trackFeedback(VALUE_FEEDBACK_DISMISSED)

        FeatureFeedbackSettings(
            CURRENT_WIP_NOTICE_FEATURE.name,
            DISMISSED
        ).registerItselfWith(TAG)

        showWIPNoticeCard(false)
    }

    private fun trackFeedback(feedbackAction: String) {
        AnalyticsTracker.track(
            FEATURE_FEEDBACK_BANNER,
            mapOf(
                AnalyticsTracker.KEY_FEEDBACK_CONTEXT to VALUE_PRODUCT_ADDONS_FEEDBACK,
                AnalyticsTracker.KEY_FEEDBACK_ACTION to feedbackAction
            )
        )
    }

    private fun track(addons: List<ProductAddon>) =
        addons.distinctBy { it.name }
            .map { it.name }
            .filter { it.isNotEmpty() }
            .joinToString(",")
            .let {
                AnalyticsTracker.track(
                    PRODUCT_ADDONS_ORDER_ADDONS_VIEWED,
                    mapOf(AnalyticsTracker.KEY_ADDONS to it)
                )
            }
}
