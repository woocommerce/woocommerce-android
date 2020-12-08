package com.woocommerce.android.ui.orders.tracking

import android.content.Context
import android.view.View
import android.widget.Filter
import android.widget.Filterable
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionParameters
import com.woocommerce.android.widgets.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.woocommerce.android.widgets.sectionedrecyclerview.StatelessSection
import kotlinx.android.synthetic.main.dialog_order_tracking_provider_list_header.view.*
import kotlinx.android.synthetic.main.dialog_order_tracking_provider_list_item.view.*
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel

class AddOrderTrackingProviderListAdapter(
    private val context: Context?,
    private val storeCountry: String?,
    private val listener: OnProviderClickListener
) : SectionedRecyclerViewAdapter(), Filterable {
    private var providerList: ArrayList<WCOrderShipmentProviderModel> = ArrayList()
    private var providerSearchList: ArrayList<WCOrderShipmentProviderModel> = ArrayList()

    var selectedCarrierName: String = ""
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    interface OnProviderClickListener {
        fun onProviderClick(providerName: String)
    }

    fun setProviders(providers: List<WCOrderShipmentProviderModel>) {
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

    private fun updateAdapter(providers: List<WCOrderShipmentProviderModel>) {
        // clear all the current data from the adapter
        removeAllSections()

        /*
         * Build a list of [WCOrderShipmentProviderModel] for each country section.
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

        val finalMap = mutableMapOf<String, List<WCOrderShipmentProviderModel>>()
        countryProvidersMap[storeCountry]?.let { wcOrderShipmentProviderModels ->
            storeCountry?.let { finalMap.put(it, wcOrderShipmentProviderModels) }
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
            val customShipmentProviderModel: WCOrderShipmentProviderModel =
                    WCOrderShipmentProviderModel().apply {
                        carrierName = it.getString(R.string.order_shipment_tracking_custom_provider_section_name)
                    }
            return ProviderListSection(
                    it.getString(R.string.order_shipment_tracking_custom_provider_section_title),
                    listOf(customShipmentProviderModel)
            )
        }
        return null
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val charString = charSequence.toString()
                providerList = if (charString.isEmpty()) {
                    providerSearchList
                } else {
                    val filteredList = ArrayList<WCOrderShipmentProviderModel>()
                    for (row in providerSearchList) {
                        if (row.carrierName.contains(charString) ||
                                row.country.contains(charString) ||
                                row.carrierLink.contains(charString)) {
                            filteredList.add(row)
                        }
                    }
                    filteredList
                }
                val filterResults = FilterResults()
                filterResults.values = providerList
                return filterResults
            }
            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                providerList = filterResults.values as ArrayList<WCOrderShipmentProviderModel>
                updateAdapter(providerList)
            }
        }
    }

    /**
     * Custom class represents a single [WCOrderShipmentProviderModel.country]
     * and it's assigned list of [WCOrderShipmentProviderModel].
     * Responsible for providing and populating the header and item view holders.
     */
    private inner class ProviderListSection(
        val country: String,
        val list: List<WCOrderShipmentProviderModel>
    ) : StatelessSection(
            SectionParameters.Builder(R.layout.dialog_order_tracking_provider_list_item)
                    .headerResourceId(R.layout.dialog_order_tracking_provider_list_header)
                    .build()
    ) {
        override fun getContentItemsTotal() = list.size

        override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
            return ItemViewHolder(
                    view
            )
        }

        override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val provider = list[position]
            val itemHolder = holder as ItemViewHolder

            itemHolder.providerName.text = provider.carrierName
            itemHolder.rootView.tag = provider

            val isChecked = provider.carrierName == selectedCarrierName
            itemHolder.selectedProviderRadioButton.isVisible = isChecked
            itemHolder.selectedProviderRadioButton.isChecked = isChecked

            itemHolder.rootView.setOnClickListener {
                val providerItem = it.tag as WCOrderShipmentProviderModel
                selectedCarrierName = providerItem.carrierName
                listener.onProviderClick(selectedCarrierName)
            }
        }

        override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
            return HeaderViewHolder(
                    view
            )
        }

        override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
            val headerViewHolder = holder as HeaderViewHolder
            headerViewHolder.title.text = country
        }
    }

    private class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var providerName: TextView = view.addShipmentTrackingProviderListItem_name
        var selectedProviderRadioButton: RadioButton = view.addShipmentTrackingProviderListItem_tick
        var rootView = view
    }

    private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.providerListHeader
    }
}
