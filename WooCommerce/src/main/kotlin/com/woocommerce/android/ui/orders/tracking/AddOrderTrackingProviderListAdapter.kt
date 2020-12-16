package com.woocommerce.android.ui.orders.tracking

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogOrderTrackingProviderListHeaderBinding
import com.woocommerce.android.databinding.DialogOrderTrackingProviderListItemBinding
import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.sectionedrecyclerview.StatelessSection

class AddOrderTrackingProviderListAdapter(
    private val context: Context?,
    private val storeCountry: String?,
    private val listener: OnProviderClickListener
) : SectionedRecyclerViewAdapter() {
    private var providerList: ArrayList<OrderShipmentProvider> = ArrayList()
    private var providerSearchList: ArrayList<OrderShipmentProvider> = ArrayList()

    var selectedCarrierName: String = ""
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    interface OnProviderClickListener {
        fun onProviderClick(provider: OrderShipmentProvider)
    }

    fun setProviders(providers: List<OrderShipmentProvider>) {
        updateAdapter(providers)
        providerList.clear()
        providerList.addAll(providers)

        providerSearchList.clear()
        providerSearchList.addAll(providerList)
    }

    fun clearAdapterData() {
        if (providerList.isNotEmpty()) {
            removeAllSections()
            providerList.clear()
            notifyDataSetChanged()
        }
    }

    /**
     * returns the total item count in a given section
     * @param title = the title of the section
     */
    fun getSectionItemsTotal(title: String): Int {
        for (entry in sectionsMap) {
            (entry.value as? ProviderListSection)?.let {
                if (title == it.country) {
                    return it.list.size
                }
            }
        }
        return 0
    }

    private fun updateAdapter(providers: List<OrderShipmentProvider>) {
        // clear all the current data from the adapter
        removeAllSections()

        /*
         * Build a list of [OrderShipmentProvider] for each country section.
         * Order of provider list should be:
         * 1. Store country
         * 2. Custom
         * 3. Other countries
         * if the country that the store is associated with matches a country on the providers list
         * then that country section should be displayed first
         * */
        val countryProvidersMap = providers
            .groupBy { it.country }
            .mapValues { entry -> entry.value.map { it } }

        val finalMap = mutableMapOf<String, List<OrderShipmentProvider>>()
        countryProvidersMap[storeCountry]?.let { OrderShipmentProviders ->
            storeCountry?.let { finalMap.put(it, OrderShipmentProviders) }
        }

        /*
         * Add a new section below the store country provider list section to display custom provider
         */
        getCustomProviderSection()?.let { finalMap.put(it.country, it.list) }
        finalMap.putAll(countryProvidersMap)

        finalMap.forEach {
            addSection(ProviderListSection(it.key, it.value))
        }
        notifyDataSetChanged()
    }

    /**
     * Create a section to display custom provider at the top of the list
     */
    private fun getCustomProviderSection(): ProviderListSection? {
        context?.let {
            val customShipmentProviderModel =
                OrderShipmentProvider(
                    carrierName = it.getString(R.string.order_shipment_tracking_custom_provider_section_name),
                    carrierLink = "",
                    country = ""
                )
            return ProviderListSection(
                it.getString(R.string.order_shipment_tracking_custom_provider_section_title),
                listOf(customShipmentProviderModel)
            )
        }
        return null
    }

    /**
     * Custom class represents a single [OrderShipmentProvider.country]
     * and it's assigned list of [OrderShipmentProvider].
     * Responsible for providing and populating the header and item view holders.
     */
    private inner class ProviderListSection(
        val country: String,
        val list: List<OrderShipmentProvider>
    ) : StatelessSection(
        SectionParameters.Builder(R.layout.dialog_order_tracking_provider_list_item)
            .headerResourceId(R.layout.dialog_order_tracking_provider_list_header)
            .build()
    ) {
        override fun getContentItemsTotal() = list.size

        override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
            val viewBinding = DialogOrderTrackingProviderListItemBinding.inflate(
                LayoutInflater.from(view.context),
                view as ViewGroup,
                false
            )
            return ItemViewHolder(viewBinding)
        }

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val provider = list[position]
            val itemHolder = holder as ItemViewHolder
            itemHolder.bind(provider)
        }

        override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
            val viewBinding = DialogOrderTrackingProviderListHeaderBinding.inflate(
                LayoutInflater.from(view.context),
                view as ViewGroup,
                false
            )
            return HeaderViewHolder(viewBinding)
        }

        override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
            val headerViewHolder = holder as HeaderViewHolder
            headerViewHolder.bind(country)
        }
    }

    private inner class ItemViewHolder(private var viewBinding: DialogOrderTrackingProviderListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(provider: OrderShipmentProvider) {
            viewBinding.addShipmentTrackingProviderListItemName.text = provider.carrierName

            val isChecked = provider.carrierName == selectedCarrierName
            viewBinding.addShipmentTrackingProviderListItemTick.isVisible = isChecked
            viewBinding.addShipmentTrackingProviderListItemTick.isChecked = isChecked

            viewBinding.root.setOnClickListener {
                listener.onProviderClick(provider)
            }
        }
    }

    private class HeaderViewHolder(private var viewBinding: DialogOrderTrackingProviderListHeaderBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(country: String) {
            viewBinding.providerListHeader.text = country
        }
    }
}
