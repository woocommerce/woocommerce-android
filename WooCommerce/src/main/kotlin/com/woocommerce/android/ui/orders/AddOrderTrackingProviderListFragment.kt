package com.woocommerce.android.ui.orders

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.dialog_order_tracking_provider_list.*
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

class AddOrderTrackingProviderListFragment : DialogFragment(), Toolbar.OnMenuItemClickListener,
        AddOrderShipmentTrackingContract.DialogView {
    companion object {
        const val TAG: String = "AddOrderTrackingProviderListFragment"

        /**
         * @param selectedProviderText = to update the currently selected provider item (if already selected)
         * @param uiMessageResolver = to display snackbar error if provider list could not fetched
         * @param presenter = to load the list of providers from api
         * @param listener = to pass the selected provider back to the [AddOrderShipmentTrackingActivity]
         */
        fun newInstance(
            selectedProviderText: String?,
            uiMessageResolver: UIMessageResolver,
            presenter: AddOrderShipmentTrackingContract.Presenter,
            listener: AddOrderTrackingProviderActionListener
        ): AddOrderTrackingProviderListFragment {
            val fragment = AddOrderTrackingProviderListFragment()
            fragment.listener = listener
            fragment.presenter = presenter
            fragment.uiMessageResolver = uiMessageResolver
            fragment.selectedProviderText = selectedProviderText
            return fragment
        }
    }

    private var selectedProviderText: String? = null
    private lateinit var uiMessageResolver: UIMessageResolver
    private lateinit var listener: AddOrderTrackingProviderActionListener
    private lateinit var presenter: AddOrderShipmentTrackingContract.Presenter
    private lateinit var providerListAdapter: AddOrderTrackingProviderListAdapter

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
        toolbar.setNavigationOnClickListener {
            // handles back button navigation
            listener.onTrackingProviderSelected(providerListAdapter.selectedCarrierName)
            dismiss()
        }
        toolbar.inflateMenu(R.menu.menu_search)
        toolbar.setOnMenuItemClickListener(this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        presenter.takeProviderDialogView(this)

        // Initialise the adapter
        providerListAdapter = AddOrderTrackingProviderListAdapter()

        addTrackingProviderList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = providerListAdapter
        }

        /**
         *  Load shipment tracking providers list from api.
         *
         *  This logic would need to be modified to refresh the provider list
         *  only after a certain time frame. Perhaps once they open the app
         **/

        presenter.loadShipmentTrackingProviders()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        // TODO: add logic to search the list of providers here
        return true
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroyView() {
        presenter.dropProviderDialogView()
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
        uiMessageResolver.getSnack(R.string.orders_shipment_tracking_provider_list_error_fetch_generic).show()
    }

    override fun showProviderList(providers: List<WCOrderShipmentProviderModel>) {
        providerListAdapter.setProviders(providers)

        // Update previously selected provider by the user, if available
        selectedProviderText?.let { providerListAdapter.selectedCarrierName = it }
    }
}
