package com.woocommerce.android.ui.products

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.base.BaseFragment
import kotlinx.android.synthetic.main.fragment_product_shipping_class_list.*
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

/**
 * Dialog which displays a list of product shipping classes
 */
class ProductShippingClassFragment : BaseFragment() {
    companion object {
        const val TAG: String = "ProductShippingClassDialog"

        fun newInstance(targetFragment: Fragment): ProductShippingClassFragment {
            return ProductShippingClassFragment().also { fragment ->
                fragment.setTargetFragment(targetFragment, RequestCodes.PRODUCT_SHIPPING_CLASS)
            }
        }
    }

    interface ShippingClassFragmentListener {
        fun onRequestShippingClasses(loadMore: Boolean = false)
        fun onShippingClassClicked(shippingClass: WCProductShippingClassModel?)
    }

    private var adapter: ProductShippingClassAdapter? = null
    private lateinit var listener: ShippingClassFragmentListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as ShippingClassFragmentListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_product_shipping_class_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ProductShippingClassAdapter(requireActivity(), listener)
        recycler.adapter = adapter
        listener.onRequestShippingClasses()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun getFragmentTitle() = getString(R.string.product_shipping_class)

    fun setShippingClasses(shippingClasses: List<WCProductShippingClassModel>) {
        adapter?.shippingClassList = shippingClasses
    }
}
