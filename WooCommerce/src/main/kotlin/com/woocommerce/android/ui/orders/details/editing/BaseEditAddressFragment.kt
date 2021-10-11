package com.woocommerce.android.ui.orders.details.editing

import android.os.Bundle
import android.view.View
import com.woocommerce.android.R
import com.woocommerce.android.databinding.FragmentBaseEditAddressBinding

class BaseEditAddressFragment :
    BaseOrderEditingFragment(R.layout.fragment_base_edit_address) {
    companion object {
        const val TAG = "BaseEditAddressFragment"
    }

    private var _binding: FragmentBaseEditAddressBinding? = null
    private val binding get() = _binding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


    }

    override fun hasChanges(): Boolean {
        TODO("Not yet implemented")
    }

    override fun saveChanges(): Boolean {
        TODO("Not yet implemented")
    }

}
