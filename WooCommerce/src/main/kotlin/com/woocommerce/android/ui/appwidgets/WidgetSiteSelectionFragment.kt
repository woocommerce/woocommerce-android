package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentWidgetSiteSelectorBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.appwidgets.WidgetSiteSelectionAdapter.OnWidgetSiteSelectedListener
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel.SiteUiModel
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.android.support.AndroidSupportInjection

class WidgetSiteSelectionFragment : BaseFragment(), OnWidgetSiteSelectedListener {
    private val viewModel: TodayWidgetConfigureViewModel by navGraphViewModels(R.id.nav_graph_today_widget)

    private var _binding: FragmentWidgetSiteSelectorBinding? = null
    private val binding get() = _binding!!

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun getFragmentTitle() = getString(R.string.stats_today_widget_configure_store_hint)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWidgetSiteSelectorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)

        val activity = requireActivity()
        with(binding.sitesRecycler) {
            layoutManager = LinearLayoutManager(activity)
            adapter = WidgetSiteSelectionAdapter(
                requireContext(), GlideApp.with(activity), this@WidgetSiteSelectionFragment
            )
            addItemDecoration(
                AlignedDividerDecoration(
                    activity, DividerItemDecoration.VERTICAL, R.id.widget_site_name, clipToMargin = false
                )
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers(viewModel: TodayWidgetConfigureViewModel) {
        viewModel.sites.observe(viewLifecycleOwner) { event ->
            (binding.sitesRecycler.adapter as? WidgetSiteSelectionAdapter)?.update(event)
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        }
        viewModel.loadSites()
    }

    override fun onSiteSelected(site: SiteUiModel) {
        viewModel.selectSite(site)
    }
}
