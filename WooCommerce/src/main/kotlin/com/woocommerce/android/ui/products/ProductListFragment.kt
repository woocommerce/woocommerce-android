package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.ui.base.TopLevelFragment
import kotlinx.android.synthetic.main.fragment_product_list.*
import kotlinx.android.synthetic.main.wc_empty_view.*

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
        return inflater.inflate(R.layout.fragment_product_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // TODO this is temporary until we have a real product list
        empty_view.visibility = View.VISIBLE
        empty_view_text.text = "Some day this will be a beautiful new products list..."
    }

    override fun getFragmentTitle() = getString(R.string.products)

    override fun refreshFragmentState() {
        // TODO
    }

    override fun onReturnedFromChildFragment() {
        // TODO
    }

    override fun scrollToTop() {
        productsList.smoothScrollToPosition(0)
    }
}
