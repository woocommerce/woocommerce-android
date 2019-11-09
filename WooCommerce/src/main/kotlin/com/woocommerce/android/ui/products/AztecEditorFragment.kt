package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.BaseFragment

class AztecEditorFragment : BaseFragment() {
    companion object {
        const val TAG: String = "AztecEditorFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_aztec_editor, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_description)
}
