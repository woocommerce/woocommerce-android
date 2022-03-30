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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentOrderedAddonBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.products.addons.AddonListAdapter
import com.woocommerce.android.ui.products.addons.order.OrderedAddonViewModel.ShowSurveyView
import com.woocommerce.android.ui.products.addons.order.OrderedAddonViewModel.ViewState
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.SkeletonView
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.domain.Addon
import javax.inject.Inject

@AndroidEntryPoint
class OrderedAddonFragment : BaseFragment(R.layout.fragment_ordered_addon) {
    @Inject lateinit var currencyFormatter: CurrencyFormatter

    private val viewModel: OrderedAddonViewModel by viewModels()

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

    private val supportActionBar
        get() = activity
            ?.let { it as? AppCompatActivity }
            ?.supportActionBar

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOrderedAddonBinding.bind(view)

        setupViewModel()
        setupViews()
        loadViewData()
    }

    override fun onResume() {
        super.onResume()
        binding.addonsWipCard.isExpanded = false
    }

    override fun getFragmentTitle() = getString(R.string.product_add_ons_title)

    private fun loadViewData() {
        navArgs<OrderedAddonFragmentArgs>().value.apply {
            viewModel.start(orderId, orderItemId, addonsProductId)
        }
    }

    private fun setupViewModel() {
        viewModel.orderedAddonsData.observe(viewLifecycleOwner, Observer(::onOrderedAddonsReceived))
        viewModel.viewStateLiveData.observe(viewLifecycleOwner, ::handleViewStateChanges)
        viewModel.event.observe(viewLifecycleOwner, ::handleViewModelEvents)
    }

    private fun handleViewStateChanges(old: ViewState?, new: ViewState?) {
        new?.isSkeletonShown?.takeIfNotEqualTo(old?.isSkeletonShown) { isLoadingSkeletonVisible = it }
        new?.isLoadingFailure?.takeIfNotEqualTo(old?.isLoadingFailure) { if (it) onOrderedAddonsFailed() }
        new?.shouldDisplayFeedbackCard?.takeIfNotEqualTo(old?.shouldDisplayFeedbackCard, ::showWIPNoticeCard)
    }

    private fun handleViewModelEvents(event: MultiLiveEvent.Event) {
        when (event) {
            is ShowSurveyView ->
                NavGraphMainDirections
                    .actionGlobalFeedbackSurveyFragment(SurveyType.PRODUCT)
                    .apply { findNavController().navigateSafely(this) }
        }
    }

    private fun setupViews() {
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_gridicons_cross_24dp)
        binding.addonsWipCard.initView(
            title = getString(R.string.ordered_add_ons_wip_title),
            message = getString(R.string.ordered_add_ons_wip_message),
            onGiveFeedbackClick = viewModel::onGiveFeedbackClicked,
            onDismissClick = viewModel::onDismissWIPCardClicked
        )

        binding.addonsList.layoutManager = LinearLayoutManager(
            activity,
            RecyclerView.VERTICAL,
            false
        )
    }

    private fun setupRecyclerViewWith(addonList: List<Addon>) {
        binding.addonsList.adapter = AddonListAdapter(
            addonList,
            currencyFormatter.buildBigDecimalFormatter(viewModel.currencyCode),
            orderMode = true
        )
    }

    private fun onOrderedAddonsReceived(orderedAddons: List<Addon>) {
        binding.addonsEditNotice.visibility = VISIBLE
        setupRecyclerViewWith(orderedAddons)
    }

    private fun onOrderedAddonsFailed() {
        binding.addonsEditNotice.visibility = GONE
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.ordered_add_ons_loading_failed_dialog_title)
            .setMessage(R.string.ordered_add_ons_loading_failed_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.ordered_add_ons_loading_failed_dialog_ok_action) { _, _ ->
                findNavController().navigateUp()
            }
            .show()
    }

    private fun showWIPNoticeCard(shouldBeVisible: Boolean) {
        binding.addonsWipCard.visibility = if (shouldBeVisible) VISIBLE else GONE
    }
}
