package com.woocommerce.android.ui.products.addons

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AddonItemBinding
import com.woocommerce.android.model.ProductAddon

class AddonListAdapter(
    val addons: List<ProductAddon>
) : RecyclerView.Adapter<AddonListAdapter.AddonsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddonsViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: AddonsViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount() = addons.size

    inner class AddonsViewHolder(
        val viewBinding: AddonItemBinding
    ) : RecyclerView.ViewHolder(viewBinding.root)
}
