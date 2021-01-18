package com.woocommerce.android.ui.orders.tracking

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogOrderTrackingProviderListBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListAdapter.OnProviderClickListener
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import javax.inject.Inject

class AddOrderTrackingProviderListFragment : BaseFragment(R.layout.dialog_order_tracking_provider_list),
    OnQueryTextListener,
    OnProviderClickListener {
    companion object {
        const val TAG: String = "AddOrderTrackingProviderListFragment"
        const val SHIPMENT_TRACKING_PROVIDER_RESULT = "tracking-provider-result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: AddOrderTrackingProviderListViewModel by viewModels { viewModelFactory }

    private val providerListAdapter: AddOrderTrackingProviderListAdapter by lazy {
        val countryName = StringUtils.getCountryByCountryCode(requireContext(), viewModel.countryCode)
        AddOrderTrackingProviderListAdapter(
            context,
            countryName,
            this
        )
    }

    private var searchView: SearchView? = null

    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = DialogOrderTrackingProviderListBinding.bind(view)

        initUi(binding)
        setupObservers(binding)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu.findItem(R.id.menu_search)
        searchView = searchMenuItem!!.actionView as SearchView
        searchView?.let {
            val currentQuery = viewModel.trackingProviderListViewStateData.liveData.value?.query ?: ""
            it.setQuery(currentQuery, false)
            if (currentQuery.isNotEmpty()) it.isIconified = false
            it.imeOptions = it.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            it.setOnQueryTextListener(this@AddOrderTrackingProviderListFragment)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        searchView = null
        super.onDestroyView()
    }

    private fun setupObservers(binding: DialogOrderTrackingProviderListBinding) {
        viewModel.trackingProviderListViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.providersList.takeIfNotEqualTo(old?.providersList) {
                providerListAdapter.setProviders(it)
            }

            new.showSkeleton.takeIfNotEqualTo(old?.showSkeleton) { show ->
                if (show) {
                    skeletonView.show(binding.providersView, R.layout.skeleton_tracking_provider_list, delayed = true)
                } else {
                    skeletonView.hide()
                }
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                is ExitWithResult<*> -> navigateBackWithResult(SHIPMENT_TRACKING_PROVIDER_RESULT, event.data)
                else -> event.isHandled = false
            }
        }
    }

    private fun initUi(binding: DialogOrderTrackingProviderListBinding) {
        providerListAdapter.selectedCarrierName = viewModel.currentSelectedProvider

        binding.providerList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = providerListAdapter
        }
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.order_shipment_tracking_provider_toolbar_title)
    }

    override fun onProviderClick(provider: OrderShipmentProvider) {
        viewModel.onProviderSelected(provider)
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        viewModel.onSearchQueryChanged(query)
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        viewModel.onSearchQueryChanged(newText)
        return true
    }
}
