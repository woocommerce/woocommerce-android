package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_add_product_category.*
import org.wordpress.android.util.ActivityUtils

class AddProductCategoryFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_add_product_category, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_add_category)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        activity?.let { ActivityUtils.hideKeyboard(it) }
    }

    private fun getCategoryName() = product_category_name.getText()
}
