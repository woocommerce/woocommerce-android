package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.SearchView
import android.support.v7.widget.SearchView.OnQueryTextListener
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_PROVIDERS_LIST_SEARCH
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.dialog_order_tracking_provider_list.*
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

class AddOrderTrackingProviderListFragment : DialogFragment(), AddOrderShipmentTrackingContract.DialogView,
        OnQueryTextListener, View.OnClickListener {
    companion object {
        const val TAG: String = "AddOrderTrackingProviderListFragment"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_SELECTED_PROVIDER = "selected-provider"

        /**
         * @param selectedProviderText = to update the currently selected provider item (if already selected)
         * @param presenter = to load the list of providers from api
         * @param listener = to pass the selected provider back to the [AddOrderShipmentTrackingActivity]
         */
        fun newInstance(
            selectedProviderText: String?,
            presenter: AddOrderShipmentTrackingContract.Presenter,
            listener: AddOrderTrackingProviderActionListener
        ): AddOrderTrackingProviderListFragment {
            val fragment = AddOrderTrackingProviderListFragment()
            fragment.retainInstance = true
            fragment.listener = listener
            fragment.presenter = presenter
            fragment.selectedProviderText = selectedProviderText
            return fragment
        }
    }

    private var selectedProviderText: String? = null
    private var listener: AddOrderTrackingProviderActionListener? = null
    private var presenter: AddOrderShipmentTrackingContract.Presenter? = null
    private lateinit var providerListAdapter: AddOrderTrackingProviderListAdapter

    private var searchView: SearchView? = null
    private var searchQuery: String = ""

    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_order_tracking_provider_list, container)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Dialog_ProviderList

        val toolbar = toolbar as Toolbar
        toolbar.title = getString(R.string.order_shipment_tracking_provider_toolbar_title)
        toolbar.navigationIcon = ContextCompat.getDrawable(requireContext(), R.drawable.zui_ic_back)
        toolbar.setNavigationOnClickListener(this)
        toolbar.inflateMenu(R.menu.menu_search)
        val searchMenuItem = toolbar.menu?.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as SearchView?
        searchView?.setOnQueryTextListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            selectedProviderText = it.getString(STATE_KEY_SELECTED_PROVIDER)
            searchQuery = it.getString(STATE_KEY_SEARCH_QUERY, "")
        }

        presenter?.takeProviderDialogView(this)

        // Initialise the adapter
        providerListAdapter = AddOrderTrackingProviderListAdapter()

        // Update previously selected provider by the user, if available
        selectedProviderText?.let {
            providerListAdapter.selectedCarrierName = it
        }

        addTrackingProviderList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = providerListAdapter
        }

        // Load shipment tracking providers
        presenter?.loadShipmentTrackingProviders()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)
        outState.putString(STATE_KEY_SELECTED_PROVIDER, providerListAdapter.selectedCarrierName)

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter?.dropProviderDialogView()
        searchView = null

        if (retainInstance) {
            dialog?.setDismissMessage(null)
        }

        super.onDestroyView()
    }

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(providersView, R.layout.skeleton_order_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    override fun showProviderListErrorSnack() {
        dialog.window?.let {
            Snackbar.make(
                    it.findViewById(android.R.id.content),
                    R.string.orders_shipment_tracking_provider_list_error_fetch_generic,
                    Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun showProviderList(providers: List<WCOrderShipmentProviderModel>) {
        providerListAdapter.setProviders(providers)
    }

    override fun onClick(v: View?) {
        listener?.onTrackingProviderSelected(providerListAdapter.selectedCarrierName)
        dismiss()
    }

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        AnalyticsTracker.track(
                ORDER_SHIPMENT_TRACKING_PROVIDERS_LIST_SEARCH,
                mapOf(AnalyticsTracker.KEY_SEARCH to query))

        providerListAdapter.filter.filter(query)
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.isNotEmpty()) {
            providerListAdapter.clearAdapterData()
            providerListAdapter.filter.filter(newText)
        } else {
            presenter?.loadShipmentTrackingProvidersFromDb()
        }
        return true
    }
}
