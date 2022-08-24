package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.widgets.WidgetSiteSelectionAdapter.OnWidgetSiteSelectedListener
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel.SiteUiModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.AlignedDividerDecoration
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_widget_site_selector.*
import javax.inject.Inject

class WidgetSiteSelectionFragment : BaseFragment(), OnWidgetSiteSelectedListener {
    @Inject lateinit var viewModelFactory: ViewModelFactory

    private val viewModel: TodayWidgetConfigureViewModel
        by navGraphViewModels(R.id.nav_graph_today_widget) { viewModelFactory }

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun getFragmentTitle() = getString(R.string.stats_today_widget_configure_store_hint)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_widget_site_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val activity = requireActivity()
        with(sites_recycler) {
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

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    private fun setupObservers(viewModel: TodayWidgetConfigureViewModel) {
        viewModel.sites.observe(viewLifecycleOwner, Observer {
            (sites_recycler.adapter as? WidgetSiteSelectionAdapter)?.update(it)
        })
        viewModel.event.observe(viewLifecycleOwner, Observer { event ->
            when (event) {
                is Exit -> findNavController().navigateUp()
                else -> event.isHandled = false
            }
        })
        viewModel.loadSites()
    }

    override fun onSiteSelected(site: SiteUiModel) {
        viewModel.selectSite(site)
    }
}
