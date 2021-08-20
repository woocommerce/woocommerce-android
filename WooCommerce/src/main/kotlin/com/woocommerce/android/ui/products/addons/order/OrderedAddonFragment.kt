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
import androidx.recyclerview.widget.RecyclerView.LayoutManager
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderedAddonBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.FeatureFeedbackSettings
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.DISMISSED
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.GIVEN
import com.woocommerce.android.model.FeatureFeedbackSettings.FeedbackState.UNANSWERED
import com.woocommerce.android.model.ProductAddon
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.products.addons.AddonListAdapter
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OrderedAddonFragment : BaseFragment(R.layout.fragment_ordered_addon) {
    companion object {
        val TAG: String = OrderedAddonFragment::class.java.simpleName
        val CURRENT_WIP_NOTICE_FEATURE = FeatureFeedbackSettings.Feature.PRODUCT_ADDONS
    }

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderedAddonViewModel by viewModels()

    private val navArgs: OrderedAddonFragmentArgs by navArgs()

    private var _binding: FragmentOrderedAddonBinding? = null
    private val binding get() = _binding!!
    private var layoutManager: LayoutManager? = null

    private val supportActionBar
        get() = activity
            ?.let { it as? AppCompatActivity }
            ?.supportActionBar

    private val currentFeedbackSettings
        get() = FeedbackPrefs.getFeatureFeedbackSettings(TAG)
            ?: FeatureFeedbackSettings(CURRENT_WIP_NOTICE_FEATURE.name)
                .apply { registerItselfWith(TAG) }

    private val shouldRequestFeedback
        get() = currentFeedbackSettings
            .takeIf { it.name == CURRENT_WIP_NOTICE_FEATURE.name }
            ?.let { it.state != DISMISSED }
            ?: false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderedAddonBinding.bind(view)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)

        setupObservers()
        viewModel.start(
            navArgs.orderId,
            navArgs.orderItemId,
            navArgs.addonsProductId
        )
    }

    override fun getFragmentTitle() = getString(R.string.product_add_ons_title)

    private fun setupObservers() {
        viewModel.orderedAddonsData
            .observe(viewLifecycleOwner, Observer(::onOrderedAddonsReceived))
    }

    private fun onOrderedAddonsReceived(orderedAddons: List<ProductAddon>) {
        showWIPNoticeCard(true)
        setupRecyclerViewWith(orderedAddons)
    }

    private fun setupRecyclerViewWith(addonList: List<ProductAddon>) {
        layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
        binding.addonsList.layoutManager = layoutManager
        binding.addonsList.adapter = AddonListAdapter(
            addonList,
            currencyFormatter.buildBigDecimalFormatter(viewModel.currencyCode),
            orderMode = true
        )
    }

    private fun showWIPNoticeCard(shouldBeVisible: Boolean) =
        with(binding.addonsWipCard) {
            initView(
                title = "View add-ons from your device!",
                message = "We are working on making it easier for you to see product add-ons from your device! For now, you’ll be able to see the add-ons for your orders. You can create and edit these add-ons in your web dashbaord.",
                onGiveFeedbackClick = ::onGiveFeedbackClicked,
                onDismissClick = ::onDismissWIPCardClicked
            )

            visibility =
                if (shouldBeVisible && shouldRequestFeedback) VISIBLE
                else GONE
        }

    private fun onGiveFeedbackClicked() {
        // should send track event

        FeatureFeedbackSettings(
            CURRENT_WIP_NOTICE_FEATURE.name,
            GIVEN
        ).registerItselfWith(TAG)

        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.PRODUCT)
            .apply { findNavController().navigateSafely(this) }
    }

    private fun onDismissWIPCardClicked() {
        // should send track event

        FeatureFeedbackSettings(
            CURRENT_WIP_NOTICE_FEATURE.name,
            UNANSWERED
        ).registerItselfWith(TAG)

        showWIPNoticeCard(false)
    }
}
