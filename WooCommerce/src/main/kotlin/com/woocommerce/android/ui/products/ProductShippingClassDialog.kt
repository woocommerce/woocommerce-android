package com.woocommerce.android.ui.products

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

/**
 * Dialog displays a list of product shipping classes
 *
 * This fragment should be instantiated using the [ProductShippingClassDialog.newInstance] method.
 * Calling classes can obtain the results of selection through the [onActivityResult]
 * via [ProductShippingClassDialog.getTargetFragment].
 */
class ProductShippingClassDialog : DialogFragment() {
    companion object {
        const val TAG: String = "ProductShippingClassDialog"

        fun newInstance(listener: Fragment): ProductShippingClassDialog {
            return ProductShippingClassDialog().also { fragment ->
                fragment.setTargetFragment(listener, RequestCodes.PRODUCT_SHIPPING_CLASS)
                fragment.retainInstance = true
            }
        }
    }

    interface ShippingClassDialogListener {
        fun onShippingClassClicked(shippingClass: WCProductShippingClassModel)
        fun onRequestShippingClasses(loadMore: Boolean = false)
    }

    private var recycler: RecyclerView? = null
    private var adapter: ProductShippingClassAdapter? = null
    private lateinit var listener: ShippingClassDialogListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listener = targetFragment as ShippingClassDialogListener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext()).also { builder ->
            builder.setTitle(R.string.product_shipping_class)
            builder.setView(R.layout.dialog_product_shipping_class_list)
        }.create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        adapter = ProductShippingClassAdapter(requireActivity(), listener)
        recycler = dialog?.findViewById(R.id.recycler)
        recycler?.adapter = adapter
        listener.onRequestShippingClasses()
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    fun setShippingClasses(shippingClasses: List<WCProductShippingClassModel>) {
        adapter?.shippingClassList = shippingClasses
    }
}
