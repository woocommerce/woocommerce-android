package com.woocommerce.android.ui.products.addons

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.ui.products.addons.AddonListAdapter.AddonsViewHolder
import org.wordpress.android.fluxc.domain.Addon
import java.math.BigDecimal

class AddonListAdapter(
    private val addons: List<Addon>,
    private val formatCurrencyForDisplay: (BigDecimal) -> String,
    private val orderMode: Boolean = false
) : RecyclerView.Adapter<AddonsViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        AddonsViewHolder(ProductAddonCard(parent.context))

    override fun onBindViewHolder(holder: AddonsViewHolder, position: Int) {
        holder.addonCard.bind(addons[position], formatCurrencyForDisplay, orderMode)
    }

    override fun getItemCount() = addons.size

    inner class AddonsViewHolder(
        val addonCard: ProductAddonCard
    ) : RecyclerView.ViewHolder(addonCard)
}
