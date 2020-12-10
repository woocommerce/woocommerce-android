package com.woocommerce.android.ui.orders.tracking

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.annotation.StringRes
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.OnQueryTextListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_CARRIER_SELECTED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_CUSTOM_PROVIDER_SELECTED
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListAdapter.OnProviderClickListener
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListContract.Presenter
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ViewModelFactory
import com.woocommerce.android.widgets.SkeletonView
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.dialog_order_tracking_provider_list.*
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import javax.inject.Inject

class AddOrderTrackingProviderListFragment : BaseFragment(), AddOrderTrackingProviderListContract.View,
    OnQueryTextListener, OnProviderClickListener {
    companion object {
        const val TAG: String = "AddOrderTrackingProviderListFragment"
        const val STATE_KEY_SEARCH_QUERY = "search-query"
        const val STATE_KEY_SELECTED_PROVIDER = "selected-provider"
        const val STATE_KEY_ORDER_IDENTIFIER = "order-identifier"
        const val SHIPMENT_TRACKING_PROVIDER_RESULT = "tracking-provider-result"
    }

    @Inject lateinit var presenter: Presenter
    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val navArgs: AddOrderTrackingProviderListFragmentArgs by navArgs()
    private var orderIdentifier: OrderIdentifier? = null
    private var selectedProviderText: String? = null

    private lateinit var providerListAdapter: AddOrderTrackingProviderListAdapter

    private var searchView: SearchView? = null
    private var searchQuery: String = ""

    private val skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        orderIdentifier = navArgs.orderId
        selectedProviderText = navArgs.selectedProvider
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_order_tracking_provider_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let {
            orderIdentifier = it.getString(STATE_KEY_ORDER_IDENTIFIER)
            selectedProviderText = it.getString(STATE_KEY_SELECTED_PROVIDER)
            searchQuery = it.getString(STATE_KEY_SEARCH_QUERY, "")
        }

        presenter.takeView(this)

        // Initialise the adapter
        providerListAdapter = AddOrderTrackingProviderListAdapter(
            context,
            getCountryName(),
            this
        )

        // Update previously selected provider by the user, if available
        selectedProviderText?.let {
            providerListAdapter.selectedCarrierName = it
        }

        addTrackingProviderList.apply {
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = providerListAdapter
        }

        // Load shipment tracking providers
        presenter.loadShipmentTrackingProviders(orderIdentifier)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val searchMenuItem = menu?.findItem(R.id.menu_search)
        searchView = searchMenuItem?.actionView as? SearchView?
        searchView?.let {
            it.imeOptions = it.imeOptions or EditorInfo.IME_FLAG_NO_EXTRACT_UI
            it.setOnQueryTextListener(this@AddOrderTrackingProviderListFragment)
        }
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_KEY_SEARCH_QUERY, searchQuery)
        outState.putString(STATE_KEY_ORDER_IDENTIFIER, orderIdentifier)
        outState.putString(STATE_KEY_SELECTED_PROVIDER, providerListAdapter.selectedCarrierName)

        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropView()
        searchView = null
        super.onDestroyView()
    }

    override fun getCountryName(): String? {
        context?.let {
            return StringUtils.getCountryByCountryCode(it, presenter.loadStoreCountryFromDb())
        }
        return null
    }

    override fun getFragmentTitle(): String {
        return getString(R.string.order_shipment_tracking_provider_toolbar_title)
    }

    override fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(providersView, R.layout.skeleton_tracking_provider_list, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    // BaseTransientBottomBar.LENGTH_LONG is pointing to Snackabr.LENGTH_LONG which confuses checkstyle
    @Suppress("WrongConstant")
    override fun showProviderListErrorSnack(@StringRes stringResId: Int) {
        uiMessageResolver.showSnack(stringResId)
    }

    override fun onProviderClick(providerName: String) {
        if (providerName == getString(R.string.order_shipment_tracking_custom_provider_section_name)) {
            AnalyticsTracker.track(ORDER_SHIPMENT_TRACKING_CUSTOM_PROVIDER_SELECTED)
        } else {
            AnalyticsTracker.track(
                ORDER_SHIPMENT_TRACKING_CARRIER_SELECTED,
                mapOf(AnalyticsTracker.KEY_OPTION to providerName)
            )
        }
        val carrier = Carrier(
            name = providerName,
            isCustom = providerName == getString(R.string.order_shipment_tracking_custom_provider_section_name)
        )
        navigateBackWithResult(SHIPMENT_TRACKING_PROVIDER_RESULT, carrier)
    }

    override fun showProviderList(providers: List<WCOrderShipmentProviderModel>) {
        providerListAdapter.setProviders(providers)
    }

    // region search
    override fun onQueryTextSubmit(query: String): Boolean {
        providerListAdapter.filter.filter(query)
        org.wordpress.android.util.ActivityUtils.hideKeyboard(activity)
        return true
    }

    override fun onQueryTextChange(newText: String): Boolean {
        if (newText.isNotEmpty()) {
            providerListAdapter.clearAdapterData()
            providerListAdapter.filter.filter(newText)
        } else {
            presenter.loadShipmentTrackingProvidersFromDb()
        }
        return true
    }
}
