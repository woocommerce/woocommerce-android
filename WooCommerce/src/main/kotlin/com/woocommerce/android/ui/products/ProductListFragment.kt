package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.R.layout
import com.woocommerce.android.ui.base.TopLevelFragment

class ProductListFragment : TopLevelFragment() {
    companion object {
        val TAG: String = ProductListFragment::class.java.simpleName
        fun newInstance() = ProductListFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout.fragment_root, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.products)

    override fun refreshFragmentState() {
        // TODO
    }

    override fun scrollToTop() {
        // TODO
    }

    override fun onReturnedFromChildFragment() {
        // TODO
    }
}
